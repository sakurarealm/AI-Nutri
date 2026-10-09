Page({
  data: {
    statusBarHeightPx: 0,
    navHeightPx: 0,
    navContentHeightPx: 0,
    windowHeightPx: 0,
    cameraHeightPx: 420,
    resultScrollHeightPx: 0,

    previewImage: '',
    recognizing: false,
    showResult: false,

    summary: '',
    mealItems: [],
    nutritionText: '',
    notesText: '',
    impactText: '',
    recordFoods: [],

    addPromptVisible: false,
    mealPickerVisible: false,
    addedMealType: '',

    memberId: null,
    memberKey: 'me',
    memberLoading: false,

    nutritionFolded: true,
    notesFolded: true,
    impactFolded: true,

    wheelCarbs: 0,
    wheelProtein: 0,
    wheelFat: 0,
    wheelFiber: 0,
    wheelCalories: 0,
    showWheel: false
  },

  onLoad() {
    this.cameraCtx = wx.createCameraContext();
    this.initNavMetrics();
    const memberKey = (() => {
      try {
        return String(wx.getStorageSync('nutrition_selected_member') || 'me');
      } catch (e) {
        return 'me';
      }
    })();
    this.setData({ memberKey: memberKey || 'me' });
  },

  initNavMetrics() {
    try {
      const sys = wx.getSystemInfoSync ? wx.getSystemInfoSync() : {};
      const statusBarHeight = Number(sys.statusBarHeight) || 0;
      const windowHeight = Number(sys.windowHeight) || 0;
      const menu = wx.getMenuButtonBoundingClientRect ? wx.getMenuButtonBoundingClientRect() : null;
      const navBarHeight = menu && menu.top && menu.height
        ? Math.round((menu.top - statusBarHeight) * 2 + menu.height)
        : 44;
      const navHeightPx = Math.max(0, Math.round(statusBarHeight + navBarHeight));
      const navContentHeightPx = Math.max(0, Math.round(navHeightPx - statusBarHeight));
      const cameraHeightPx = this.calcCameraHeight(windowHeight);
      const resultScrollHeightPx = this.calcResultScrollHeight(windowHeight, navHeightPx, cameraHeightPx);
      this.setData({
        statusBarHeightPx: Math.max(0, Math.round(statusBarHeight)),
        navHeightPx,
        navContentHeightPx,
        windowHeightPx: Math.max(0, Math.round(windowHeight)),
        cameraHeightPx,
        resultScrollHeightPx
      });
    } catch (e) {
      this.setData({ statusBarHeightPx: 0, navHeightPx: 0, navContentHeightPx: 0, windowHeightPx: 0, cameraHeightPx: 420, resultScrollHeightPx: 0 });
    }
  },

  calcCameraHeight(windowHeight) {
    const h = Number(windowHeight) || 0;
    if (h <= 0) return 420;
    const v = Math.round(h * 0.56);
    return Math.max(320, Math.min(v, h - 180));
  },

  calcResultScrollHeight(windowHeight, navHeightPx, cameraHeightPx) {
    const h = Number(windowHeight) || 0;
    if (h <= 0) return 0;
    const nav = Math.max(0, Number(navHeightPx) || 0);
    const cam = Math.max(0, Number(cameraHeightPx) || 0);
    return Math.max(0, Math.round(h - nav - cam));
  },

  takePhoto() {
    if (this.data.recognizing) return;

    const preview = String(this.data.previewImage || '').trim();
    if (preview) {
      this.recognizeImage(preview);
      return;
    }

    if (!this.cameraCtx) this.cameraCtx = wx.createCameraContext();
    this.cameraCtx.takePhoto({
      quality: 'high',
      success: (res) => {
        const path = res && res.tempImagePath ? String(res.tempImagePath) : '';
        if (!path) {
          wx.showToast({ title: '拍照失败', icon: 'none' });
          return;
        }
        this.setData({
          previewImage: path,
          showResult: false,
          addedMealType: '',
          addPromptVisible: false,
          mealPickerVisible: false
        });
        this.recognizeImage(path);
      },
      fail: () => {
        wx.showToast({ title: '拍照失败', icon: 'none' });
      }
    });
  },

  resetPhoto() {
    this.setData({
      previewImage: '',
      recognizing: false,
      showResult: false,
      summary: '',
      mealItems: [],
      nutritionText: '',
      notesText: '',
      impactText: '',
      recordFoods: [],
      addPromptVisible: false,
      mealPickerVisible: false,
      addedMealType: '',
      nutritionFolded: true,
      notesFolded: true,
      impactFolded: true,
      wheelCarbs: 0,
      wheelProtein: 0,
      wheelFat: 0,
      wheelFiber: 0,
      wheelCalories: 0,
      showWheel: false
    });
  },

  async recognizeImage(filePath) {
    const path = String(filePath || '').trim();
    if (!path) return;
    if (this.data.recognizing) return;

    this.setData({
      recognizing: true,
      showResult: false,
      addPromptVisible: false,
      mealPickerVisible: false,
      summary: '',
      mealItems: [],
      nutritionText: '',
      notesText: '',
      impactText: '',
      recordFoods: []
    });

    const { compressOrPassthrough } = require('../../../utils/imageCompress.js')
    const compressed = await compressOrPassthrough(path, { maxWidth: 1280, quality: 0.75 })
    const uploadPath = compressed.tempFilePath || path

    const request = require('../../../utils/request');
    try {
      const body = await request.uploadFile('/recognition/meal/recognize', uploadPath, { loadingText: '识别中...' });
      const data = (body && (body.data || body)) || {};
      const items = Array.isArray(data.items) ? data.items : [];
      const recordFoods = Array.isArray(data.recordFoods) ? data.recordFoods : [];
      const mealItems = items.map((it) => ({
        name: String(it && it.name ? it.name : ''),
        portion: String(it && it.portion ? it.portion : ''),
        calories: Math.max(0, Number(it && it.calories != null ? it.calories : 0) || 0)
      })).filter((it) => !!it.name);

      this.setData({
        recognizing: false,
        showResult: true,
        summary: String(data.summary || ''),
        mealItems,
        nutritionText: String(data.nutritionText || ''),
        notesText: String(data.notesText || ''),
        impactText: String(data.impactText || ''),
        recordFoods,
        addPromptVisible: recordFoods.length > 0
      });
      this.extractNutritionForWheel(data, items);
    } catch (e) {
      this.setData({ recognizing: false });
      wx.showToast({ title: '识别失败', icon: 'none' });
    }
  },

  toggleFold(e) {
    const key = String(e && e.currentTarget && e.currentTarget.dataset ? e.currentTarget.dataset.type : '').trim();
    if (!key) return;
    this.setData({ [key]: !this.data[key] });
  },

  goBack() {
    const pages = typeof getCurrentPages === 'function' ? getCurrentPages() : [];
    if (pages.length > 1) {
      wx.navigateBack({ delta: 1 });
      return;
    }
    wx.switchTab({ url: '/pages/nutritionbrochure/nutritionbrochure' });
  },

  dismissAddPrompt() {
    this.setData({ addPromptVisible: false, mealPickerVisible: false });
  },

  openMealPicker() {
    const foods = Array.isArray(this.data.recordFoods) ? this.data.recordFoods : [];
    if (!foods.length) {
      wx.showToast({ title: '没有可添加的菜品', icon: 'none' });
      return;
    }
    this.setData({ mealPickerVisible: true, addPromptVisible: false });
  },

  cancelMealPicker() {
    this.setData({ mealPickerVisible: false, addPromptVisible: true });
  },

  async ensureMemberId() {
    let memberId = Number(this.data.memberId);
    if (Number.isFinite(memberId) && memberId > 0) return memberId;
    if (this.data.memberLoading) return null;

    this.setData({ memberLoading: true });
    const request = require('../../../utils/request');
    const memberKey = String(this.data.memberKey || 'me').trim() || 'me';
    try {
      const body = await request.get('/nutrition/members', {}, { loading: false, showError: false });
      const list = body && Array.isArray(body.data) ? body.data : [];
      const m = list.find(it => String((it && it.memberKey) || '').trim() === memberKey)
        || list.find(it => String((it && it.memberKey) || '').trim() === 'me')
        || (list.length ? list[0] : null);
      const id = m && m.id ? Number(m.id) : null;
      if (Number.isFinite(id) && id > 0) {
        this.setData({ memberId: id, memberLoading: false });
        return id;
      }
    } catch (e) {}
    this.setData({ memberLoading: false });
    return null;
  },

  getTodayDate() {
    const d = new Date();
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  },

  async pickMealType(e) {
    const type = String(e && e.currentTarget && e.currentTarget.dataset ? e.currentTarget.dataset.type : '').trim();
    if (!type) return;

    const foods = Array.isArray(this.data.recordFoods) ? this.data.recordFoods : [];
    if (!foods.length) {
      wx.showToast({ title: '没有可添加的菜品', icon: 'none' });
      return;
    }

    const memberId = await this.ensureMemberId();
    if (!memberId) {
      wx.showToast({ title: '未获取到成员信息', icon: 'none' });
      return;
    }

    const request = require('../../../utils/request');
    try {
      const body = await request.post('/recognition/meal/add-to-brochure', {
        memberId,
        date: this.getTodayDate(),
        mealType: type,
        foods
      }, { loading: true, showError: true });
      const ok = body && (body.code === 0 || body.success || (body.data && body.data.ok));
      if (ok) {
        this.setData({
          mealPickerVisible: false,
          addPromptVisible: false,
          addedMealType: type
        });
        wx.showToast({ title: '已添加到营养册', icon: 'success' });
      }
    } catch (e) {}
  },

  goToBrochure() {
    wx.switchTab({ url: '/pages/nutritionbrochure/nutritionbrochure' });
  },

  extractNutritionForWheel(data, items) {
    let carbs = 0
    let protein = 0
    let fat = 0
    let fiber = 0
    let calories = 0

    const tot = data && data.totalNutrition ? data.totalNutrition : null
    if (tot) {
      carbs = parseFloat(tot.carbs || tot.carbohydrate || 0) || 0
      protein = parseFloat(tot.protein || 0) || 0
      fat = parseFloat(tot.fat || 0) || 0
      fiber = parseFloat(tot.fiber || 0) || 0
      calories = parseFloat(tot.calories || tot.calorie || 0) || 0
    }

    const arr = Array.isArray(items) ? items : []
    arr.forEach((it) => {
      const cals = parseFloat(it.calories || it.calorie || 0) || 0
      if (cals > 0) {
        calories = calories > 0 ? calories + cals : cals
      }
      const c = parseFloat(it.carbs || it.carbohydrate || 0) || 0
      const p = parseFloat(it.protein || 0) || 0
      const f = parseFloat(it.fat || 0) || 0
      const fb = parseFloat(it.fiber || 0) || 0
      if (c > 0) carbs = carbs > 0 ? carbs + c : c
      if (p > 0) protein = protein > 0 ? protein + p : p
      if (f > 0) fat = fat > 0 ? fat + f : f
      if (fb > 0) fiber = fiber > 0 ? fiber + fb : fb
    })

    if (calories <= 0 && arr.length > 0) {
      arr.forEach((it) => {
        const cals = parseFloat(it.calories || it.calorie || 0) || 0
        if (cals > 0) calories += cals
      })
    }

    const hasData = carbs > 0 || protein > 0 || fat > 0 || fiber > 0 || calories > 0

    this.setData({
      wheelCarbs: carbs,
      wheelProtein: protein,
      wheelFat: fat,
      wheelFiber: fiber,
      wheelCalories: calories,
      showWheel: hasData
    })
  },

  onCameraError() {
    wx.showToast({ title: '相机不可用', icon: 'none' });
  }
});

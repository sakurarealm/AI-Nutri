const { get } = require('../../../utils/request')

Page({
  data: {
    kind: 'recipe',
    recipeId: '',
    memberId: null,
    date: '',
    image: '',
    title: '',
    calories: 0,
    time: 0,
    reason: '这道菜很适合你今天的饮食计划。',
    nutritionValue: '',  // 动态生成

    carb: 45,
    protein: 25,
    fat: 15,
    fiber: 8,
    unit: '',
    note: '',

    ingredients: [
      { name: '生菜', amount: '200g' },
      { name: '鸡胸肉', amount: '100g' },
      { name: '小番茄', amount: '50g' },
      { name: '胡萝卜', amount: '10g' },
      { name: '黄瓜', amount: '30g' },
      { name: '橄榄油', amount: '15ml' }
    ],

    steps: [
      { title: '准备食材', description: '鸡胸肉切丁，用盐和黑胡椒腌制10分钟，让肉质更加入味', time: 10 },
      { title: '处理蔬菜', description: '生菜、小番茄、胡萝卜、黄瓜洗净切好备用，保持新鲜', time: 8 },
      { title: '烹饪鸡胸肉', description: '平底锅刷少量油，将鸡胸肉煎至两面金黄，确保熟透', time: 6 },
      { title: '制作沙拉', description: '所有食材混合装盘，淋上橄榄油和柠檬汁，轻轻拌匀即可', time: 3 }
    ],

    isFavorited: false,  // 新增：收藏状态
    navPaddingTop: 0,
    navPaddingBottom: 0,
    contentPaddingTop: 0,
    contentPaddingBottom: 0,
    bottomBarPaddingBottom: 0
  },

  async onLoad(options) {
    const sys = wx.getSystemInfoSync();
    const statusBarHeight = sys.statusBarHeight || 0;

    let capsuleRect = null;
    if (typeof wx.getMenuButtonBoundingClientRect === 'function') {
      try {
        capsuleRect = wx.getMenuButtonBoundingClientRect();
      } catch (e) {}
    }

    const navBarHeight =
      capsuleRect && capsuleRect.height
        ? capsuleRect.height + (capsuleRect.top - statusBarHeight) * 2
        : 44;

    const navTotalHeight = statusBarHeight + navBarHeight;
    const safeAreaBottomInset =
      sys.safeArea && typeof sys.screenHeight === 'number'
        ? Math.max(0, sys.screenHeight - sys.safeArea.bottom)
        : 0;

    const dataToSet = {
      navPaddingTop: statusBarHeight + 8,
      navPaddingBottom: 10,
      contentPaddingTop: navTotalHeight + 16,
      contentPaddingBottom: 140 + safeAreaBottomInset,
      bottomBarPaddingBottom: 20 + safeAreaBottomInset
    };

    const kind = options && options.kind ? String(options.kind) : 'recipe';
    const recipeId = options && options.id ? String(options.id) : '';
    const memberId = options && options.memberId ? Number(options.memberId) : null;
    const date = options && options.date ? String(options.date) : this.getTodayDate();

    Object.assign(dataToSet, {
      kind,
      recipeId,
      memberId: Number.isFinite(memberId) && memberId > 0 ? memberId : null,
      date
    });

    this.setData(dataToSet);
    if (String(kind) === 'food') {
      await this.loadFoodDetail();
    } else {
      await this.loadRecipeDetail();
    }
  },

  getTodayDate() {
    const d = new Date();
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  },

  async ensureMemberId() {
    let memberId = Number(this.data.memberId);
    if (Number.isFinite(memberId) && memberId > 0) return memberId;

    const memberKey = (() => {
      try { return String(wx.getStorageSync('nutrition_selected_member') || 'me'); } catch (e) { return 'me'; }
    })();

    try {
      const body = await get('/nutrition/members', {}, { loading: false, showError: false });
      const list = body && Array.isArray(body.data) ? body.data : [];
      const m = list.find(it => String((it && it.memberKey) || '').trim() === String(memberKey || 'me'));
      const id = m && m.id ? Number(m.id) : null;
      if (Number.isFinite(id) && id > 0) {
        this.setData({ memberId: id });
        return id;
      }
    } catch (e) {}
    return null;
  },

  async loadRecipeDetail() {
    const id = String(this.data.recipeId || '').trim();
    if (!id) {
      wx.showToast({ title: '缺少食谱ID', icon: 'none' });
      return;
    }
    const memberId = await this.ensureMemberId();
    const date = String(this.data.date || this.getTodayDate());

    try {
      const body = await get(`/nutrition/recipes/${encodeURIComponent(id)}`, { memberId: memberId ? Number(memberId) : undefined, date }, { loading: true, showError: false });
      const d = body && body.data && typeof body.data === 'object' ? body.data : null;
      if (!d) {
        wx.showToast({ title: '食谱不存在', icon: 'none' });
        return;
      }

      const steps = Array.isArray(d.steps) ? d.steps : [];
      const ingredients = Array.isArray(d.ingredients) ? d.ingredients : [];

      this.setData({
        image: String(d.image || ''),
        title: String(d.title || ''),
        calories: Number(d.calories) || 0,
        time: Number(d.timeMinutes) || 0,
        reason: String(d.reason || ''),
        carb: Number(d.carb) || 0,
        protein: Number(d.protein) || 0,
        fat: Number(d.fat) || 0,
        fiber: Number(d.fiber) || 0,
        ingredients: ingredients.map((it) => ({
          name: String((it && it.name) || ''),
          amount: String((it && it.amount) || '')
        })),
        steps: steps.map((it) => ({
          title: String((it && it.title) || ''),
          description: String((it && it.description) || ''),
          time: Number((it && it.time) || 0)
        })),
        nutritionValue: String(d.nutritionValue || '')
      });
    } catch (e) {
      const status = e && (e.statusCode || (e.response && e.response.statusCode)) ? Number(e.statusCode || (e.response && e.response.statusCode)) : 0;
      if (status === 401 || status === 403) {
        wx.showToast({ title: '登录后才能查看食谱详情', icon: 'none' });
        return;
      }
      wx.showToast({ title: '加载失败', icon: 'none' });
    }
  },

  async loadFoodDetail() {
    const id = String(this.data.recipeId || '').trim();
    if (!id) {
      wx.showToast({ title: '缺少菜品ID', icon: 'none' });
      return;
    }
    const memberId = await this.ensureMemberId();
    const date = String(this.data.date || this.getTodayDate());

    try {
      const body = await get(`/nutrition/foods/${encodeURIComponent(id)}`, { memberId: memberId ? Number(memberId) : undefined, date }, { loading: true, showError: false });
      const d = body && body.data && typeof body.data === 'object' ? body.data : null;
      if (!d) {
        wx.showToast({ title: '菜品不存在', icon: 'none' });
        return;
      }

      this.setData({
        image: String(d.img || ''),
        title: String(d.name || ''),
        calories: Number(d.calorie) || 0,
        time: 0,
        reason: '这道菜可以直接加入你的饮食记录。',
        carb: Number(d.carb) || 0,
        protein: Number(d.protein) || 0,
        fat: Number(d.fat) || 0,
        fiber: Number(d.fiber) || 0,
        unit: String(d.unit || ''),
        note: String(d.note || ''),
        ingredients: [],
        steps: [],
        nutritionValue: String(d.nutrient || '')
      });
    } catch (e) {
      const status = e && (e.statusCode || (e.response && e.response.statusCode)) ? Number(e.statusCode || (e.response && e.response.statusCode)) : 0;
      if (status === 401 || status === 403) {
        wx.showToast({ title: '登录后才能查看菜品详情', icon: 'none' });
        return;
      }
      wx.showToast({ title: '加载失败', icon: 'none' });
    }
  },

  goBack() {
    wx.navigateBack();
  },

  addToFavorites() {
    this.setData({ isFavorited: !this.data.isFavorited });
    wx.showToast({ 
      title: this.data.isFavorited ? '已收藏' : '已取消收藏', 
      icon: 'success' 
    });
  },

  startCooking() {
    wx.showToast({ title: '开始制作啦～', icon: 'success' });
  }
});

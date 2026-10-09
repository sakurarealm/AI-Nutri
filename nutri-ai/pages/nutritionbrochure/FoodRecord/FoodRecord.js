const { get, post } = require('../../../utils/request')

Page({
  data: {
    from: '',
    returnTarget: null,
    targetLabel: '',
    memberId: null,
    recordDate: '',
    mealType: '',
    keyword: '',
    recentFoods: [],
    filteredRecentFoods: [],
    commonFoods: [
      { name: '米饭', unit: '1碗', calorie: 260, carb: 80, protein: 5, fat: 1, fiber: 0, nutrient: '80克碳水', img: 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=200&q=80' },
      { name: '煎蛋', unit: '2个', calorie: 190, carb: 1, protein: 15, fat: 14, fiber: 0, nutrient: '15克蛋白', img: 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?auto=format&fit=crop&w=200&q=80' },
      { name: '鸡胸肉', unit: '150克', calorie: 250, carb: 0, protein: 35, fat: 6, fiber: 0, nutrient: '35克蛋白', img: 'https://images.unsplash.com/photo-1604909053197-45c574b3c0c9?auto=format&fit=crop&w=200&q=80' },
      { name: '蔬菜沙拉', unit: '1份', calorie: 120, carb: 10, protein: 3, fat: 6, fiber: 5, nutrient: '高纤维', img: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=200&q=80' },
      { name: '牛奶', unit: '250ml', calorie: 150, carb: 12, protein: 8, fat: 8, fiber: 0, nutrient: '8克蛋白', img: 'https://images.unsplash.com/photo-1563636619-e9143da7973b?auto=format&fit=crop&w=200&q=80' }
    ],
    serverCommonFoods: [],
    serverSearchFoods: [],
    filteredCommonFoods: [],
    form: {
      name: '',
      unit: '',
      calorie: '',
      nutrient: '',
      carb: '',
      protein: '',
      fat: '',
      fiber: '',
      img: '',
      note: ''
    }
  },

  onLoad(options) {
    const from = options && options.from ? String(options.from) : '';
    const raw = options && options.target ? String(options.target) : '';
    let target = null;
    if (raw) {
      const candidates = [];
      candidates.push(raw);
      try { candidates.unshift(decodeURIComponent(raw)); } catch (e) {}
      for (let i = 0; i < candidates.length; i++) {
        try {
          const parsed = JSON.parse(candidates[i]);
          if (parsed && typeof parsed === 'object') {
            target = parsed;
            break;
          }
        } catch (e) {}
      }
    }
    const memberId = options && options.memberId ? Number(options.memberId) : null
    const date = options && options.date ? String(options.date) : ''
    const recentFoods = this.readRecentFoods();
    this.setData({
      from,
      returnTarget: target,
      targetLabel: this.computeTargetLabel(target),
      memberId: Number.isNaN(memberId) ? null : memberId,
      recordDate: date,
      mealType: this.computeMealType(target),
      recentFoods
    });
    this.applyKeywordFilter('');
    this.fetchRecommendFoods();
  },

  computeTargetLabel(target) {
    const t = target && typeof target === 'object' ? target : null;
    if (!t) return '';
    if (t.type === 'snack') return '加餐';
    const key = String(t.mealKey || '');
    if (key === 'breakfastFoods') return '早餐';
    if (key === 'lunchFoods') return '午餐';
    if (key === 'dinnerFoods') return '晚餐';
    return '';
  },

  computeMealType(target) {
    const t = target && typeof target === 'object' ? target : null
    if (!t) return ''
    if (t.type === 'snack') return 'SNACK'
    const key = String(t.mealKey || '')
    if (key === 'breakfastFoods') return 'BREAKFAST'
    if (key === 'lunchFoods') return 'LUNCH'
    if (key === 'dinnerFoods') return 'DINNER'
    return ''
  },

  goBack() {
    wx.navigateBack({ delta: 1 });
  },

  onKeywordInput(e) {
    const v = e && e.detail ? String(e.detail.value || '') : '';
    this.setData({ keyword: v });
    this.applyKeywordFilter(v);
    this.scheduleSearchFoods(v);
  },

  clearKeyword() {
    this.setData({ keyword: '' });
    this.applyKeywordFilter('');
    this.scheduleSearchFoods('');
  },

  applyKeywordFilter(keyword) {
    const k = String(keyword || '').trim();
    const filterBy = (arr) => {
      const list = Array.isArray(arr) ? arr : [];
      if (!k) return list;
      return list.filter((it) => {
        const name = String(it && it.name ? it.name : '');
        return name.includes(k);
      });
    };
    const baseCommon = k
      ? (Array.isArray(this.data.serverSearchFoods) && this.data.serverSearchFoods.length ? this.data.serverSearchFoods : this.data.commonFoods)
      : (Array.isArray(this.data.serverCommonFoods) && this.data.serverCommonFoods.length ? this.data.serverCommonFoods : this.data.commonFoods)
    this.setData({
      filteredRecentFoods: filterBy(this.data.recentFoods),
      filteredCommonFoods: filterBy(baseCommon)
    });
  },

  onPickFood(e) {
    const now = Date.now()
    const food = e && e.currentTarget && e.currentTarget.dataset ? e.currentTarget.dataset.food : null
    const f = food && typeof food === 'object' ? food : null
    if (!f) return
    const foodKey = (f.id ? `catalog_${f.id}` : `name_${(f.name || '')}`) + (f.unit || '')
    if (this._lastPickTime && (now - this._lastPickTime) < 500 && this._lastPickKey === foodKey) return
    this._lastPickTime = now
    this._lastPickKey = foodKey

    const foodId = f.id ? Number(f.id) : 0
    if (foodId > 0) {
      this.pickCatalogFoodWithPortion(f)
      return
    }
    const item = this.normalizeFood(f);
    if (!item.name) return;
    this.persistRecentFood(item);
    this.recordPickToServer({ foodCatalogId: null, portionMultiplier: 1, food: item })
    this.returnFoodToCaller(item);
  },

  onFormInput(e) {
    const field = e && e.currentTarget && e.currentTarget.dataset ? String(e.currentTarget.dataset.field || '') : '';
    if (!field) return;
    const v = e && e.detail ? e.detail.value : '';
    this.setData({ [`form.${field}`]: v });
  },

  chooseImage() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const filePath = res && res.tempFiles && res.tempFiles[0] ? res.tempFiles[0].tempFilePath : '';
        if (!filePath) return;
        this.setData({ 'form.img': filePath });
      },
      fail: () => {}
    });
  },

  clearImage() {
    this.setData({ 'form.img': '' });
  },

  submitManual() {
    const f = this.data.form || {};
    const rawName = String(f.name || '').trim();
    if (!rawName) {
      wx.showToast({ title: '先填写食物名称', icon: 'none' });
      return;
    }
    const item = this.normalizeFood({
      name: rawName,
      unit: String(f.unit || '').trim(),
      calorie: f.calorie,
      nutrient: String(f.nutrient || '').trim(),
      carb: f.carb,
      protein: f.protein,
      fat: f.fat,
      fiber: f.fiber,
      img: String(f.img || ''),
      note: String(f.note || '').trim()
    });
    this.persistRecentFood(item);
    this.recordPickToServer({ foodCatalogId: null, portionMultiplier: 1, food: item })
    this.returnFoodToCaller(item);
  },

  normalizeFood(food) {
    const f = food && typeof food === 'object' ? food : {};
    const name = String(f.name || '').trim();
    const unit = String(f.unit || '').trim();
    const img = String(f.img || f.image || '').trim();
    const note = String(f.note || '').trim();

    const calorieNum = Number(f.calorie);
    const calorie = Number.isNaN(calorieNum) ? 0 : Math.max(0, Math.round(calorieNum));

    const carbNum = Number(f.carb);
    const proteinNum = Number(f.protein);
    const fatNum = Number(f.fat);
    const fiberNum = Number(f.fiber);
    const carb = Number.isNaN(carbNum) ? 0 : Math.max(0, carbNum);
    const protein = Number.isNaN(proteinNum) ? 0 : Math.max(0, proteinNum);
    const fat = Number.isNaN(fatNum) ? 0 : Math.max(0, fatNum);
    const fiber = Number.isNaN(fiberNum) ? 0 : Math.max(0, fiberNum);

    let nutrient = String(f.nutrient || '').trim();
    if (!nutrient) {
      if (carb) nutrient = `${carb}克碳水`;
      else if (protein) nutrient = `${protein}克蛋白`;
      else if (fat) nutrient = `${fat}克脂肪`;
      else if (fiber) nutrient = `${fiber}克纤维`;
      else if (calorie) nutrient = `约${calorie}千卡`;
      else nutrient = '待识别';
    }

    return { name, unit, nutrient, img, note, calorie, carb, protein, fat, fiber };
  },

  scheduleSearchFoods(keyword) {
    if (this._searchTimer) clearTimeout(this._searchTimer)
    const k = String(keyword || '').trim()
    if (!k) {
      this.setData({ serverSearchFoods: [] })
      this.applyKeywordFilter('')
      return
    }
    this._searchTimer = setTimeout(() => {
      this._searchTimer = null
      this.searchFoodsNow(k)
    }, 250)
  },

  async searchFoodsNow(keyword) {
    const q = String(keyword || '').trim()
    if (!q) return
    try {
      const body = await get('/nutrition/foods/search', { q, limit: 30 }, { loading: false, showError: false })
      const list = body && body.data ? body.data : []
      const foods = (Array.isArray(list) ? list : []).map((it) => this.normalizeServerFood(it))
      this.setData({ serverSearchFoods: foods })
      this.applyKeywordFilter(q)
    } catch (e) {}
  },

  async fetchRecommendFoods() {
    const mealType = String(this.data.mealType || '').trim()
    try {
      const body = await get('/nutrition/foods/recommend', { mealType, limit: 12 }, { loading: false, showError: false })
      const list = body && body.data ? body.data : []
      const foods = (Array.isArray(list) ? list : []).map((it) => this.normalizeServerFood(it))
      if (foods.length) {
        this.setData({ serverCommonFoods: foods }, () => this.applyKeywordFilter(this.data.keyword))
      }
    } catch (e) {}
  },

  normalizeServerFood(food) {
    const f = food && typeof food === 'object' ? food : {}
    return {
      id: Number(f.id) || 0,
      name: String(f.name || '').trim(),
      unit: String(f.unit || '').trim(),
      nutrient: String(f.nutrient || '').trim(),
      calorie: Number(f.calorie) || 0,
      carb: Number(f.carb) || 0,
      protein: Number(f.protein) || 0,
      fat: Number(f.fat) || 0,
      fiber: Number(f.fiber) || 0,
      img: String(f.img || '').trim(),
      note: String(f.note || '').trim(),
      portionMultiplier: Number(f.portionMultiplier) || 1
    }
  },

  pickCatalogFoodWithPortion(food) {
    const f = food && typeof food === 'object' ? food : {}
    const options = ['0.5份', '1份', '1.5份', '2份']
    wx.showActionSheet({
      itemList: options,
      success: (res) => {
        const idx = res && typeof res.tapIndex === 'number' ? res.tapIndex : -1
        if (idx < 0 || idx >= options.length) return
        const chosen = String(options[idx] || '')
        const m = Number(chosen.replace('份', ''))
        const multiplier = Number.isNaN(m) ? 1 : m
        this.calcAndPickCatalogFood(f, multiplier, chosen)
      },
      fail: () => {}
    })
  },

  async calcAndPickCatalogFood(food, multiplier, unitLabel) {
    const f = food && typeof food === 'object' ? food : {}
    const foodId = f.id ? Number(f.id) : 0
    if (!foodId) return
    try {
      const body = await post('/nutrition/foods/portion', { foodId, multiplier, unitLabel }, { loading: true, showError: true })
      const data = body && body.data && typeof body.data === 'object' ? body.data : null
      if (!data) return
      const picked = this.normalizeServerFood(data)
      const item = this.normalizeFood(picked)
      if (!item.name) return
      this.persistRecentFood(item)
      this.recordPickToServer({
        foodCatalogId: foodId,
        portionMultiplier: multiplier,
        food: item
      })
      this.returnFoodToCaller(item)
    } catch (e) {}
  },

  async recordPickToServer({ foodCatalogId, portionMultiplier, food } = {}) {
    const item = food && typeof food === 'object' ? food : null
    if (!item || !item.name) return
    const memberId = this.data.memberId
    const date = String(this.data.recordDate || '').trim()
    const mealType = String(this.data.mealType || '').trim()
    try {
      await post('/nutrition/foods/picks', {
        memberId: memberId && !Number.isNaN(Number(memberId)) ? Number(memberId) : null,
        date,
        mealType,
        foodCatalogId: foodCatalogId ? Number(foodCatalogId) : null,
        portionMultiplier: portionMultiplier ? Number(portionMultiplier) : 1,
        name: item.name,
        unit: item.unit,
        calorie: item.calorie,
        nutrient: item.nutrient,
        carb: item.carb,
        protein: item.protein,
        fat: item.fat,
        fiber: item.fiber,
        img: item.img,
        note: item.note
      }, { loading: false, showError: false })
    } catch (e) {}
  },

  returnFoodToCaller(item) {
    const from = String(this.data.from || '');
    const target = this.data.returnTarget || {};

    if (from === 'DataPage') {
      const ec = typeof this.getOpenerEventChannel === 'function' ? this.getOpenerEventChannel() : null;
      if (ec && typeof ec.emit === 'function') {
        ec.emit('foodSelected', { target, food: item });
      }

      wx.showToast({ title: `已添加${item.name}`, icon: 'success' });
      wx.navigateBack({ delta: 1 });
      return;
    }

    this.persistStandaloneRecord(item);
    wx.showToast({ title: '已保存记录', icon: 'success' });
    wx.navigateBack({ delta: 1 });
  },

  readRecentFoods() {
    try {
      const arr = wx.getStorageSync('nutrition_recent_foods');
      const list = Array.isArray(arr) ? arr : [];
      return list.slice(0, 2);
    } catch (e) {
      return [];
    }
  },

  persistRecentFood(item) {
    const list = this.readRecentFoods();
    const name = String(item && item.name ? item.name : '');
    const next = [item, ...list.filter((it) => String(it && it.name ? it.name : '') !== name)];
    try {
      wx.setStorageSync('nutrition_recent_foods', next.slice(0, 2));
    } catch (e) {}
    this.setData({ recentFoods: next.slice(0, 2) });
    this.applyKeywordFilter(this.data.keyword);
  },

  persistStandaloneRecord(item) {
    try {
      const list = wx.getStorageSync('nutrition_food_records');
      const arr = Array.isArray(list) ? list : [];
      const record = { id: `rec_${Date.now()}_${Math.floor(Math.random() * 10000)}`, createdAt: Date.now(), food: item };
      wx.setStorageSync('nutrition_food_records', [record, ...arr].slice(0, 200));
    } catch (e) {}
  }
});

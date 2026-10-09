const { get } = require('../../../utils/request')

Page({
  data: {
    navPaddingTopPx: 0,
    memberId: null,
    memberKey: 'me',
    date: '',
    todaySummary: '',
    recipeList: [],
    foodKeyword: '',
    foodList: [],
    foodSearchList: []
  },

  onLoad() {
    const sys = wx.getSystemInfoSync ? wx.getSystemInfoSync() : {};
    const statusBarHeightPx = Number(sys.statusBarHeight) || 0;
    const navPaddingTopPx = Math.max(0, Math.round(statusBarHeightPx + 12));
    const memberKey = (() => {
      try { return String(wx.getStorageSync('nutrition_selected_member') || 'me'); } catch (e) { return 'me'; }
    })();
    this.setData({ navPaddingTopPx, memberKey, date: this.getTodayDate() });
  },

  onShow() {
    this.loadRecommendations();
    this.fetchFoodRecommend();
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

    const memberKey = String(this.data.memberKey || 'me').trim() || 'me';
    try {
      const body = await get('/nutrition/members', {}, { loading: false, showError: false });
      const list = body && Array.isArray(body.data) ? body.data : [];
      const m = list.find(it => String((it && it.memberKey) || '').trim() === memberKey);
      const id = m && m.id ? Number(m.id) : null;
      if (Number.isFinite(id) && id > 0) {
        this.setData({ memberId: id });
        return id;
      }
    } catch (e) {}
    return null;
  },

  async loadRecommendations() {
    const date = String(this.data.date || this.getTodayDate());
    const memberId = await this.ensureMemberId();
    if (!memberId) {
      this.setData({
        todaySummary: '登录后才能基于营养册与人体图进行个性化推荐。',
        recipeList: []
      });
      return;
    }

    try {
      const body = await get('/nutrition/recipes/recommend', { memberId: Number(memberId), date, limit: 6 }, { loading: true, loadingText: '生成推荐中...', showError: false });
      const payload = body && body.data && typeof body.data === 'object' ? body.data : {};
      const list = Array.isArray(payload.recipes) ? payload.recipes : [];
      const cards = list.map((it) => {
        const t = it && typeof it === 'object' ? it : {};
        const tm = Number(t.timeMinutes) || 0;
        return {
          id: t.id,
          title: String(t.title || ''),
          calories: Number(t.calories) || 0,
          timeMinutes: tm,
          time: `${tm}分钟`,
          reason: String(t.reason || ''),
          image: String(t.image || '')
        };
      }).filter(x => x && x.id);

      this.setData({
        date: String(payload.date || date),
        todaySummary: String(payload.tips || ''),
        recipeList: cards
      });
    } catch (e) {
      const status = e && (e.statusCode || (e.response && e.response.statusCode)) ? Number(e.statusCode || (e.response && e.response.statusCode)) : 0;
      if (status === 401 || status === 403) {
        this.setData({
          todaySummary: '登录后才能基于营养册与人体图进行个性化推荐。',
          recipeList: []
        });
        return;
      }
      this.setData({
        todaySummary: '推荐生成失败了，稍后再试试。',
        recipeList: []
      });
    }
  },

  getCurrentMealType() {
    const h = new Date().getHours();
    if (h < 10) return 'BREAKFAST';
    if (h < 15) return 'LUNCH';
    if (h < 21) return 'DINNER';
    return 'SNACK';
  },

  onFoodKeywordInput(e) {
    const v = e && e.detail ? String(e.detail.value || '') : '';
    this.setData({ foodKeyword: v });
    this.scheduleFoodSearch(v);
  },

  clearFoodKeyword() {
    this.setData({ foodKeyword: '', foodSearchList: [] });
    if (Array.isArray(this.data.foodList) && this.data.foodList.length) return;
    this.fetchFoodRecommend();
  },

  scheduleFoodSearch(keyword) {
    if (this._foodSearchTimer) clearTimeout(this._foodSearchTimer);
    const q = String(keyword || '').trim();
    if (!q) {
      this.setData({ foodSearchList: [] });
      return;
    }
    this._foodSearchTimer = setTimeout(() => {
      this._foodSearchTimer = null;
      this.searchFoodsNow(q);
    }, 250);
  },

  normalizeFoodCard(food) {
    const f = food && typeof food === 'object' ? food : {};
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
    };
  },

  async fetchFoodRecommend() {
    const memberId = await this.ensureMemberId();
    if (!memberId) {
      this.setData({ foodList: [], foodSearchList: [] });
      return;
    }
    const mealType = this.getCurrentMealType();
    try {
      const body = await get('/nutrition/foods/recommend', { mealType, limit: 12 }, { loading: false, showError: false });
      const list = body && body.data ? body.data : [];
      const foods = (Array.isArray(list) ? list : []).map((it) => this.normalizeFoodCard(it)).filter((x) => x && x.id);
      this.setData({ foodList: foods });
    } catch (e) {}
  },

  async searchFoodsNow(keyword) {
    const memberId = await this.ensureMemberId();
    if (!memberId) return;
    const q = String(keyword || '').trim();
    if (!q) return;
    try {
      const body = await get('/nutrition/foods/search', { q, limit: 30 }, { loading: false, showError: false });
      const list = body && body.data ? body.data : [];
      const foods = (Array.isArray(list) ? list : []).map((it) => this.normalizeFoodCard(it)).filter((x) => x && x.id);
      this.setData({ foodSearchList: foods });
    } catch (e) {}
  },

  goBack() {
    wx.navigateBack();
  },

  goToRecipeDetail(e) {
    const recipe = e.currentTarget.dataset.recipe;
    const id = recipe && recipe.id ? String(recipe.id) : '';
    if (!id) return;
    const memberId = this.data.memberId ? String(this.data.memberId) : '';
    const date = String(this.data.date || this.getTodayDate());
    wx.navigateTo({
      url: `/pages/nutritionbrochure/food-detail/food-detail?id=${encodeURIComponent(id)}&memberId=${encodeURIComponent(memberId)}&date=${encodeURIComponent(date)}`
    });
  },

  goToFoodDetail(e) {
    const food = e && e.currentTarget && e.currentTarget.dataset ? e.currentTarget.dataset.food : null;
    const id = food && food.id ? String(food.id) : '';
    if (!id) return;
    const memberId = this.data.memberId ? String(this.data.memberId) : '';
    const date = String(this.data.date || this.getTodayDate());
    wx.navigateTo({
      url: `/pages/nutritionbrochure/food-detail/food-detail?kind=food&id=${encodeURIComponent(id)}&memberId=${encodeURIComponent(memberId)}&date=${encodeURIComponent(date)}`
    });
  }
});

const VIP_CACHE_KEY = 'vip_cache';
const FAMILY_MAX_SEATS = 7;
const FAMILY_SEAT_PRICE = 10;
const request = require('../../../utils/request.js');
const us = require('../../../utils/userState.js');

function pad2(n) {
  const s = String(n);
  return s.length === 1 ? `0${s}` : s;
}

function formatDate(d) {
  const dt = d instanceof Date ? d : new Date(d);
  if (Number.isNaN(dt.getTime())) return '';
  return `${dt.getFullYear()}-${pad2(dt.getMonth() + 1)}-${pad2(dt.getDate())}`;
}

function addDays(date, days) {
  const base = date instanceof Date ? date : new Date(date);
  const t = base.getTime();
  if (Number.isNaN(t)) return new Date(Date.now() + days * 86400000);
  return new Date(t + days * 86400000);
}

function normalizeExpireDate(expireDate) {
  if (!expireDate || expireDate === '永久') return null;
  const dt = new Date(expireDate);
  if (Number.isNaN(dt.getTime())) return null;
  return dt;
}

Page({
  data: {
    user: {},
    avatarInitial: '游',
    vipInfo: {
      level: 0,
      levelName: '普通会员',
      expireDate: null,
      benefits: []
    },
    seatSlots: [0, 1, 2, 3, 4, 5, 6],
    benefitCards: [
      { key: 'recipe', title: '智能菜谱', sub: '更省心的搭配建议', color: '#10b981' },
      { key: 'monitor', title: '健康监测', sub: '趋势追踪更直观', color: '#2563eb' },
      { key: 'dietitian', title: '营养师问诊', sub: '专业版每月4次预约', color: '#f97316' },
      { key: 'family', title: '家庭账户', sub: '加座10元/月，最多7位', color: '#a855f7' }
    ],
    plans: [
      {
        id: 'basic',
        title: '基础版',
        desc: '智能菜谱 + 健康监测',
        price: 39,
        originPrice: '',
        per: '￥39/月',
        days: 30,
        supportAutoRenew: true,
        tag: '',
        accent: '#22c55e',
        highlights: ['智能菜谱推荐', '健康数据趋势', '更细的饮食记录'],
        details: ['智能菜谱推荐', '健康监测趋势追踪', '饮食记录与统计', '基础分析报告']
      },
      {
        id: 'pro',
        title: '专业版',
        desc: '含专业营养师季度视频问诊（每月4次，需预约）',
        price: 99,
        originPrice: '',
        per: '￥99/月',
        days: 30,
        supportAutoRenew: true,
        tag: '推荐',
        accent: '#f59e0b',
        highlights: ['每月 4 次问诊', '更详细数据分析', '专属计划更精准'],
        details: ['智能菜谱推荐', '健康监测趋势追踪', '更详细的数据分析', '专业营养师视频问诊（每月4次，需预约）', '更精细的计划建议']
      }
    ],
    selectedPlanId: 'pro',
    planDetailOpen: { pro: true },
    selectedPlan: null,
    familySeats: 0,
    familyFee: 0,
    totalAmount: 0,
    amountPulse: false,
    autoRenew: true
  },

  onLoad() {
    this.refresh();
    this.syncPlansFromServer();
    this.syncVipFromServer();
  },

  onShow() {
    this.refresh();
    this.syncVipFromServer();
  },

  refresh() {
    const state = us.load()
    const user = (state && state.user) || wx.getStorageSync('user_cache') || wx.getStorageSync('userProfile') || wx.getStorageSync('userInfo') || {};
    const nickname = String((user && user.nickname) || '游客').trim() || '游客';
    const avatarInitial = nickname.substring(0, 1);
    const vipInfo = (state && state.vip && us.mapVipInfo(state.vip)) || wx.getStorageSync(VIP_CACHE_KEY) || {};
    const normalized = this.normalizeVipInfo(vipInfo);

    const preferPlanId = normalized.planId && (this.data.plans || []).some(p => p.id === normalized.planId) ? normalized.planId : this.data.selectedPlanId;
    const selectedPlan = (this.data.plans || []).find(p => p.id === preferPlanId) || this.data.plans?.[0] || null;
    const autoRenew = selectedPlan?.supportAutoRenew ? this.data.autoRenew : false;
    const familySeats = 0;
    const familyFee = this.calcFamilyFee(familySeats);
    const totalAmount = this.calcTotalAmount(selectedPlan, familySeats);

    this.setData({
      user,
      avatarInitial,
      vipInfo: normalized,
      selectedPlanId: selectedPlan?.id || this.data.selectedPlanId,
      selectedPlan,
      autoRenew,
      familySeats,
      familyFee,
      totalAmount
    });
  },

  async syncVipFromServer() {
    try {
      const body = await request.get('/user/vip', {}, { loading: false, showError: false });
      const data = body && typeof body.data === 'object' ? body.data : null;
      if (!data) return;
      try { wx.setStorageSync(VIP_CACHE_KEY, data); } catch (e) {}
      us.set('vip', data)
      const normalized = this.normalizeVipInfo(data);
      const preferPlanId = normalized.planId && (this.data.plans || []).some(p => p.id === normalized.planId) ? normalized.planId : this.data.selectedPlanId;
      const selectedPlan = (this.data.plans || []).find(p => p.id === preferPlanId) || this.data.plans?.[0] || null;
      const autoRenew = selectedPlan?.supportAutoRenew ? !!data.autoRenew : false;
      const familySeats = 0;
      const familyFee = this.calcFamilyFee(familySeats);
      const totalAmount = this.calcTotalAmount(selectedPlan, familySeats);
      this.setData({
        vipInfo: normalized,
        selectedPlanId: selectedPlan?.id || this.data.selectedPlanId,
        selectedPlan,
        autoRenew,
        familySeats,
        familyFee,
        totalAmount
      });
    } catch (e) {}
  },

  async syncPlansFromServer() {
    try {
      const body = await request.get('/vip/plans', {}, { loading: false, showError: false });
      const list = body && Array.isArray(body.data) ? body.data : [];
      if (!list.length) return;
      const mapped = list.map((p) => ({
        id: String(p.id || ''),
        title: String(p.title || ''),
        desc: String(p.desc || ''),
        price: Number(p.price) || 0,
        originPrice: '',
        per: String(p.per || ''),
        days: Number(p.days) || 30,
        supportAutoRenew: !!p.supportAutoRenew,
        tag: String(p.tag || ''),
        accent: String(p.accent || '#22c55e'),
        highlights: Array.isArray(p.highlights) ? p.highlights : [],
        details: Array.isArray(p.details) ? p.details : []
      })).filter((p) => p.id);
      if (!mapped.length) return;
      const open = { ...this.data.planDetailOpen };
      for (const p of mapped) {
        if (open[p.id] == null) open[p.id] = p.id === this.data.selectedPlanId;
      }
      this.setData({ plans: mapped, planDetailOpen: open }, () => this.refresh());
    } catch (e) {}
  },

  normalizeVipInfo(v = {}) {
    const level = v.level ?? v.vipLevel ?? 0;
    const planId = v.planId ?? v.plan ?? v.planType ?? '';
    const levelNames = ['普通会员', '基础版', '专业版', '专业版'];
    let levelName = v.levelName ?? levelNames[level] ?? '普通会员';
    if (!levelName || levelName === '普通会员') {
      if (planId === 'basic') levelName = '基础版';
      if (planId === 'pro') levelName = '专业版';
    }
    const expireDate = v.expireDate ?? v.expireTime ?? v.expire ?? null;

    const benefits = Array.isArray(v.benefits) && v.benefits.length > 0 ? v.benefits : this.getBenefitsByPlan(planId || (level >= 2 ? 'pro' : level === 1 ? 'basic' : ''));
    const familySeats = this.normalizeFamilySeats(v.familySeats ?? v.familyCount ?? 0);

    return { level: Number(level) || 0, levelName, expireDate, benefits, planId, familySeats };
  },

  selectPlan(e) {
    const id = e.currentTarget.dataset.id;
    const selectedPlan = (this.data.plans || []).find(p => p.id === id) || null;
    if (!selectedPlan) return;
    const familySeats = this.normalizeFamilySeats(this.data.familySeats);
    const familyFee = this.calcFamilyFee(familySeats);
    const totalAmount = this.calcTotalAmount(selectedPlan, familySeats);
    this.setData({
      selectedPlanId: selectedPlan.id,
      selectedPlan,
      autoRenew: selectedPlan.supportAutoRenew ? this.data.autoRenew : false,
      familySeats,
      familyFee,
      totalAmount
    });
    this.setData({ planDetailOpen: { ...this.data.planDetailOpen, [selectedPlan.id]: true } });
    this.pulseAmount();
  },

  togglePlanDetail(e) {
    const id = String(e.currentTarget.dataset.id || '');
    if (!id) return;
    const next = { ...this.data.planDetailOpen, [id]: !this.data.planDetailOpen?.[id] };
    this.setData({ planDetailOpen: next });
  },

  toggleAutoRenew(e) {
    this.setData({ autoRenew: !!e.detail.value });
  },

  normalizeFamilySeats(n) {
    const x = Number(n);
    if (Number.isNaN(x)) return 0;
    return Math.max(0, Math.min(FAMILY_MAX_SEATS, Math.floor(x)));
  },

  calcFamilyFee(seats) {
    return this.normalizeFamilySeats(seats) * FAMILY_SEAT_PRICE;
  },

  calcTotalAmount(plan, seats) {
    const base = plan ? Number(plan.price) : 0;
    const b = Number.isNaN(base) ? 0 : base;
    return b + this.calcFamilyFee(seats);
  },

  changeFamilySeats(e) {
    wx.showToast({ title: '家庭账户筹备中', icon: 'none' });
    this.setData({ familySeats: 0, familyFee: 0, totalAmount: this.calcTotalAmount(this.data.selectedPlan, 0) });
    return;
    const op = e.currentTarget.dataset.op;
    const current = this.normalizeFamilySeats(this.data.familySeats);
    let next = current;
    if (op === 'plus') next = current + 1;
    if (op === 'minus') next = current - 1;
    next = this.normalizeFamilySeats(next);
    const familyFee = this.calcFamilyFee(next);
    const totalAmount = this.calcTotalAmount(this.data.selectedPlan, next);
    this.setData({ familySeats: next, familyFee, totalAmount });
    this.pulseAmount();
  },

  pulseAmount() {
    if (this._pulseTimer) clearTimeout(this._pulseTimer);
    this.setData({ amountPulse: true });
    this._pulseTimer = setTimeout(() => {
      this.setData({ amountPulse: false });
    }, 260);
  },

  getBenefitsByPlan(planId) {
    if (planId === 'pro') {
      return ['智能菜谱', '健康监测', '专业营养师视频问诊（每月4次，需预约）', '更详细的数据分析'];
    }
    if (planId === 'basic') {
      return ['智能菜谱', '健康监测'];
    }
    return ['基础功能使用'];
  },

  openBenefits() {
    const v = this.data.vipInfo || {};
    const planId = v.planId || (v.level >= 2 ? 'pro' : v.level === 1 ? 'basic' : '');
    const benefits = (Array.isArray(v.benefits) && v.benefits.length > 0) ? v.benefits : this.getBenefitsByPlan(planId);
    const familySeats = this.normalizeFamilySeats(v.familySeats);
    const lines = [];
    for (const x of benefits) lines.push(`• ${x}`);
    if (familySeats > 0) lines.push(`• 家庭账户加座：${familySeats} 位（￥${familySeats * FAMILY_SEAT_PRICE}/月）`);
    const list = lines.length > 0 ? lines.join('\n') : '• 暂无权益信息';
    wx.showModal({
      title: '会员权益',
      content: list,
      showCancel: false,
      confirmText: '知道了'
    });
  },

  showServiceAgreement() {
    wx.showModal({
      title: '会员服务协议',
      content: '会员为虚拟服务，开通/续费后权益立即生效。家庭账户按加座数计费；视频问诊需预约。',
      showCancel: false,
      confirmText: '我知道了'
    });
  },

  showAutoRenewRules() {
    wx.showModal({
      title: '自动续费规则',
      content: '自动续费为按月订阅，到期前至少24小时可关闭。关闭后仍可使用至当前订阅周期结束。',
      showCancel: false,
      confirmText: '我知道了'
    });
  },

  showPrivacy() {
    wx.showModal({
      title: '隐私政策',
      content: '我们会在提供服务所必需的范围内收集与使用信息，并采取合理措施保护您的数据安全。',
      showCancel: false,
      confirmText: '我知道了'
    });
  },

  buyNow() {
    let token = '';
    try {
      const app = getApp();
      token = (app && app.getAuthToken) ? app.getAuthToken() : (wx.getStorageSync('token') || wx.getStorageSync('AUTH_TOKEN'));
    } catch (e) {
      token = wx.getStorageSync('token') || wx.getStorageSync('AUTH_TOKEN');
    }
    if (!token) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      wx.navigateTo({ url: '/pages/login/login?auto=1' });
      return;
    }
    const plan = this.data.selectedPlan;
    if (!plan) return;
    const familySeats = 0;
    const totalAmount = this.calcTotalAmount(plan, familySeats);
    const autoText = plan.supportAutoRenew ? (this.data.autoRenew ? '\n已开启自动续费' : '\n未开启自动续费') : '';
    const familyText = familySeats > 0 ? `\n家庭账户加座：${familySeats} 位（￥${familySeats * FAMILY_SEAT_PRICE}/月）` : '';

    wx.showModal({
      title: '确认支付',
      content: `开通套餐：${plan.title}\n支付金额：￥${totalAmount}/月${familyText}${autoText}`,
      confirmText: '立即支付',
      success: async (res) => {
        if (!res.confirm) return;
        await this.createAndPayOrder(plan);
      }
    });
  },

  async createAndPayOrder(plan) {
    try {
      wx.showLoading({ title: '下单中...' });
      const orderBody = await request.post('/vip/orders', {
        planId: plan.id,
        months: 1,
        familySeats: 0,
        autoRenew: !!this.data.autoRenew
      }, { loading: false, showError: false });
      const order = orderBody && typeof orderBody.data === 'object' ? orderBody.data : null;
      if (!order || !order.id) {
        wx.hideLoading();
        wx.showToast({ title: '下单失败', icon: 'none' });
        return;
      }

      wx.showLoading({ title: '支付中...' });
      await request.post(`/vip/orders/${Number(order.id)}/pay`, {}, { loading: false, showError: false });
      wx.hideLoading();
      wx.showToast({ title: '开通成功', icon: 'success' });
      await this.syncVipFromServer();
      this.refresh();
    } catch (e) {
      wx.hideLoading();
      wx.showToast({ title: '支付失败', icon: 'none' });
    }
  }
});

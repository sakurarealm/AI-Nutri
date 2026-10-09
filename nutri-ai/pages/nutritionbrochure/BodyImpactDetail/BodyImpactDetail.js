const { get, post } = require('../../../utils/request')

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 44,
    safeAreaBottom: 0,
    memberId: null,
    memberKey: 'me',
    selectedDate: '',
    regionKey: '',
    organTabs: [],
    scoreClass: '',
    statusText: '',
    trendMin: '--',
    trendMax: '--',
    trendAvg: '--',
    organEmoji: '❤️',
    dietImpactText: '',
    organ: {
      key: 'heart',
      name: '心脏',
      icon: 'heart',
      intro: '',
      score: 0,
      summary: '',
      metrics: [],
      suggestions: [],
      recommendations: [],
      dailyTip: '',
      aiPlan: '',
      achievements: []
    },
    history: [],
    expandedMetric: null,
    cardAnimated: false,
    showSuccess: false,
    successMessage: '',
    particleCtx: null,
    particles: [],
    animationFrame: null
  },

  // Computed properties as methods
  getRingDash() {
    const score = Math.max(0, Math.min(100, this.data.organ.score));
    const circumference = 2 * Math.PI * 52;
    const progress = (score / 100) * circumference;
    return `${progress} ${circumference}`;
  },

  getScoreClass() {
    if (this.data.organ.score >= 70) return 'score-good';
    if (this.data.organ.score >= 45) return 'score-neutral';
    return 'score-bad';
  },

  getStatusText() {
    if (this.data.organ.score >= 70) return '状态优秀';
    if (this.data.organ.score >= 45) return '需要关注';
    return '亟需改善';
  },

  getTrendMin() {
    if (!this.data.history.length) return '--';
    return Math.min(...this.data.history);
  },

  getTrendMax() {
    if (!this.data.history.length) return '--';
    return Math.max(...this.data.history);
  },

  getTrendAvg() {
    if (!this.data.history.length) return '--';
    const sum = this.data.history.reduce((a, b) => a + b, 0);
    return Math.round(sum / this.data.history.length);
  },

  // Lifecycle
  async onLoad(options) {
    this._skipOnShowOnce = true;
    const systemInfo = wx.getSystemInfoSync();
    const statusBarHeight = Number(systemInfo.statusBarHeight) || 0;
    const memberKey = options && options.member ? String(options.member) : 'me';
    const selectedDate = options && options.date ? String(options.date) : this.getTodayDate();
    const memberId = options && options.memberId ? Number(options.memberId) : null;
    this.setData({
      statusBarHeight,
      navBarHeight: statusBarHeight + 44,
      safeAreaBottom: systemInfo.safeAreaInsets?.bottom || 0,
      memberId: Number.isFinite(memberId) && memberId > 0 ? memberId : null,
      memberKey,
      selectedDate,
      regionKey: (options && (options.region || options.key)) ? String(options.region || options.key) : 'heart'
    });

    // 加载器官数据
    await this.ensureMemberId();
    await this.loadOrganData(this.data.regionKey, { refreshAi: false });

    // 延迟启动动画
    setTimeout(() => {
      this.setData({ cardAnimated: true });
      this.drawRingChart();
      this.drawTrendChart();
      this.initParticles();
    }, 100);
  },

  async onShow() {
    if (this._skipOnShowOnce) {
      this._skipOnShowOnce = false;
      return;
    }
    await this.ensureMemberId();
    await this.loadOrganData(this.data.regionKey, { refreshAi: false });
  },

  onUnload() {
    if (this.data.animationFrame) {
      cancelAnimationFrame(this.data.animationFrame);
    }
  },

  // Methods
  goBack() {
    wx.navigateBack();
  },

  async onOrganTap(e) {
    const key = e && e.currentTarget && e.currentTarget.dataset ? String(e.currentTarget.dataset.key || '') : '';
    if (!key) return;
    if (key === this.data.regionKey) return;
    this.setData({ regionKey: key, expandedMetric: null, cardAnimated: false });
    await this.ensureMemberId();
    await this.loadOrganData(key, { refreshAi: false });
    setTimeout(() => {
      this.setData({ cardAnimated: true });
      this.drawRingChart();
      this.drawTrendChart();
    }, 60);
  },

  getScoreClassByScore(score) {
    const s = Number(score) || 0;
    if (s >= 70) return 'score-good';
    if (s >= 45) return 'score-neutral';
    return 'score-bad';
  },

  getStatusTextByScore(score) {
    const s = Number(score) || 0;
    if (s >= 70) return '状态优秀';
    if (s >= 45) return '需要关注';
    return '亟需改善';
  },

  getOrganEmojiByKey(key) {
    const k = String(key || '');
    const map = {
      brain: '🧠',
      heart: '🫀',
      lungs: '🫁',
      liver: '🧬',
      stomach: '🍲',
      intestine: '🥬',
      pancreas: '🍯',
      kidney: '💧',
      eyes: '👁️'
    };
    return map[k] || '❤️';
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

    const memberKey = String(this.data.memberKey || 'me').trim();
    if (!memberKey) return null;

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

  readDietContext(memberKey, date) {
    const mk = String(memberKey || 'me');
    const dt = String(date || this.getTodayDate());
    let store = {};
    try { store = wx.getStorageSync('nutrition_organ_impact_by_member') || {}; } catch (e) { store = {}; }
    const byMember = store && typeof store === 'object' ? store : {};
    const member = byMember[mk] && typeof byMember[mk] === 'object' ? byMember[mk] : null;
    if (!member) return null;
    const payload = member[dt] && typeof member[dt] === 'object' ? member[dt] : null;
    if (!payload) return null;
    const impactByKey = payload.impactByKey && typeof payload.impactByKey === 'object' ? payload.impactByKey : {};
    const profile = payload.profile && typeof payload.profile === 'object' ? payload.profile : {};
    return { impactByKey, profile };
  },

  async trySyncDietContextFromServer(options) {
    const memberKey = String(this.data.memberKey || 'me');
    const date = String(this.data.selectedDate || this.getTodayDate());
    let memberId = options && options.memberId ? Number(options.memberId) : null;
    if (!Number.isFinite(memberId) || memberId <= 0) memberId = null;

    if (!memberId) {
      try {
        const body = await get('/nutrition/members', {}, { loading: false, showError: false });
        const list = body && Array.isArray(body.data) ? body.data : [];
        const m = list.find(it => String((it && it.memberKey) || '').trim() === memberKey);
        const id = m && m.id ? Number(m.id) : null;
        if (Number.isFinite(id) && id > 0) memberId = id;
      } catch (e) {}
    }

    if (!memberId) return;
    try {
      const body = await get('/nutrition/organ-impact', { memberId, date }, { loading: false, showError: false });
      const payload = body && body.data && typeof body.data === 'object' ? body.data : null;
      const profile = payload && payload.profile && typeof payload.profile === 'object' ? payload.profile : null;
      const impactByKey = payload && payload.impactByKey && typeof payload.impactByKey === 'object' ? payload.impactByKey : null;
      const impacts = payload && Array.isArray(payload.impacts) ? payload.impacts : [];
      if (!profile || !impactByKey) return;

      let store = {};
      try { store = wx.getStorageSync('nutrition_organ_impact_by_member') || {}; } catch (e2) { store = {}; }
      const byMember = store && typeof store === 'object' ? store : {};
      const nextMember = (byMember[memberKey] && typeof byMember[memberKey] === 'object') ? byMember[memberKey] : {};
      nextMember[date] = { profile, impactByKey, impacts };
      const next = { ...byMember, [memberKey]: nextMember };
      try { wx.setStorageSync('nutrition_organ_impact_by_member', next); } catch (e3) {}
      if (this.data && this.data.regionKey) {
        this.loadOrganData(this.data.regionKey);
      }
    } catch (e) {}
  },

  async createHealthRecord(payload) {
    const p = payload && typeof payload === 'object' ? payload : {};
    const type = String(p.type || '').trim();
    if (!type) return null;
    const extObj = p.ext && typeof p.ext === 'object' ? p.ext : (p.ext ? { ext: p.ext } : {});
    const ext = Object.keys(extObj).length ? JSON.stringify(extObj) : '';
    const body = {
      type,
      value1: (p.value1 === undefined || p.value1 === null) ? undefined : Number(p.value1),
      value2: (p.value2 === undefined || p.value2 === null) ? undefined : Number(p.value2),
      unit1: p.unit1 ? String(p.unit1) : undefined,
      unit2: p.unit2 ? String(p.unit2) : undefined,
      note: p.note ? String(p.note) : undefined,
      ext: ext || undefined
    };
    try {
      const res = await post('/api/health/records', body, { loading: false, showError: false });
      return res && res.data ? res.data : null;
    } catch (e) {
      return null;
    }
  },

  recordOrganView(organKey) {
    const ok = String(organKey || this.data.regionKey || '').trim();
    if (!ok) return;
    this.createHealthRecord({
      type: 'organ_view',
      note: ok,
      ext: {
        memberId: this.data.memberId,
        date: this.data.selectedDate,
        organKey: ok
      }
    });
  },

  async loadOrganData(key, { refreshAi } = {}) {
    const rawKey = String(key || this.data.regionKey || 'heart').trim() || 'heart';
    const organKey = rawKey === 'gut' ? 'intestine' : rawKey;
    const date = String(this.data.selectedDate || this.getTodayDate());
    const memberId = await this.ensureMemberId();
    if (!memberId) return;

    try {
      const body = await get('/nutrition/organ-detail', { memberId: Number(memberId), organKey, date, refreshAi: !!refreshAi }, { loading: false, showError: false });
      const payload = body && body.data && typeof body.data === 'object' ? body.data : null;
      const organ = payload && payload.organ && typeof payload.organ === 'object' ? payload.organ : null;
      if (!organ) return;

      const ok = String(organ.key || organKey || 'heart');
      const score = Number(organ.score) || 0;
      const history = (payload && Array.isArray(payload.history)) ? payload.history.map(n => Number(n)).filter(n => Number.isFinite(n)) : [];
      const rawTabs = (payload && Array.isArray(payload.organTabs)) ? payload.organTabs : [];
      const organTabs = rawTabs.map((t) => {
        const tab = t && typeof t === 'object' ? t : {};
        const tabKey = String(tab.key || '').trim();
        const s = Number(tab.score) || 0;
        return {
          key: tabKey,
          name: String(tab.name || ''),
          emoji: String(tab.emoji || this.getOrganEmojiByKey(tabKey)),
          score: s,
          scoreClass: this.getScoreClassByScore(s)
        };
      }).filter(x => x && x.key);

      const scoreClass = this.getScoreClassByScore(score);
      const statusText = this.getStatusTextByScore(score);
      const trendMin = history.length ? Math.min(...history) : '--';
      const trendMax = history.length ? Math.max(...history) : '--';
      const trendAvg = history.length ? Math.round(history.reduce((a, b) => a + b, 0) / history.length) : '--';
      const emoji = String(organ.emoji || this.getOrganEmojiByKey(ok));

      this.setData({
        regionKey: ok,
        organ: { ...organ, key: ok, emoji },
        organEmoji: emoji,
        organTabs,
        history,
        scoreClass,
        statusText,
        trendMin,
        trendMax,
        trendAvg,
        dietImpactText: String(payload && payload.dietImpactText ? payload.dietImpactText : '')
      });

      this.recordOrganView(ok);
      if (this.data.cardAnimated) {
        this.drawRingChart();
        this.drawTrendChart();
      }
    } catch (e) {}
  },

  drawRingChart() {
    const ctx = wx.createCanvasContext('ringCanvas', this);
    const score = this.data.organ.score;
    const radius = 52;
    const centerX = 60;
    const centerY = 60;
    const circumference = 2 * Math.PI * radius;
    const progress = (score / 100) * circumference;

    // Draw background ring
    ctx.beginPath();
    ctx.arc(centerX, centerY, radius, 0, 2 * Math.PI);
    ctx.setStrokeStyle('#f0fdf4');
    ctx.setLineWidth(12);
    ctx.stroke();

    // Draw progress ring
    ctx.beginPath();
    ctx.arc(centerX, centerY, radius, -Math.PI / 2, -Math.PI / 2 + (score / 100) * 2 * Math.PI);
    ctx.setStrokeStyle(this.getScoreClass() === 'score-good' ? '#48bb78' : this.getScoreClass() === 'score-neutral' ? '#ed8936' : '#f56565');
    ctx.setLineWidth(12);
    ctx.setLineCap('round');
    ctx.stroke();

    ctx.draw();
  },

  drawTrendChart() {
    const query = wx.createSelectorQuery();
    query.select('#trendChart').boundingClientRect(res => {
      if (!res) return;
      const rect = res;
      const ctx = wx.createCanvasContext('trendChart', this);
      const w = rect.width;
      const h = 200;
      const padding = 20;

      ctx.clearRect(0, 0, w, h);

      const data = this.data.history.slice();
      const n = data.length;
      const min = Math.min(...data);
      const max = Math.max(...data);
      const range = Math.max(1, max - min);

      // 绘制网格
      ctx.setStrokeStyle('#f0fdf4');
      ctx.setLineWidth(1);
      for (let i = 0; i <= 4; i++) {
        const y = padding + (h - padding * 2) * (i / 4);
        ctx.beginPath();
        ctx.moveTo(padding, y);
        ctx.lineTo(w - padding, y);
        ctx.stroke();
      }

      // 绘制渐变区域
      const gradient = ctx.createLinearGradient(0, 0, 0, h);
      gradient.addColorStop(0, 'rgba(72, 187, 120, 0.3)');
      gradient.addColorStop(1, 'rgba(72, 187, 120, 0.05)');

      ctx.beginPath();
      ctx.moveTo(padding, h - padding);
      for (let i = 0; i < n; i++) {
        const x = padding + (w - padding * 2) * (i / (n - 1));
        const y = padding + ((max - data[i]) / range) * (h - padding * 2);
        ctx.lineTo(x, y);
      }
      ctx.lineTo(w - padding, h - padding);
      ctx.closePath();
      ctx.setFillStyle(gradient);
      ctx.fill();

      // 绘制曲线
      ctx.beginPath();
      for (let i = 0; i < n; i++) {
        const x = padding + (w - padding * 2) * (i / (n - 1));
        const y = padding + ((max - data[i]) / range) * (h - padding * 2);
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      }
      ctx.setStrokeStyle('#48bb78');
      ctx.setLineWidth(3);
      ctx.setLineCap('round');
      ctx.setLineJoin('round');
      ctx.stroke();

      // 绘制数据点
      for (let i = 0; i < n; i++) {
        const x = padding + (w - padding * 2) * (i / (n - 1));
        const y = padding + ((max - data[i]) / range) * (h - padding * 2);
        ctx.beginPath();
        ctx.arc(x, y, 5, 0, Math.PI * 2);
        ctx.setFillStyle('#ffffff');
        ctx.fill();
        ctx.beginPath();
        ctx.arc(x, y, 4, 0, Math.PI * 2);
        ctx.setFillStyle('#48bb78');
        ctx.fill();
      }

      ctx.draw();
    }).exec();
  },

  initParticles() {
    const query = wx.createSelectorQuery();
    query.select('#particleCanvas').boundingClientRect(res => {
      if (!res) return;
      const rect = res;
      const ctx = wx.createCanvasContext('particleCanvas', this);
      const particles = [];

      // 创建粒子
      for (let i = 0; i < 15; i++) {
        particles.push({
          x: Math.random() * rect.width,
          y: Math.random() * rect.height,
          vx: (Math.random() - 0.5) * 0.5,
          vy: (Math.random() - 0.5) * 0.5,
          radius: Math.random() * 3 + 1,
          opacity: Math.random() * 0.3 + 0.1
        });
      }

      this.setData({ particleCtx: ctx, particles });
      this.animateParticles(rect.width, rect.height);
    }).exec();
  },

  animateParticles(w, h) {
    const ctx = this.data.particleCtx;
    if (!ctx) return;

    ctx.clearRect(0, 0, w, h);

    this.data.particles.forEach(p => {
      p.x += p.vx;
      p.y += p.vy;

      if (p.x < 0 || p.x > w) p.vx *= -1;
      if (p.y < 0 || p.y > h) p.vy *= -1;

      ctx.beginPath();
      ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
      ctx.setFillStyle(`rgba(72, 187, 120, ${p.opacity})`);
      ctx.fill();
    });

    ctx.draw(true);
    this.data.animationFrame = requestAnimationFrame(() => this.animateParticles(w, h));
  },

  toggleMetric(e) {
    const idx = e.currentTarget.dataset.idx;
    this.setData({
      expandedMetric: this.data.expandedMetric === idx ? null : idx
    });
  },

  refreshMetrics() {
    this.showSuccessMessage('正在刷新数据...');
    this.ensureMemberId()
      .then(() => this.loadOrganData(this.data.regionKey, { refreshAi: false }))
      .then(() => {
        this.showSuccessMessage('数据已更新');
      })
      .catch(() => {});
  },

  applyAdvice(e) {
    const metric = e.currentTarget.dataset.metric;
    this.showSuccessMessage(`已采纳建议：${metric.name}`);
    this.createHealthRecord({
      type: 'organ_adopt_metric',
      note: String(metric && metric.name ? metric.name : ''),
      ext: {
        memberId: this.data.memberId,
        date: this.data.selectedDate,
        organKey: this.data.regionKey,
        metric: metric && typeof metric === 'object' ? metric : null
      }
    });
  },

  compareStandard(e) {
    const metric = e.currentTarget.dataset.metric;
    wx.showModal({
      title: '标准对比',
      content: `${metric.name}\n当前值：${metric.value}${metric.unit || ''}\n${metric.detail}`,
      showCancel: false,
      confirmText: '知道了'
    });
  },

  applySuggestion(e) {
    const suggestion = e.currentTarget.dataset.suggestion;
    this.showSuccessMessage(`已应用：${suggestion.title}`);
    this.createHealthRecord({
      type: 'organ_adopt_suggestion',
      note: String(suggestion && suggestion.title ? suggestion.title : ''),
      ext: {
        memberId: this.data.memberId,
        date: this.data.selectedDate,
        organKey: this.data.regionKey,
        suggestion: suggestion && typeof suggestion === 'object' ? suggestion : null
      }
    });
  },

  showFoodDetail(e) {
    const food = e.currentTarget.dataset.food;
    this.createHealthRecord({
      type: 'organ_view_food',
      note: String(food && food.name ? food.name : ''),
      ext: {
        memberId: this.data.memberId,
        date: this.data.selectedDate,
        organKey: this.data.regionKey,
        food: food && typeof food === 'object' ? food : null
      }
    });
    wx.showModal({
      title: food.name,
      content: `${food.note}\n\n这是一种对${this.data.organ.name}非常有益的健康食物，建议适量食用。`,
      showCancel: true,
      confirmText: '添加到食谱',
      cancelText: '知道了',
      success: res => {
        if (res.confirm) {
          this.showSuccessMessage('已添加到食谱');
          this.createHealthRecord({
            type: 'organ_adopt_food',
            note: String(food && food.name ? food.name : ''),
            ext: {
              memberId: this.data.memberId,
              date: this.data.selectedDate,
              organKey: this.data.regionKey,
              food: food && typeof food === 'object' ? food : null
            }
          });
        }
      }
    });
  },

  generateAIPlan() {
    this.showSuccessMessage('AI正在生成个性化计划...');
    this.ensureMemberId()
      .then(() => this.loadOrganData(this.data.regionKey, { refreshAi: true }))
      .then(() => {
        this.showSuccessMessage('新计划已生成');
      })
      .catch(() => {});
  },

  saveRecord() {
    this.createHealthRecord({
      type: 'organ_status',
      value1: Number(this.data.organ && this.data.organ.score),
      unit1: 'score',
      note: String(this.data.organ && this.data.organ.name ? this.data.organ.name : ''),
      ext: {
        memberId: this.data.memberId,
        date: this.data.selectedDate,
        organKey: this.data.regionKey
      }
    })
      .then(() => {
        this.showSuccessMessage('状态已记录到健康档案');
      })
      .catch(() => {});
  },

  exportData() {
    this.showSuccessMessage('数据导出中...');
    setTimeout(() => {
      this.showSuccessMessage('数据已导出');
    }, 1000);
  },

  shareOrgan() {
    wx.showShareMenu({
      withShareTicket: true,
      success: () => {
        this.showSuccessMessage('分享成功');
      }
    });
  },

  showSuccessMessage(message) {
    this.setData({ successMessage: message, showSuccess: true });
    setTimeout(() => {
      this.setData({ showSuccess: false });
    }, 2000);
  },

  onScroll(e) {
    // 可以添加滚动相关的交互效果
  },

  getMetricEmoji(name) {
    const emojiMap = {
      '控盐表现': '🧂',
      'Omega-3 结构': '�',
      '膳食纤维': '�',
      '精制负担': '🍬',
      '碳水稳定性': '🍚',
      '蛋白负担': '🥩',
      '加餐频率': '🕒',
      '刺激负担': '🌶️',
      '抗氧化食物': '🥦',
      '能量平衡': '⚡',
      '脂肪占比': '🫒'
    };
    return emojiMap[name] || '📊';
  },

  getTrendClass(status) {
    return 'trend-' + (status || 'neutral');
  },

  getSuggestionEmoji(type) {
    const emojiMap = {
      'food': '🥗',
      'exercise': '💪',
      'lifestyle': '🧘'
    };
    return emojiMap[type] || '💡';
  }
});

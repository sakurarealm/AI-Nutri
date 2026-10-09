const request = require('../../../utils/request.js');
const us = require('../../../utils/userState.js');

function clampInt(n, min, max) {
  const v = Math.floor(Number(n));
  if (Number.isNaN(v)) return min;
  return Math.min(max, Math.max(min, v));
}

function nowSubtitle(d = new Date()) {
  const h = d.getHours();
  if (h < 6) return '睡一觉也算努力，慢慢来就好';
  if (h < 11) return '把今天拆小一点，会更容易坚持';
  if (h < 16) return '给身体一点能量，也给心一点松弛';
  if (h < 20) return '晚一点也没关系，重要的是继续';
  return '让今天轻轻收尾，明天会更顺';
}

function getTodayDate() {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function parseLevelNumber(label) {
  const s = String(label || '');
  const m = s.match(/(\d+)/);
  const n = m ? Number(m[1]) : 0;
  return Number.isFinite(n) && n >= 0 ? n : 0;
}

function computeXp(levelNumber, todayView, weekView) {
  const level = Math.max(0, Number(levelNumber) || 0);
  const base = level * 1000;
  const completionRate = clampInt(todayView && todayView.completionRate, 0, 100);
  const warmScore = clampInt(weekView && weekView.warmScore, 0, 100);
  const streakDays = Math.max(0, Math.floor(Number(weekView && weekView.streakDays) || 0));
  const progress = clampInt(Math.round(completionRate * 6 + warmScore * 5 + streakDays * 30), 0, 999);
  const current = base + progress;
  const next = (level + 1) * 1000;
  const left = Math.max(0, next - current);
  const percent = clampInt(Math.round((progress / 1000) * 100), 0, 100);
  return { current, next, left, percent };
}

function achievementFallback() {
  const pool = [
    { icon: '🌿', text: '蔬菜优先' },
    { icon: '🥛', text: '蛋白补足' },
    { icon: '💧', text: '补水在线' },
    { icon: '👟', text: '走路加分' },
    { icon: '🛌', text: '早睡一点点' },
    { icon: '🥗', text: '轻食也饱腹' },
    { icon: '🧘', text: '放松 2 分钟' },
    { icon: '❤️', text: '温柔对自己' }
  ];
  return pool.map((it, i) => ({ ...it, id: `local-${i}` }));
}

function buildTasks({ nutrition, todayView }) {
  const calorieConsumed = Math.max(0, Math.round(Number(nutrition && nutrition.calorieConsumed) || 0));
  const completionRate = clampInt(todayView && todayView.completionRate, 0, 100);
  return [
    {
      key: 'record',
      icon: '🥗',
      title: '记一笔饮食',
      desc: '越真实，分析越准',
      done: calorieConsumed > 0
    },
    {
      key: 'checkin',
      icon: '✅',
      title: '打卡一下',
      desc: '给今天一个小标记',
      done: completionRate >= 60
    },
    {
      key: 'review',
      icon: '🤖',
      title: '让 AI 复盘',
      desc: '基于记录给你 3 条建议',
      done: false
    }
  ];
}

function buildNutritionHint(nutrition) {
  const consumed = Math.max(0, Math.round(Number(nutrition && nutrition.calorieConsumed) || 0));
  const target = Math.max(1, Math.round(Number(nutrition && nutrition.calorieTarget) || 2000));
  const pct = clampInt(Math.round((consumed / target) * 100), 0, 200);
  if (consumed <= 0) return '去营养册记一下，今晚就能看到更贴合的建议';
  if (pct < 60) return '能量偏低，别饿着自己：加一份蛋白或慢碳水更稳';
  if (pct <= 110) return '整体节奏不错：保持三餐稳定，比追数字更重要';
  return '今天吃得有点多：晚一点散步 10 分钟，别用极端补救';
}

Page({
  data: {
    user: { avatar: '/images/default_avatar.svg', nickname: 'AI-nutri用户' },
    subtitle: '',
    health: { healthLevel: 'Lv.0' },
    xp: { current: 0, next: 1000, left: 1000, percent: 0 },
    tasks: [],
    spaceToday: {
      completionRate: 0,
      completionHint: '',
      warmScore: 0,
      timeline: [],
      achievements: []
    },
    spaceWeek: { streakDays: 0, warmScore: 0 },
    nutrition: {
      calorieConsumed: 0,
      calorieTarget: 2000,
      macroPercent: { carb: 0, protein: 0, fat: 0 }
    },
    nutritionHint: ''
  },

  onLoad() {
    this.loadFromCache();
    this.refreshAll({ loading: true });
  },

  onShow() {
    this.loadFromCache();
    this.refreshAll({ loading: false });
  },

  onPullDownRefresh() {
    this.refreshAll({ loading: false })
      .finally(() => wx.stopPullDownRefresh());
  },

  loadFromCache() {
    const subtitle = nowSubtitle(new Date());
    const next = { subtitle };

    try {
      const state = us.load()
      const u = (state && state.user) || wx.getStorageSync('userProfile') || wx.getStorageSync('userInfo')
      if (u) {
        const avatar = u.avatar ?? u.avatarUrl ?? u.avatar_url ?? u.headImg ?? this.data.user.avatar;
        const nickname = u.nickname ?? u.username ?? u.mobile ?? this.data.user.nickname;
        next.user = { avatar: String(avatar || this.data.user.avatar), nickname: String(nickname || this.data.user.nickname) };
      }
    } catch (e) {}

    this.setData(next);
  },

  async refreshAll({ loading } = {}) {
    const safeLoading = loading === true;
    const [userMe, healthOverview, todayView, weekView, nutrition] = await Promise.all([
      this.fetchUserMe({ loading: false }),
      this.fetchHealthOverview({ loading: false }),
      this.fetchSpaceView(0, { loading: safeLoading }),
      this.fetchSpaceView(1, { loading: false }),
      this.fetchNutritionSummary({ loading: false })
    ]);

    const healthLevel = (healthOverview && healthOverview.healthLevel) || this.data.health.healthLevel || 'Lv.0';
    const levelNumber = parseLevelNumber(healthLevel);
    const xp = computeXp(levelNumber, todayView || this.data.spaceToday, weekView || this.data.spaceWeek);

    const finalToday = todayView || this.data.spaceToday;
    const finalNutrition = nutrition || this.data.nutrition;
    const tasks = buildTasks({ nutrition: finalNutrition, todayView: finalToday });
    const nutritionHint = buildNutritionHint(finalNutrition);

    this.setData({
      user: (userMe && userMe.user) ? userMe.user : this.data.user,
      health: { healthLevel: String(healthLevel) },
      xp,
      tasks,
      nutrition: finalNutrition,
      nutritionHint
    });
  },

  async fetchUserMe({ loading } = {}) {
    try {
      const resp = await request.get('/user/me', {}, { loading: !!loading, showError: false });
      const data = (resp && (resp.data || resp)) || null;
      if (!data) return null;
      const avatar = data.avatar ?? data.avatarUrl ?? data.avatar_url ?? data.headImg ?? this.data.user.avatar;
      const nickname = data.nickname ?? data.username ?? data.mobile ?? this.data.user.nickname;
      const mapped = { avatar: String(avatar || this.data.user.avatar), nickname: String(nickname || this.data.user.nickname) };
      try { wx.setStorageSync(USER_CACHE_KEY, data); } catch (e) {}
      this.setData({ user: mapped });
      return { user: mapped };
    } catch (e) {
      return null;
    }
  },

  async fetchHealthOverview({ loading } = {}) {
    try {
      const resp = await request.get('/health/overview', {}, { loading: !!loading, showError: false });
      const d = (resp && (resp.data || resp)) || null;
      if (!d) return null;
      const levelLabel = d.levelName ?? d.healthLevel ?? (d.level != null ? `Lv.${d.level}` : 'Lv.1');
      const result = { healthLevel: String(levelLabel) };
      this.setData({ health: result });
      return result;
    } catch (e) {
      return null;
    }
  },

  async fetchSpaceView(viewIndex, { loading } = {}) {
    const idx = clampInt(viewIndex, 0, 2);
    try {
      const resp = await request.get('/health/space', { viewIndex: idx }, { loading: !!loading, showError: false });
      const data = (resp && (resp.data || resp)) || {};

      const mapped = {
        completionRate: clampInt(data.completionRate, 0, 100),
        completionHint: String(data.completionHint || ''),
        streakDays: Math.max(0, Math.floor(Number(data.streakDays) || 0)),
        warmScore: clampInt(data.warmScore, 0, 100),
        timeline: Array.isArray(data.timeline) ? data.timeline : [],
        achievements: Array.isArray(data.achievements) ? data.achievements : []
      };

      if (!mapped.achievements.length && idx === 0) {
        mapped.achievements = achievementFallback();
      }

      if (idx === 0) this.setData({ spaceToday: mapped });
      if (idx === 1) this.setData({ spaceWeek: mapped });
      return mapped;
    } catch (e) {
      if (idx === 0) {
        const fallback = { ...this.data.spaceToday, achievements: achievementFallback() };
        this.setData({ spaceToday: fallback });
        return fallback;
      }
      return null;
    }
  },

  async fetchNutritionSummary({ loading } = {}) {
    const date = getTodayDate();
    const selectedKey = (() => {
      try { return String(wx.getStorageSync('nutrition_selected_member') || 'me'); } catch (e) { return 'me'; }
    })();

    const fromServer = await this.tryLoadNutritionFromServer(selectedKey, date, { loading: !!loading });
    if (fromServer) return fromServer;
    const fromCache = this.tryLoadNutritionFromCache(selectedKey, date);
    if (fromCache) return fromCache;
    return {
      calorieConsumed: 0,
      calorieTarget: 2000,
      macroPercent: { carb: 0, protein: 0, fat: 0 }
    };
  },

  async tryLoadNutritionFromServer(selectedKey, date, { loading } = {}) {
    try {
      const body = await request.get('/nutrition/members', {}, { loading: !!loading, showError: false });
      const list = body && Array.isArray(body.data) ? body.data : [];
      if (!list.length) return null;

      let picked = list.find((m) => String(m && m.memberKey ? m.memberKey : '') === String(selectedKey || ''));
      if (!picked) picked = list.find((m) => String(m && m.memberKey ? m.memberKey : '') === 'me') || list[0];
      const memberId = picked && picked.id != null ? Number(picked.id) : null;
      const memberKey = String(picked && picked.memberKey ? picked.memberKey : selectedKey || 'me');
      if (!memberId) return null;

      if (memberKey && memberKey !== selectedKey) {
        try { wx.setStorageSync('nutrition_selected_member', memberKey); } catch (e) {}
      }

      const dayBody = await request.get('/nutrition/day', { memberId, date: String(date || '') }, { loading: false, showError: false });
      const day = dayBody && dayBody.data && typeof dayBody.data === 'object' ? dayBody.data : null;
      if (!day) return null;

      const macroPercent = day.macroPercent && typeof day.macroPercent === 'object' ? day.macroPercent : {};
      const nutrition = {
        calorieConsumed: Math.max(0, Math.round(Number(day.calorieConsumed) || 0)),
        calorieTarget: Math.max(1, Math.round(Number(day.calorieTarget) || 2000)),
        macroPercent: {
          carb: clampInt(macroPercent.carb, 0, 100),
          protein: clampInt(macroPercent.protein, 0, 100),
          fat: clampInt(macroPercent.fat, 0, 100)
        }
      };

      this.setData({ nutrition });
      let byMember = {};
      try { byMember = wx.getStorageSync('nutrition_daily_macros_by_member') || {}; } catch (e) { byMember = {}; }
      byMember[String(memberKey || 'me')] = {
        date: String(day.date || date),
        totals: day.macroTotals || {},
        targets: day.macroTargets || {},
        percent: macroPercent || {},
        calorieConsumed: nutrition.calorieConsumed,
        calorieTarget: nutrition.calorieTarget
      };
      try { wx.setStorageSync('nutrition_daily_macros_by_member', byMember); } catch (e2) {}

      return nutrition;
    } catch (e) {
      return null;
    }
  },

  tryLoadNutritionFromCache(memberKey, date) {
    try {
      const byMember = wx.getStorageSync('nutrition_daily_macros_by_member') || {};
      const payload = byMember && byMember[String(memberKey || 'me')] ? byMember[String(memberKey || 'me')] : null;
      if (!payload) return null;
      if (payload.date && String(payload.date) !== String(date)) return null;
      const p = payload.percent && typeof payload.percent === 'object' ? payload.percent : {};
      const nutrition = {
        calorieConsumed: Math.max(0, Math.round(Number(payload.calorieConsumed) || 0)),
        calorieTarget: Math.max(1, Math.round(Number(payload.calorieTarget) || 2000)),
        macroPercent: {
          carb: clampInt(p.carb, 0, 100),
          protein: clampInt(p.protein, 0, 100),
          fat: clampInt(p.fat, 0, 100)
        }
      };
      this.setData({ nutrition });
      return nutrition;
    } catch (e) {
      return null;
    }
  },

  onTaskTap(e) {
    const key = String((e && e.currentTarget && e.currentTarget.dataset && e.currentTarget.dataset.key) || '');
    if (!key) return;
    if (key === 'record') {
      wx.navigateTo({ url: '/pages/nutritionbrochure/FoodRecord/FoodRecord' });
      return;
    }
    if (key === 'checkin') {
      wx.showToast({ title: '已为你记下今天的努力', icon: 'none' });
      const nextTasks = (Array.isArray(this.data.tasks) ? this.data.tasks : []).map((t) => t.key === 'checkin' ? { ...t, done: true } : t);
      this.setData({ tasks: nextTasks });
      return;
    }
    if (key === 'review') {
      wx.switchTab({ url: '/pages/aiAgent/aiAgent' });
    }
  },

  goNutrition() {
    wx.switchTab({ url: '/pages/nutritionbrochure/nutritionbrochure' });
  }
});


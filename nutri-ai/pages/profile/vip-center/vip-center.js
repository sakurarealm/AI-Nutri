const VIP_CACHE_KEY = 'vip_cache';
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

Page({
  data: {
    user: {},
    nickname: '游客',
    avatarInitial: '游',
    vipInfo: { level: 0, levelName: '普通会员', expireDate: null, planId: '' },
    greeting: '欢迎回来',
    subtitle: '你的健康管理空间，今天也要温柔地变更好',
    vipMeta: '完善档案后，AI 会更懂你',
    ctaTop: '开通会员',
    healthLinkText: '查看详情',
    healthTitle: '健康档案已就绪',
    healthSub: 'AI 与营养师正在为你生成更贴合的建议',
    healthLevelText: 'Lv.1',
    progressPercent: 28,
    progressText: '本周完成 2/7 个小目标',
    goals: [
      { key: 'streak', value: '0', label: '连续打卡' },
      { key: 'level', value: 'Lv.1', label: '健康等级' },
      { key: 'weekly', value: '0/7', label: '本周目标' }
    ],
    planHint: '越贴合你的状态，越容易坚持',
    planTitle: '今日补充建议',
    planSub: '从轻量开始，把行动变得更容易',
    planChip: 'AI 定制',
    planItems: [
      { key: 'water', title: '补水提醒', sub: '今天先把饮水分配到每 2 小时一次', tag: '轻量', color: '#3b82f6' },
      { key: 'protein', title: '蛋白质优先', sub: '每餐增加一份优质蛋白，饱腹更稳定', tag: '稳定', color: '#10b981' },
      { key: 'walk', title: '微运动', sub: '饭后 10 分钟散步，帮助控糖与消化', tag: '轻松', color: '#f59e0b' }
    ],
    planAction: '查看定制方案',
    planPayAction: '升级解锁更多',
    achievements: [
      { key: 'a1', title: '第一周', sub: '坚持记录 7 天', pill: '进行中', unlocked: true },
      { key: 'a2', title: '轻食达人', sub: '完成 5 次健康餐', pill: '待解锁', unlocked: false },
      { key: 'a3', title: '目标稳住', sub: '连续 3 天达成目标', pill: '待解锁', unlocked: false }
    ],
    recHint: '结合你的状态，轻量而不打扰',
    recommendations: [
      { key: 'supplement', title: '今日补充建议', sub: '根据你的饮食结构，给出更稳的选择', action: '查看', color: '#10b981' },
      { key: 'recipe', title: '适合你的食谱', sub: '更容易坚持的 3 道快手搭配', action: '看看', color: '#3b82f6' },
      { key: 'product', title: '专属商品推荐', sub: '匹配你的目标与习惯，少而精', action: '进入', color: '#f59e0b' }
    ],
    bottomTitle: '让健康更有掌控感',
    bottomSub: '开通后立即解锁更细分析与专属计划',
    ctaBottom: '立即开通',
    ctaBottomSub: '权益即时生效'
  },

  onLoad() {
    this.refresh();
  },

  onShow() {
    this.refresh();
  },

  refresh() {
    const state = us.load()
    const user = (state && state.user) || wx.getStorageSync('user_cache') || wx.getStorageSync('userProfile') || wx.getStorageSync('userInfo') || {};
    const nickname = String((user && (user.nickname || user.username)) || '游客').trim() || '游客';
    const avatarInitial = nickname.substring(0, 1);

    const vipRaw = (state && state.vip) || wx.getStorageSync(VIP_CACHE_KEY) || {};
    const vipInfo = this.normalizeVipInfo(vipRaw);

    const greeting = this.getGreeting();
    const subtitle = vipInfo.level > 0 ? '你的专属空间已点亮，今天也要温柔地变更好' : '在开通之前，也可以先看看你的专属方案';

    const healthOverview = wx.getStorageSync('health_overview_cache') || {};
    const goals = this.buildGoals(healthOverview);
    const progress = this.buildProgress(healthOverview);

    const ctaTop = vipInfo.level > 0 ? '管理会员' : '开通会员';
    const vipMeta = vipInfo.level > 0
      ? `有效期至 ${vipInfo.expireDate || '永久'}`
      : '升级后可获得更细的分析与定制方案';

    const bottomTitle = vipInfo.level > 0 ? '继续保持，让权益不断档' : '让健康更有掌控感';
    const bottomSub = vipInfo.level > 0 ? '续费后权益立即延续，不中断' : '开通后立即解锁更细分析与专属计划';
    const ctaBottom = vipInfo.level > 0 ? '立即续费' : '立即开通';
    const ctaBottomSub = vipInfo.level > 0 ? '权益持续在线' : '权益即时生效';

    const planPayAction = vipInfo.level > 0 ? '管理套餐' : '升级解锁更多';

    this.setData({
      user,
      nickname,
      avatarInitial,
      vipInfo,
      greeting,
      subtitle,
      vipMeta,
      ctaTop,
      goals,
      progressPercent: progress.percent,
      progressText: progress.text,
      healthLevelText: goals[1]?.value || 'Lv.1',
      bottomTitle,
      bottomSub,
      ctaBottom,
      ctaBottomSub,
      planPayAction
    });
  },

  normalizeVipInfo(v = {}) {
    const level = Number(v.level ?? v.vipLevel ?? 0) || 0;
    const planId = String(v.planId ?? v.plan ?? v.planType ?? '').trim();
    let levelName = String(v.levelName ?? '').trim();
    if (!levelName) {
      if (planId === 'basic') levelName = '基础版';
      else if (planId === 'pro') levelName = '专业版';
      else if (level >= 2) levelName = '专业版';
      else if (level === 1) levelName = '基础版';
      else levelName = '普通会员';
    }
    const expireDate = v.expireDate ?? v.expireTime ?? v.expire ?? null;
    const e = expireDate ? String(expireDate) : '';
    const normalizedExpire = e && e !== '永久' ? formatDate(e) || e : expireDate;
    return { level, planId, levelName, expireDate: normalizedExpire };
  },

  buildGoals(h = {}) {
    const streak = Number(h.streakDays ?? h.continuousDays ?? h.streak ?? 0) || 0;
    const levelLabel = String(h.levelName ?? h.healthLevel ?? (h.level != null ? `Lv.${h.level}` : 'Lv.1'));
    const done = Number(h.weeklyCompleted ?? h.weeklyDone ?? h.weeklyGoalsCompleted ?? h.weeklyGoals?.done ?? 0) || 0;
    const total = Number(h.weeklyTarget ?? h.weeklyGoals?.target ?? 7) || 7;
    return [
      { key: 'streak', value: String(streak), label: '连续打卡' },
      { key: 'level', value: levelLabel, label: '健康等级' },
      { key: 'weekly', value: `${done}/${total}`, label: '本周目标' }
    ];
  },

  buildProgress(h = {}) {
    const done = Number(h.weeklyCompleted ?? h.weeklyDone ?? h.weeklyGoalsCompleted ?? h.weeklyGoals?.done ?? 0) || 0;
    const total = Number(h.weeklyTarget ?? h.weeklyGoals?.target ?? 7) || 7;
    const percent = total <= 0 ? 0 : Math.max(0, Math.min(100, Math.round((done / total) * 100)));
    return { percent, text: `本周完成 ${done}/${total} 个小目标` };
  },

  getGreeting() {
    const h = new Date().getHours();
    if (h < 6) return '夜深了';
    if (h < 11) return '早上好';
    if (h < 14) return '中午好';
    if (h < 18) return '下午好';
    return '晚上好';
  },

  goToPay() {
    wx.navigateTo({ url: '/pages/profile/vip/vip' });
  },

  openPlan() {
    wx.showToast({ title: '方案详情开发中', icon: 'none' });
  },

  openAchievements() {
    wx.showToast({ title: '成就页开发中', icon: 'none' });
  },

  openRecommendation() {
    wx.showToast({ title: '内容开发中', icon: 'none' });
  },

  openHealthFile() {
    wx.showToast({ title: '档案详情开发中', icon: 'none' });
  }
});


function formatDateLabel(d = new Date()) {
  const m = String(d.getMonth() + 1);
  const day = String(d.getDate());
  const weekday = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()];
  return `${m}月${day}日 ${weekday}`;
}

function nowPresenceLine(d = new Date()) {
  const h = d.getHours();
  if (h < 6) return '把每一次睡眠当成修复';
  if (h < 12) return '把早餐当成今天的开关';
  if (h < 18) return '把每一步都算进进步里';
  return '把晚餐吃得更轻一点';
}

function clampInt(n, min, max) {
  const v = Math.floor(Number(n));
  if (Number.isNaN(v)) return min;
  return Math.min(max, Math.max(min, v));
}

const request = require('../../utils/request.js');

const mottoPool = [
  '你和很多人，正悄悄变好',
  '每一次坚持，都在被看见',
  '今天的努力，明天会回馈你',
  '我们都在同一条温柔的路上',
  '小步前进，也在改变体质',
  '呼吸之间，就有力量积累'
];

const achievementPool = [
  { icon: '🏅', text: '连续7天轻食' },
  { icon: '🌿', text: '蛋白质达标' },
  { icon: '💧', text: '饮水满格' },
  { icon: '👟', text: '步数超8000' },
  { icon: '🛌', text: '早睡成就' },
  { icon: '🥗', text: '蔬菜优先' },
  { icon: '⚡', text: '能量均衡' },
  { icon: '❤️', text: '心率稳定' },
  { icon: '🌱', text: '纤维充足' },
  { icon: '🧘', text: '深呼吸打卡' }
];

function animateValue(obj, start, end, duration) {
  const raf = typeof wx.requestAnimationFrame === 'function'
    ? wx.requestAnimationFrame.bind(wx)
    : (cb) => setTimeout(() => cb(Date.now()), 16);
  let startTime = null;
  const step = (timestamp) => {
    if (!startTime) startTime = timestamp;
    const progress = Math.min((timestamp - startTime) / duration, 1);
    const value = Math.floor(progress * (end - start) + start);
    obj.setData({ completionRate: value });
    if (progress < 1) raf(step);
  };
  raf(step);
}

function fallbackViewModel(viewIndex = 0) {
  const idx = clampInt(viewIndex, 0, 2);
  const completionRate = [78, 86, 72][idx];
  const warmScore = [83, 88, 79][idx];
  const streakDays = [5, 12, 7][idx];
  const timeline = [
    [
      {
        id: 't-01',
        time: '08:12',
        badge: '系统',
        title: '早餐能量更均衡了',
        desc: '和昨天相比，蛋白质占比上升，饱腹感会更稳定。',
        kv: [{ k: '蛋白质', v: '+12%' }, { k: '膳食纤维', v: '+6g' }]
      },
      {
        id: 't-02',
        time: '12:40',
        badge: 'AI',
        title: '午后困倦风险降低',
        desc: '你把主食换成更慢的碳水，血糖波动更温和。',
        kv: [{ k: '波动', v: '更平稳' }, { k: '建议', v: '餐后走 8 分钟' }]
      },
      {
        id: 't-03',
        time: '19:18',
        badge: '系统',
        title: '今日饮水接近达标',
        desc: '还差一点点就满格，睡前留一小杯就好。',
        kv: [{ k: '完成', v: `${completionRate}%` }]
      }
    ],
    [
      {
        id: 'w-01',
        time: '本周',
        badge: 'AI',
        title: '体重不是唯一指标',
        desc: '这周你更“稳”：晚餐更轻，睡眠更规律，体感会先于数字改变。',
        kv: [{ k: '晚餐', v: '更轻' }, { k: '睡眠', v: '更稳' }]
      },
      {
        id: 'w-02',
        time: '第4天',
        badge: '系统',
        title: '连续达标记录刷新',
        desc: '你把目标拆成了小块，反而更容易坚持。',
        kv: [{ k: '连续', v: `${streakDays} 天` }]
      },
      {
        id: 'w-03',
        time: '第6天',
        badge: 'AI',
        title: '适应期已过',
        desc: '如果想继续提升，把“蛋白质优先”变成默认策略。',
        kv: [{ k: '策略', v: '先蛋白' }, { k: '提示', v: '每餐一掌心' }]
      }
    ],
    [
      {
        id: 'g-01',
        time: '人群',
        badge: '匿名',
        title: '同阶段的大家在做什么',
        desc: '更少的“自律”，更多的“顺手”：提前准备、减少选择成本。',
        kv: [{ k: '共同行为', v: '提前备餐' }, { k: '微习惯', v: '餐后散步' }]
      },
      {
        id: 'g-02',
        time: '趋势',
        badge: 'AI',
        title: '轻食不等于吃得少',
        desc: '大家更关注营养密度：蛋白 + 蔬菜 + 慢碳水。',
        kv: [{ k: '组合', v: '蛋白+蔬菜+慢碳' }]
      },
      {
        id: 'g-03',
        time: '提醒',
        badge: '系统',
        title: '久坐时间偏长',
        desc: '很多人用“2 分钟拉伸”替代一次性补救，更容易形成习惯。',
        kv: [{ k: '建议', v: '每小时 2 分钟' }]
      }
    ]
  ][idx];
  return {
    viewIndex: idx,
    completionRate,
    completionHint: [
      '不是满分也很好，继续保持节奏。',
      '这一周挺稳，身体会记得这些努力。',
      '你和同频的人群正在一起变好。'
    ][idx],
    streakDays,
    warmScore,
    pulse: {
      reached: [1248, 2631, 892][idx],
      stepsAvg: [6482, 7349, 6021][idx],
      lightMealRate: [41, 48, 36][idx]
    },
    pulseSubtitle: [
      '来自同城同阶段用户的匿名汇总',
      '更看重趋势，不追逐噪音',
      '你并不孤单，大家都在做同一件事'
    ][idx],
    barLeft: ['今日节奏', '本周趋势', '人群同频'][idx],
    barRight: [`已完成 ${completionRate}%`, `稳定度 ${warmScore}%`, `同步度 ${clampInt(66 + idx * 7, 0, 100)}%`][idx],
    timeline
  };
}

Page({
  data: {
    statusBarHeightPx: 0,
    navPaddingTopPx: 0,
    navHeightPx: 0,
    navRightPaddingPx: 48,
    contentPaddingTopPx: 0,
    dateLabel: '',
    presenceLine: '',
    viewIndex: 0,
    completionRate: 0,
    completionHint: '',
    streakDays: 0,
    warmScore: 0,
    barLeft: '',
    barRight: '',
    pulseSubtitle: '',
    pulse: { reached: 0, stepsAvg: 0, lightMealRate: 0 },
    timeline: [],
    currentMotto: '',
    mottoIndex: 0,
    showMotto: true,
    achievements: []
  },

  onLoad() {
    const sys = wx.getSystemInfoSync ? wx.getSystemInfoSync() : {};
    const statusBarHeightPx = Number(sys.statusBarHeight) || 0;
    const windowWidthPx = Number(sys.windowWidth) || 375;
    const menu = wx.getMenuButtonBoundingClientRect ? wx.getMenuButtonBoundingClientRect() : null;
    const extraTopPx = 30;

    let navBarHeightPx = 44;
    let navRightPaddingPx = 48;
    if (menu && menu.height) {
      const gap = Math.max(0, Number(menu.top) - statusBarHeightPx);
      navBarHeightPx = Math.round(gap * 2 + Number(menu.height));
      navRightPaddingPx = Math.max(48, Math.round(windowWidthPx - Number(menu.left) + 12));
    }
    const navPaddingTopPx = Math.max(0, Math.round(statusBarHeightPx + extraTopPx));
    const navHeightPx = Math.max(0, Math.round(statusBarHeightPx + navBarHeightPx + extraTopPx));
    const contentPaddingTopPx = navHeightPx + 24;

    const d = new Date();
    const base = {
      dateLabel: formatDateLabel(d),
      presenceLine: nowPresenceLine(d)
    };
    this.setData({ statusBarHeightPx, navPaddingTopPx, navHeightPx, navRightPaddingPx, contentPaddingTopPx, ...base });
    this.startMottoCycle();
    this.loadSpaceView(this.data.viewIndex, { animate: true, loading: false });
  },

  onShow() {
    const tabbar = this.selectComponent('#tabbar');
    if (tabbar) tabbar.setCurrent();
    const app = getApp();
    if (app && app.triggerEvent) app.triggerEvent('notificationUpdate');
    this.loadSpaceView(this.data.viewIndex, { animate: true, loading: false });
  },

  onPullDownRefresh() {
    const d = new Date();
    const base = {
      dateLabel: formatDateLabel(d),
      presenceLine: nowPresenceLine(d)
    };
    this.setData({ ...base });
    this.loadSpaceView(this.data.viewIndex, { animate: true, loading: false })
      .finally(() => wx.stopPullDownRefresh());
  },

  async loadSpaceView(viewIndex, { animate = true, loading = false } = {}) {
    const idx = clampInt(viewIndex, 0, 2);
    try {
      const resp = await request.get('/health/space', { viewIndex: idx }, { loading });
      const data = (resp && (resp.data || resp)) || {};

      const next = {
        viewIndex: idx,
        completionHint: String(data.completionHint || ''),
        streakDays: Number(data.streakDays || 0),
        warmScore: Number(data.warmScore || 0),
        pulseSubtitle: String(data.pulseSubtitle || ''),
        pulse: data.pulse || { reached: 0, stepsAvg: 0, lightMealRate: 0 },
        barLeft: String(data.barLeft || ''),
        barRight: String(data.barRight || ''),
        timeline: Array.isArray(data.timeline) ? data.timeline : [],
        achievements: Array.isArray(data.achievements) ? data.achievements : []
      };

      const targetRate = clampInt(data.completionRate, 0, 100);
      this.setData({ ...next, completionRate: animate ? 0 : targetRate });
      if (animate) {
        setTimeout(() => animateValue(this, 0, targetRate, 900), 300);
        this.setData({ pulseAnimate: true });
        setTimeout(() => this.setData({ pulseAnimate: false }), 1200);
      }
      if (!next.achievements || next.achievements.length === 0) this.setAchievements();
    } catch (e) {
      const vm = fallbackViewModel(idx);
      this.setData({ ...vm, completionRate: animate ? 0 : vm.completionRate });
      if (animate) setTimeout(() => animateValue(this, 0, vm.completionRate, 900), 300);
      this.setAchievements();
    }
  },

  startMottoCycle() {
    this.setData({ currentMotto: mottoPool[0] });
    this.mottoTimer = setInterval(() => {
      let next = (this.data.mottoIndex + 1) % mottoPool.length;
      this.setData({ showMotto: false });
      setTimeout(() => {
        this.setData({ currentMotto: mottoPool[next], mottoIndex: next, showMotto: true });
      }, 600);
    }, 8000);
  },

  setAchievements() {
    const shuffled = [...achievementPool].sort(() => 0.5 - Math.random()).slice(0, 8);
    const stamped = shuffled.map((it, i) => ({ ...it, id: `${it.text}-${i}` }));
    this.setData({ achievements: stamped });
  },

  goAiAgent() {
    wx.switchTab({ url: '/pages/aiAgent/aiAgent' });
  },

  onViewSwitch(e) {
    const idx = clampInt(e.currentTarget.dataset.index, 0, 2);
    this.setData({ viewIndex: idx });
    this.loadSpaceView(idx, { animate: true, loading: false });
  },

  onTimelineTap(e) {
    const id = String(e.currentTarget.dataset.id || '');
    if (!id) return;
    wx.showToast({ title: '洞察已记录', icon: 'none' });
    wx.vibrateShort({ type: 'light' });
  },

  onUnload() {
    clearInterval(this.mottoTimer);
  }
});

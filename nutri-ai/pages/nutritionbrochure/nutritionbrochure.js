const { get } = require('../../utils/request')

const LOCAL_TIPS_POOL = [
  '早起先喝一杯温水，帮助补水与启动代谢',
  '午餐后散步10分钟，帮助稳定餐后血糖',
  '每餐先吃蔬菜再吃主食，饱腹更稳',
  '主食优先选全谷物：糙米、燕麦、全麦面包',
  '每天至少吃到300g蔬菜，颜色越丰富越好',
  '水果放在两餐之间，避免空腹猛吃甜水果',
  '蛋白质分配到三餐：鸡蛋、豆制品、鱼虾、瘦肉',
  '每天加一份豆制品：豆腐、豆浆、毛豆都行',
  '每周吃2次深海鱼，补充优质脂肪酸',
  '坚果每天一小把（约10g），别当零食无上限',
  '外卖尽量少选勾芡重油菜，优先清蒸/炖/煮',
  '汤底也有油：喝汤先撇油或少喝浓汤',
  '用小碗装零食，避免无意识吃完一大袋',
  '喝咖啡/奶茶先选无糖或半糖，逐步降糖',
  '运动前后都要补水，口渴时已是缺水信号',
  '坐久了每小时起身走动2分钟',
  '晚饭控制在睡前3小时完成，睡眠更稳',
  '晚上想吃东西先刷牙，降低继续进食概率',
  '熬夜后第二天别靠甜食续命，先补蛋白和蔬菜',
  '用"盘子法则"：1/2蔬菜 + 1/4蛋白 + 1/4主食',
  '每餐吃到七分饱，速度慢下来更容易做到',
  '先放下手机再吃饭，注意力回到咀嚼',
  '同样的热量，优先选"高蛋白+高纤维"组合',
  '想吃甜先吃水果，再决定要不要甜点',
  '晚间容易饿：把蛋白质放到晚餐或加一个酸奶',
  '酸奶优先选配料表短、蛋白高、少糖款',
  '喝酒前先吃点蛋白和蔬菜，别空腹饮酒',
  '饮酒后记得补水，第二天更舒服',
  '盐并不只在盐里：少吃腌制、香肠、辣条',
  '用香料代替部分盐：胡椒、孜然、蒜、柠檬汁',
  '每天晒太阳10分钟，心情与作息都更稳定',
  '给自己设一个"最小运动量"：比如每天走5000步',
  '楼梯能走就走，日常活动累积很关键',
  '深蹲10次/靠墙静蹲30秒，碎片化力量也有效',
  '餐后想睡：午餐减少精制主食，增加蛋白与蔬菜',
  '早餐加一份蛋白：鸡蛋/牛奶/豆浆',
  '早餐别只吃面包饼干，容易饿得快',
  '加班夜宵：优先选低油高蛋白，如无糖酸奶+水果',
  '饥饿感强时先喝水，分清口渴和饿',
  '把水果洗好放显眼处，让健康选择更容易',
  '购物清单先写好，减少冲动买零食',
  '想减脂：把含糖饮料换成无糖茶/气泡水',
  '想增肌：每餐保证一掌心蛋白质',
  '想控糖：主食定量，优先粗粮与豆类',
  '想稳作息：固定起床时间比固定入睡时间更重要',
  '工作压力大：下午做5分钟拉伸，放松肩颈',
  '肩颈酸：每小时做一次"下巴回收"10次',
  '睡前少刷刺激内容，换成轻音乐或阅读',
  '睡前1小时不喝浓茶咖啡，减少夜醒',
  '睡前泡脚10分钟，帮助入睡（注意水温别太高）',
  '用餐时先夹两口蔬菜，再开始吃主食',
  '主食减量别硬扛：用蔬菜和蛋白补上饱腹',
  '油条、炸鸡尽量当"偶尔"，不是日常',
  '烤肠、火腿属于加工肉，尽量少吃',
  '零食想吃：选原味坚果/海苔/酸奶更友好',
  '喝水困难：设置两次固定喝水提醒',
  '喝水加点柠檬片/薄荷，提升坚持概率',
  '出门带一根香蕉或一小包坚果，避免饿到乱买',
  '聚餐先夹菜再夹肉，最后再吃主食',
  '吃火锅：多点蔬菜豆制品，少喝浓汤',
  '吃烧烤：多选海鲜蔬菜，少选肥肉与甜酱',
  '甜口酱料热量高：番茄酱/沙拉酱都要控量',
  '把"每周一次"写进计划，比立誓更有效',
  '运动不一定要狠：先从每周2次开始',
  '走路时抬头收腹，步幅适中更省力',
  '久坐腰痛：站立伸展髋屈肌30秒',
  '补钙别只靠奶：豆腐、芝麻酱、小鱼虾也能补',
  '补铁：瘦红肉+维C水果搭配吸收更好',
  '补维C：彩椒、猕猴桃、橙子都很顶',
  '补膳食纤维：燕麦、豆类、菌菇、绿叶菜',
  '肠胃不舒服：少油少辣，先从清淡开始',
  '吃太快容易胀：每口咀嚼到20次左右',
  '想少吃点：先用小盘子盛饭',
  '夜里饿醒：白天三餐蛋白不足，优先补上',
  '体重波动正常：看趋势，别被单天数字影响',
  '情绪性进食：先记录触发点，再找替代行为',
  '工作日做减法：固定早餐模板减少决策疲劳',
  '一日三餐不规律：先把午餐固定住',
  '饭后甜：给自己留"甜点窗口"，避免全天零食',
  '高压日：把目标改成"维持"，也算成功',
  '运动后补一份蛋白，帮助恢复',
  '睡前太饿：加一杯温牛奶或无糖酸奶',
  '喝奶不适：试试酸奶或无乳糖牛奶',
  '每天称重太焦虑：改成每周固定两天称重',
  '把"今天做到了什么"写下来，比苛责更有效'
]

// 分类数据
const CATEGORIES = [
  { key: 'metabolism', name: '基础代谢' },
  { key: 'repair', name: '组织修复' },
  { key: 'disease', name: '疾病预防' },
  { key: 'beauty', name: '美容抗衰' },
  { key: 'mental', name: '心理健康' },
  { key: 'specific', name: '特定健康' },
  { key: 'organ', name: '器官修复' }
]

// 分类详情数据
const CATEGORY_DETAILS = [
  {
    key: 'metabolism',
    name: '基础代谢',
    subItems: ['热量代谢与能量平衡', '脂质代谢优化', '血糖稳态管理']
  },
  {
    key: 'repair',
    name: '组织修复',
    subItems: ['肌肉合成与修复', '伤口愈合促进', '骨密度与关节健康']
  },
  {
    key: 'disease',
    name: '疾病预防',
    subItems: ['免疫防御强化', '抗氧化与衰老调控', '抗炎与过敏调控']
  },
  {
    key: 'beauty',
    name: '美容抗衰',
    subItems: ['抗糖化与年轻化', '光防护与蓝光防御', '表皮屏障修复与保湿', '美白与色素调控', '防脱发与头皮健康']
  },
  {
    key: 'mental',
    name: '心理健康',
    subItems: ['情绪调节与心理健康']
  },
  {
    key: 'specific',
    name: '特定健康',
    subItems: ['母婴营养', '儿童营养', '银发族营养']
  },
  {
    key: 'organ',
    name: '器官修复',
    subItems: ['听力保护', '视力保护', '生殖健康保护', '肝脏保护']
  }
]

Page({
  data: {
    searchQuery: '',
    calorieConsumed: 0,
    calorieTarget: 2000,
    caloriePercent: 0,
    dailyMacros: {
      carb: { current: 158, target: 264, percent: 60 },
      fat: { current: 10, target: 45, percent: 22 },
      protein: { current: 30, target: 56, percent: 54 }
    },
    categories: CATEGORIES,
    selectedCategory: 'metabolism',
    categoryDetails: CATEGORY_DETAILS,
    showCategoryModal: false,
    tips: [
      '每天饮用8杯水，保持身体水分平衡',
      '保证7-8小时高质量睡眠',
      '每天至少30分钟中等强度运动',
      '多吃蔬菜水果，补充膳食纤维',
      '减少精制糖和加工食品摄入'
    ],
    regions: [
      { key: 'brain', name: '大脑', top: '6%', left: '47%' },
      { key: 'heart', name: '心脏', top: '24%', left: '48%' },
      { key: 'lungs', name: '肺部', top: '18%', left: '38%' },
      { key: 'liver', name: '肝脏', top: '32%', left: '56%' },
      { key: 'stomach', name: '胃', top: '38%', left: '48%' },
      { key: 'gut', name: '肠道', top: '47%', left: '50%' },
      { key: 'pancreas', name: '血糖', top: '42%', left: '60%' },
      { key: 'kidney', name: '肾脏', top: '36%', left: '64%' },
      { key: 'muscle', name: '肌肉', top: '58%', left: '44%' },
      { key: 'bone', name: '骨骼', top: '70%', left: '48%' },
      { key: 'skin', name: '皮肤', top: '14%', left: '60%' }
    ],
    cardList: [
      { name: '营养', id: '#nutrition-card' },
      { name: '人体', id: '#body-card' },
      { name: '菜品', id: '#recognition-card' },
      { name: '膳食', id: '#balance-card' },
      { name: '食谱', id: '#recipe-card' },
      { name: '小贴士', id: '#tips-card' }
    ],
    highlightedCard: '',
    isScrolling: false,
    regionScores: {},
    bodySuggestions: [],
    selectedMemberId: null,
    selectedMemberKey: 'me'
  },

  onShow() {
    const tabbar = this.selectComponent('#tabbar');
    if (tabbar) {
      tabbar.setCurrent();
    }
    const app = getApp();
    if (app && app.triggerEvent) app.triggerEvent('notificationUpdate');
    const today = this.getTodayDate();
    const selectedKey = (() => {
      try { return String(wx.getStorageSync('nutrition_selected_member') || 'me'); } catch (e) { return 'me'; }
    })();
    this.applyDailyLocalTips(selectedKey, today);
    this.loadTodaySummary();
  },

  // 返回上一页
  goBack() {
    wx.navigateBack({
      fail: () => {
        wx.switchTab({ url: '/pages/aiAgent/aiAgent' });
      }
    });
  },

  // 选择分类
  selectCategory(e) {
    const key = e.currentTarget.dataset.key;
    this.setData({ selectedCategory: key });
  },

  // 显示所有分类（打开详情弹窗）
  showAllCategories() {
    this.setData({ showCategoryModal: true });
  },

  // 隐藏分类详情弹窗
  hideCategoryModal() {
    this.setData({ showCategoryModal: false });
  },

  // 阻止冒泡
  preventBubble() {
    // 阻止事件冒泡
  },

  // 显示器官详情
  showOrganDetail(e) {
    const organ = e.currentTarget.dataset.organ;
    const organNames = {
      brain: '大脑',
      lungs: '肺部',
      liver: '肝脏',
      kidneys: '肾脏',
      stomach: '胃',
      eyes: '眼睛',
      heart: '心脏',
      spleen: '脾脏',
      intestine: '肠道'
    };
    wx.showToast({
      title: `${organNames[organ] || organ}详情`,
      icon: 'none'
    });
  },

  async loadTodaySummary() {
    const today = this.getTodayDate();
    const selectedKey = (() => {
      try { return String(wx.getStorageSync('nutrition_selected_member') || 'me'); } catch (e) { return 'me'; }
    })();

    const resolved = await this.tryLoadSummaryFromServer(selectedKey, today);
    if (!resolved) {
      this.syncSummaryFromDataPage();
    }
    const finalKey = (resolved && resolved.memberKey) ? resolved.memberKey : (() => {
      try { return String(wx.getStorageSync('nutrition_selected_member') || selectedKey || 'me'); } catch (e) { return String(selectedKey || 'me'); }
    })();
    const finalId = resolved && resolved.memberId ? Number(resolved.memberId) : null;
    this.setData({
      selectedMemberKey: String(finalKey || 'me'),
      selectedMemberId: Number.isFinite(finalId) && finalId > 0 ? finalId : null
    });
    this.applyDailyLocalTips(String(finalKey || 'me'), today);
    if (this.data.selectedMemberId) {
      await this.trySyncBodyImpactFromServer(this.data.selectedMemberId, String(finalKey || 'me'), today);
    }
    this.syncBodyImpactFromDataPage(finalKey, today);
  },

  applyDailyLocalTips(memberKey, date) {
    const mk = String(memberKey || 'me').trim() || 'me';
    const dt = String(date || this.getTodayDate()).trim() || this.getTodayDate();
    const pool = Array.isArray(LOCAL_TIPS_POOL) ? LOCAL_TIPS_POOL.filter(Boolean) : [];
    if (!pool.length) return;
    const seed = this.hashString32(`${dt}|${mk}|tips`);
    const picked = this.pickUniqueBySeed(pool, seed, 6);
    this.setData({ tips: picked });
  },

  hashString32(input) {
    const s = String(input || '');
    let h = 2166136261;
    for (let i = 0; i < s.length; i++) {
      h ^= s.charCodeAt(i);
      h = Math.imul(h, 16777619);
    }
    return h >>> 0;
  },

  pickUniqueBySeed(list, seed, count) {
    const arr = Array.isArray(list) ? list.filter(Boolean) : [];
    const n = Math.max(1, Math.min(arr.length, Number(count) || 0));
    if (!n) return [];
    let state = (Number(seed) >>> 0) || 1;
    const out = [];
    const used = new Set();
    const nextRand = () => {
      state = (Math.imul(state, 1664525) + 1013904223) >>> 0;
      return state;
    };
    let guard = 0;
    while (out.length < n && guard < arr.length * 6) {
      guard++;
      const idx = nextRand() % arr.length;
      if (used.has(idx)) continue;
      used.add(idx);
      out.push(arr[idx]);
    }
    if (out.length < n) {
      for (let i = 0; i < arr.length && out.length < n; i++) {
        if (used.has(i)) continue;
        used.add(i);
        out.push(arr[i]);
      }
    }
    return out;
  },

  async tryLoadSummaryFromServer(selectedKey, date) {
    try {
      const body = await get('/nutrition/members', {}, { loading: false, showError: false })
      const list = body && Array.isArray(body.data) ? body.data : []
      if (!list.length) return null

      let picked = list.find((m) => String(m && m.memberKey ? m.memberKey : '') === String(selectedKey || ''))
      if (!picked) picked = list.find((m) => String(m && m.memberKey ? m.memberKey : '') === 'me') || list[0]
      const memberId = picked && picked.id != null ? Number(picked.id) : null
      const memberKey = String(picked && picked.memberKey ? picked.memberKey : selectedKey || 'me')
      if (!memberId) return null

      if (memberKey && memberKey !== selectedKey) {
        try { wx.setStorageSync('nutrition_selected_member', memberKey); } catch (e) {}
      }

      const dayBody = await get('/nutrition/day', { memberId, date: String(date || '') }, { loading: false, showError: false })
      const day = dayBody && dayBody.data && typeof dayBody.data === 'object' ? dayBody.data : null
      if (!day) return null

      this.applyServerDaySummary(memberKey, day)
      return { memberId, memberKey }
    } catch (e) {
      return null
    }
  },

  applyServerDaySummary(memberKey, day) {
    const d = day && typeof day === 'object' ? day : {}
    const macroTotals = d.macroTotals && typeof d.macroTotals === 'object' ? d.macroTotals : {}
    const macroTargets = d.macroTargets && typeof d.macroTargets === 'object' ? d.macroTargets : {}
    const macroPercent = d.macroPercent && typeof d.macroPercent === 'object' ? d.macroPercent : {}

    const calorieConsumed = Math.max(0, Math.round(Number(d.calorieConsumed) || 0))
    const calorieTarget = Math.max(1, Math.round(Number(d.calorieTarget) || 2000))
    const caloriePercent = Math.max(0, Math.min(100, Math.round((calorieConsumed / calorieTarget) * 100)))

    const totals = {
      carb: Number(macroTotals.carb) || 0,
      fat: Number(macroTotals.fat) || 0,
      protein: Number(macroTotals.protein) || 0
    }
    const targets = {
      carb: Number(macroTargets.carb) || 264,
      fat: Number(macroTargets.fat) || 67,
      protein: Number(macroTargets.protein) || 100
    }
    const percent = {
      carb: Number(macroPercent.carb) || 0,
      fat: Number(macroPercent.fat) || 0,
      protein: Number(macroPercent.protein) || 0
    }

    this.setData({
      calorieConsumed,
      calorieTarget,
      caloriePercent,
      dailyMacros: {
        carb: { current: Math.round(totals.carb), target: targets.carb, percent: percent.carb },
        fat: { current: Math.round(totals.fat), target: targets.fat, percent: percent.fat },
        protein: { current: Math.round(totals.protein), target: targets.protein, percent: percent.protein }
      }
    })

    let byMember = {}
    try { byMember = wx.getStorageSync('nutrition_daily_macros_by_member') || {}; } catch (e) { byMember = {}; }
    byMember[String(memberKey || 'me')] = {
      date: String(d.date || this.getTodayDate()),
      totals,
      targets,
      percent,
      calorieConsumed,
      calorieTarget
    }
    try { wx.setStorageSync('nutrition_daily_macros_by_member', byMember); } catch (e2) {}
  },

  getTodayDate() {
    const d = new Date();
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  },

  syncSummaryFromDataPage() {
    let byMember = {};
    let selected = 'me';
    try { byMember = wx.getStorageSync('nutrition_daily_macros_by_member') || {}; } catch (e) { byMember = {}; }
    try { selected = wx.getStorageSync('nutrition_selected_member') || 'me'; } catch (e) { selected = 'me'; }
    const payload = byMember && byMember[selected] ? byMember[selected] : null;
    const today = this.getTodayDate();

    if (payload && payload.totals && payload.targets && payload.percent && (!payload.date || String(payload.date) === today)) {
      const t = payload.totals;
      const g = payload.targets;
      const p = payload.percent;
      const calorieConsumed = Math.max(0, Math.round(Number(payload.calorieConsumed) || 0));
      const calorieTarget = Math.max(1, Math.round(Number(payload.calorieTarget) || 2000));
      const caloriePercent = Math.max(0, Math.min(100, Math.round((calorieConsumed / calorieTarget) * 100)));
      this.setData({
        calorieConsumed,
        calorieTarget,
        caloriePercent,
        dailyMacros: {
          carb: { current: Math.round(Number(t.carb) || 0), target: Number(g.carb) || 0, percent: Number(p.carb) || 0 },
          fat: { current: Math.round(Number(t.fat) || 0), target: Number(g.fat) || 0, percent: Number(p.fat) || 0 },
          protein: { current: Math.round(Number(t.protein) || 0), target: Number(g.protein) || 0, percent: Number(p.protein) || 0 }
        }
      });
    }
  },

  syncBodyImpactFromDataPage(memberKey, date) {
    let store = {};
    try { store = wx.getStorageSync('nutrition_organ_impact_by_member') || {}; } catch (e) { store = {}; }
    const byMember = store && typeof store === 'object' ? store : {};
    const member = byMember && byMember[memberKey] && typeof byMember[memberKey] === 'object' ? byMember[memberKey] : null;
    const node = member && member[date] && typeof member[date] === 'object' ? member[date] : null;
    const impactByKey = node && node.impactByKey && typeof node.impactByKey === 'object' ? node.impactByKey : {};
    const impacts = Array.isArray(node && node.impacts) ? node.impacts : [];

    const toScore = (delta) => {
      const d = Number(delta) || 0;
      return Math.max(0, Math.min(100, Math.round(50 + d * 5)));
    };

    const regionScores = {
      lungs: toScore(impactByKey.lungs && impactByKey.lungs.delta),
      muscle: 50,
      bone: 50,
      skin: 50,
      brain: toScore(impactByKey.brain && impactByKey.brain.delta),
      heart: toScore(impactByKey.heart && impactByKey.heart.delta),
      liver: toScore(impactByKey.liver && impactByKey.liver.delta),
      stomach: toScore(impactByKey.stomach && impactByKey.stomach.delta),
      pancreas: toScore(impactByKey.pancreas && impactByKey.pancreas.delta),
      kidney: toScore(impactByKey.kidney && impactByKey.kidney.delta),
      gut: toScore(impactByKey.intestine && impactByKey.intestine.delta)
    };

    const bodySuggestions = impacts
      .filter((it) => it && Number(it.delta) < 0)
      .sort((a, b) => Number(a.delta) - Number(b.delta))
      .slice(0, 3)
      .map((it) => ({
        key: String(it.key || ''),
        title: `${String(it.name || '器官')}：${String(it.deltaText || '')}`,
        text: String(it.reason || '')
      }));

    this.setData({ regionScores, bodySuggestions });
  },

  async trySyncBodyImpactFromServer(memberId, memberKey, date) {
    const id = Number(memberId);
    const mk = String(memberKey || 'me').trim() || 'me';
    const dt = String(date || this.getTodayDate());
    if (!Number.isFinite(id) || id <= 0) return false;

    try {
      const body = await get('/nutrition/organ-impact', { memberId: id, date: dt }, { loading: false, showError: false });
      const payload = body && body.data && typeof body.data === 'object' ? body.data : null;
      const profile = payload && payload.profile && typeof payload.profile === 'object' ? payload.profile : null;
      const impactByKey = payload && payload.impactByKey && typeof payload.impactByKey === 'object' ? payload.impactByKey : null;
      const impacts = payload && Array.isArray(payload.impacts) ? payload.impacts : null;
      if (!profile || !impactByKey || !impacts) return false;

      let store = {};
      try { store = wx.getStorageSync('nutrition_organ_impact_by_member') || {}; } catch (e) { store = {}; }
      const byMember = store && typeof store === 'object' ? store : {};
      const nextMember = (byMember[mk] && typeof byMember[mk] === 'object') ? byMember[mk] : {};
      nextMember[dt] = { profile, impactByKey, impacts };
      const next = { ...byMember, [mk]: nextMember };
      try { wx.setStorageSync('nutrition_organ_impact_by_member', next); } catch (e2) {}
      return true;
    } catch (e) {
      return false;
    }
  },

  updateSearchQuery(e) {
    this.setData({ searchQuery: e.detail.value });
  },

  searchFood() {
    wx.showToast({ title: `搜索: ${this.data.searchQuery}` });
  },

  navigateToDataPage() {
    wx.navigateTo({ url: '/pages/nutritionbrochure/DataPage/DataPage' });
  },

  navigateToFoodRecordPage() {
    wx.navigateTo({ url: '/pages/nutritionbrochure/FoodRecord/FoodRecord' });
  },

  navigateToDetail() {
    const date = this.getTodayDate();
    const member = String(this.data.selectedMemberKey || (() => {
      try { return String(wx.getStorageSync('nutrition_selected_member') || 'me'); } catch (e) { return 'me'; }
    })());
    const memberId = this.data.selectedMemberId;
    const idPart = memberId ? `&memberId=${encodeURIComponent(String(memberId))}` : '';
    wx.navigateTo({ url: `/pages/nutritionbrochure/BodyImpactDetail/BodyImpactDetail?member=${encodeURIComponent(member)}&date=${encodeURIComponent(date)}${idPart}` });
  },

  navigateToRecognitionPage() {
    wx.navigateTo({ url: '/pages/nutritionbrochure/recognition-page/recognition-page' });
  },

  navigateToBalancePage() {
    wx.navigateTo({ url: '/pages/nutrition/balance' });
  },

  navigateToRecipePage() {
    wx.navigateTo({ url: '/pages/nutritionbrochure/foodpage/foodpage' });
  },

  onScroll(e) {
    this.setData({
      isScrolling: e.detail.scrollTop > 0
    });
  },

  getRegionColor(score) {
    if (score > 70) return 'linear-gradient(90deg, #00c896, #00a2ff)';
    if (score > 40) return 'linear-gradient(90deg, #cbd5e1, #e2e8f0)';
    return 'linear-gradient(90deg, #ff9a9e, #ff6b6b)';
  },

  showRegionDetail(e) {
    const region = e.currentTarget.dataset.region;
    const r = region && typeof region === 'object' ? region : {};
    const key = String(r.key || '').trim();
    if (!key) return;

    const map = { gut: 'intestine' };
    const organKey = map[key] || key;
    const supported = ['brain', 'heart', 'lungs', 'liver', 'stomach', 'intestine', 'pancreas', 'kidney', 'eyes'];
    if (!supported.includes(organKey)) {
      wx.showToast({ title: '该部位暂未接入详情', icon: 'none' });
      return;
    }

    const date = this.getTodayDate();
    const member = String(this.data.selectedMemberKey || 'me');
    const memberId = this.data.selectedMemberId;
    const idPart = memberId ? `&memberId=${encodeURIComponent(String(memberId))}` : '';
    wx.navigateTo({ url: `/pages/nutritionbrochure/BodyImpactDetail/BodyImpactDetail?key=${encodeURIComponent(organKey)}&member=${encodeURIComponent(member)}&date=${encodeURIComponent(date)}${idPart}` });
  }
});

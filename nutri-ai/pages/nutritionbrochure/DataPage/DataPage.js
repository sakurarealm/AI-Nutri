const { uploadFile, get, post, delete: del } = require('../../../utils/request')
const NUTRITION_MEAL_DATA_KEY = 'nutrition_meal_data_by_member'
const VIP_CACHE_KEY = 'vip_cache'
const NUTRITION_FAMILY_MEMBERS_LOCAL_KEY = 'nutrition_family_members_local'

Page({
  data: {
    weekDays: ['日', '一', '二', '三', '四', '五', '六'],
    currentMonthText: '',
    monthPickerValue: '',
    monthDays: [],
    selectedDate: '',
    dateScrollIntoView: '',
    dateScrollLeft: 0,
    dateTopLineW: 110,
    calorieConsumed: 0,
    calorieTarget: 2000,
    macroTargets: { carb: 264, protein: 100, fat: 67, fiber: 25 },
    macroItems: [
      { key: 'carb', name: '碳水', percent: 0, value: '0/264克' },
      { key: 'fat', name: '脂肪', percent: 0, value: '0/67克' },
      { key: 'protein', name: '蛋白', percent: 0, value: '0/100克' },
      { key: 'fiber', name: '纤维', percent: 0, value: '0/25克' }
    ],
    mealDataByMember: {
      me: {
        breakfastFoods: [
          { name: '米饭', unit: '1碗', nutrient: '110克碳水', calorie: 260, img: 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=200&q=80' },
          { name: '煎蛋', unit: '2个', nutrient: '15克蛋白', calorie: 190, img: 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?auto=format&fit=crop&w=200&q=80' },
          { name: '蔬菜沙拉', unit: '1份', nutrient: '30克纤维', calorie: 120, img: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=200&q=80' }
        ],
        lunchFoods: [
          { name: '鸡胸肉', unit: '150克', nutrient: '35克蛋白', calorie: 250, img: 'https://images.unsplash.com/photo-1604909053197-45c574b3c0c9?auto=format&fit=crop&w=200&q=80' },
          { name: '西兰花', unit: '1份', nutrient: '20克纤维', calorie: 55, img: 'https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=200&q=80' },
          { name: '糙米饭', unit: '1碗', nutrient: '80克碳水', calorie: 215, img: 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=200&q=80' }
        ],
        dinnerFoods: [
          { name: '烤三文鱼', unit: '200克', nutrient: '45克蛋白', calorie: 410, img: 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=200&q=80' },
          { name: '糙米饭', unit: '1碗', nutrient: '90克碳水', calorie: 215, img: 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=200&q=80' },
          { name: '清炒西兰花', unit: '1份', nutrient: '25克纤维', calorie: 80, img: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=200&q=80' }
        ]
      },
      dad: {
        breakfastFoods: [
          { name: '豆浆', unit: '300ml', nutrient: '18克蛋白', calorie: 150, img: 'https://images.unsplash.com/photo-1526318472351-c75fcf070305?auto=format&fit=crop&w=200&q=80' },
          { name: '全麦面包', unit: '2片', nutrient: '45克碳水', calorie: 220, img: 'https://images.unsplash.com/photo-1549931319-a545dcf3bc73?auto=format&fit=crop&w=200&q=80' },
          { name: '苹果', unit: '1个', nutrient: '20克碳水', calorie: 95, img: 'https://images.unsplash.com/photo-1567306226416-28f0efdc88ce?auto=format&fit=crop&w=200&q=80' }
        ],
        lunchFoods: [
          { name: '番茄牛腩', unit: '1份', nutrient: '30克蛋白', calorie: 420, img: 'https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=200&q=80' },
          { name: '米饭', unit: '1碗', nutrient: '85克碳水', calorie: 260, img: 'https://images.unsplash.com/photo-1604908554162-3bfc3cd1f5d8?auto=format&fit=crop&w=200&q=80' },
          { name: '清炒青菜', unit: '1份', nutrient: '18克纤维', calorie: 70, img: 'https://images.unsplash.com/photo-1510626176961-4b57d4fbad03?auto=format&fit=crop&w=200&q=80' }
        ],
        dinnerFoods: [
          { name: '清蒸鱼', unit: '200克', nutrient: '40克蛋白', calorie: 320, img: 'https://images.unsplash.com/photo-1604908176997-1255a17c4f1e?auto=format&fit=crop&w=200&q=80' },
          { name: '紫薯', unit: '1个', nutrient: '35克碳水', calorie: 130, img: 'https://images.unsplash.com/photo-1604908177321-5cb9184160a9?auto=format&fit=crop&w=200&q=80' },
          { name: '菌菇汤', unit: '1碗', nutrient: '10克蛋白', calorie: 80, img: 'https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=200&q=80' }
        ]
      },
      mom: {
        breakfastFoods: [
          { name: '燕麦酸奶杯', unit: '1杯', nutrient: '28克碳水', calorie: 320, img: 'https://images.unsplash.com/photo-1514996937319-344454492b37?auto=format&fit=crop&w=200&q=80' },
          { name: '水煮蛋', unit: '1个', nutrient: '7克蛋白', calorie: 80, img: 'https://images.unsplash.com/photo-1551218808-94e220e084d2?auto=format&fit=crop&w=200&q=80' },
          { name: '坚果', unit: '20克', nutrient: '12克脂肪', calorie: 120, img: 'https://images.unsplash.com/photo-1546554137-f86b9593a222?auto=format&fit=crop&w=200&q=80' }
        ],
        lunchFoods: [
          { name: '虾仁西蓝花', unit: '1份', nutrient: '26克蛋白', calorie: 260, img: 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=200&q=80' },
          { name: '藜麦', unit: '1碗', nutrient: '65克碳水', calorie: 220, img: 'https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?auto=format&fit=crop&w=200&q=80' },
          { name: '小番茄', unit: '1份', nutrient: '10克碳水', calorie: 30, img: 'https://images.unsplash.com/photo-1546470427-e26264be0b5a?auto=format&fit=crop&w=200&q=80' }
        ],
        dinnerFoods: [
          { name: '鸡汤', unit: '1碗', nutrient: '22克蛋白', calorie: 160, img: 'https://images.unsplash.com/photo-1547592166-23ac45744acd?auto=format&fit=crop&w=200&q=80' },
          { name: '清炒时蔬', unit: '1份', nutrient: '16克纤维', calorie: 90, img: 'https://images.unsplash.com/photo-1546069901-d5bfd2cbfb1f?auto=format&fit=crop&w=200&q=80' },
          { name: '玉米', unit: '1根', nutrient: '40克碳水', calorie: 140, img: 'https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=200&q=80' }
        ]
      }
    },
    breakfastFoods: [],
    lunchFoods: [],
    dinnerFoods: [],
    snackCards: [],
    familyDrawerOpen: false,
    selectedMemberKey: 'me',
    selectedMemberId: null,
    serverReady: false,
    serverMemberKeyToId: {},
    vipInfo: { level: 0, levelName: '普通会员', expireDate: null, planId: '', familySeats: 0 },
    familyAddLocked: false,
    familyMembers: [
      { key: 'me', name: '我', type: 'me' },
      { key: 'dad', name: '爸爸', type: 'member' },
      { key: 'mom', name: '妈妈', type: 'member' },
      { key: 'add', name: '＋', type: 'add' }
    ],
    addFoodModalOpen: false,
    addFoodTarget: null,
    deleteConfirmOpen: false,
    deleteFoodTarget: null,
    organImpacts: []
  },

  isDateKey(k) {
    return /^\d{4}-\d{2}-\d{2}$/.test(String(k || ''))
  },

  resolveLocalDayPayload(memberKey, date) {
    const key = String(memberKey || 'me')
    const d = String(date || this.data.selectedDate || this.getTodayDate())
    const map = this.data.mealDataByMember || {}
    const node = map[key] || map.me || {}
    if (!node || typeof node !== 'object') return {}
    if (Array.isArray(node.breakfastFoods) || Array.isArray(node.lunchFoods) || Array.isArray(node.dinnerFoods) || Array.isArray(node.snackCards)) {
      return node
    }
    if (this.isDateKey(d) && node[d] && typeof node[d] === 'object') return node[d]
    return {}
  },

  upsertLocalDayPayload(memberKey, date, payload) {
    const key = String(memberKey || 'me')
    const d = String(date || this.data.selectedDate || this.getTodayDate())
    const map = this.data.mealDataByMember || {}
    const prevNode = map[key]
    const nextPayload = (payload && typeof payload === 'object') ? payload : {}
    let nextNode = prevNode && typeof prevNode === 'object' ? prevNode : {}
    if (Array.isArray(nextNode.breakfastFoods) || Array.isArray(nextNode.lunchFoods) || Array.isArray(nextNode.dinnerFoods) || Array.isArray(nextNode.snackCards)) {
      nextNode = { [d]: nextNode }
    }
    nextNode = { ...nextNode, [d]: nextPayload }
    const merged = { ...map, [key]: nextNode }
    this.setData({ mealDataByMember: merged })
    try { wx.setStorageSync(NUTRITION_MEAL_DATA_KEY, merged) } catch (e) {}
  },

  getMemberIdByKey(memberKey) {
    const key = String(memberKey || '')
    const map = this.data.serverMemberKeyToId || {}
    const id = map && typeof map === 'object' ? map[key] : null
    return id ? Number(id) : null
  },

  getVipInfoFromCache() {
    try {
      const raw = wx.getStorageSync(VIP_CACHE_KEY) || {}
      const level = Number(raw.level ?? raw.vipLevel ?? 0) || 0
      const planId = String(raw.planId ?? raw.plan ?? raw.planType ?? '').trim()
      const familySeats = Number(raw.familySeats ?? raw.familyCount ?? 0) || 0
      return { ...(raw && typeof raw === 'object' ? raw : {}), level, planId, familySeats }
    } catch (e) {
      return { level: 0, planId: '', familySeats: 0 }
    }
  },

  refreshFamilyAddLock() {
    const vip = this.data.vipInfo || this.getVipInfoFromCache()
    const purchased = Math.max(0, Number(vip.familySeats) || 0)
    const allowedExtra = 1 + purchased
    const members = Array.isArray(this.data.familyMembers) ? this.data.familyMembers : []
    const currentExtra = members.filter((m) => m && m.type === 'member').length
    this.setData({ familyAddLocked: currentExtra >= allowedExtra })
  },

  async tryRefreshVipInfo() {
    const cached = this.getVipInfoFromCache()
    this.setData({ vipInfo: { ...this.data.vipInfo, ...cached } }, () => this.refreshFamilyAddLock())
    try {
      const body = await get('/user/vip', {}, { loading: false, showError: false })
      const v = body && body.data && typeof body.data === 'object' ? body.data : null
      if (!v) return
      const level = Number(v.level ?? v.vipLevel ?? 0) || 0
      const planId = String(v.planId ?? v.plan ?? v.planType ?? '').trim()
      const familySeats = Number(v.familySeats ?? v.familyCount ?? 0) || 0
      const next = { ...(v && typeof v === 'object' ? v : {}), level, planId, familySeats }
      try { wx.setStorageSync(VIP_CACHE_KEY, next) } catch (e) {}
      this.setData({ vipInfo: { ...this.data.vipInfo, ...next } }, () => this.refreshFamilyAddLock())
    } catch (e) {}
  },

  ensureCanAddMemberOrPrompt() {
    if (!this.data.familyAddLocked) return true
    wx.showModal({
      title: '成员位不足',
      content: '普通会员仅 1 个免费成员位。开通会员或购买加座可继续添加。',
      confirmText: '去开通',
      cancelText: '取消',
      success: (res) => {
        if (!res || !res.confirm) return
        wx.navigateTo({ url: '/pages/profile/vip/vip' })
      }
    })
    return false
  },

  loadLocalFamilyMembers() {
    try {
      const saved = wx.getStorageSync(NUTRITION_FAMILY_MEMBERS_LOCAL_KEY)
      const list = Array.isArray(saved) ? saved : []
      const members = list
        .filter((m) => m && typeof m === 'object')
        .map((m) => ({
          key: String(m.key || '').trim() || `m_${Date.now()}`,
          name: String(m.name || '成员').trim() || '成员',
          type: 'member'
        }))
      const familyMembers = [{ key: 'me', name: '我', type: 'me' }, ...members, { key: 'add', name: '＋', type: 'add' }]
      this.setData({ familyMembers }, () => this.refreshFamilyAddLock())
    } catch (e) {}
  },

  saveLocalFamilyMembers() {
    const members = (Array.isArray(this.data.familyMembers) ? this.data.familyMembers : [])
      .filter((m) => m && m.type === 'member')
      .map((m) => ({ key: String(m.key || ''), name: String(m.name || '') }))
    try { wx.setStorageSync(NUTRITION_FAMILY_MEMBERS_LOCAL_KEY, members) } catch (e) {}
  },

  async tryLoadServerMembersAndDay() {
    try {
      const body = await get('/nutrition/members', {}, { loading: false, showError: false })
      const list = body && Array.isArray(body.data) ? body.data : []
      if (!list.length) {
        this.setData({ serverReady: false })
        return
      }
      const keyToId = {}
      const familyMembers = list.map((m) => {
        const memberKey = String((m && m.memberKey) || '').trim()
        const id = m && m.id ? Number(m.id) : null
        if (memberKey && id) keyToId[memberKey] = id
        const nickname = String((m && m.nickname) || '').trim()
        const type = memberKey === 'me' ? 'me' : 'member'
        return { id, key: memberKey || `m_${Date.now()}`, name: nickname || memberKey || '成员', type }
      })
      familyMembers.push({ key: 'add', name: '＋', type: 'add' })

      let selectedKey = String(this.data.selectedMemberKey || 'me')
      if (!keyToId[selectedKey]) selectedKey = keyToId.me ? 'me' : (familyMembers[0] ? String(familyMembers[0].key || 'me') : 'me')
      const selectedMemberId = keyToId[selectedKey] ? Number(keyToId[selectedKey]) : null

      this.setData({
        serverReady: true,
        serverMemberKeyToId: keyToId,
        familyMembers,
        selectedMemberKey: selectedKey,
        selectedMemberId
      }, () => this.refreshFamilyAddLock())

      if (selectedMemberId) {
        await this.loadDayFromServer(selectedMemberId, this.data.selectedDate)
      } else {
        this.applyMemberMeals(selectedKey, this.data.selectedDate)
      }
    } catch (e) {
      this.setData({ serverReady: false })
    }
  },

  normalizeFoodList(list) {
    const arr = Array.isArray(list) ? list : []
    return arr.map((f) => {
      const item = (f && typeof f === 'object') ? f : {}
      const calorie = Number(item.calorie)
      const carb = Number(item.carb)
      const protein = Number(item.protein)
      const fat = Number(item.fat)
      const fiber = Number(item.fiber)
      return {
        name: String(item.name || ''),
        unit: String(item.unit || ''),
        nutrient: String(item.nutrient || ''),
        calorie: Number.isNaN(calorie) ? 0 : calorie,
        carb: Number.isNaN(carb) ? (item.carb || 0) : carb,
        protein: Number.isNaN(protein) ? (item.protein || 0) : protein,
        fat: Number.isNaN(fat) ? (item.fat || 0) : fat,
        fiber: Number.isNaN(fiber) ? (item.fiber || 0) : fiber,
        img: String(item.img || ''),
        note: String(item.note || '')
      }
    }).filter((x) => x && x.name)
  },

  normalizeSnackCards(cards) {
    const arr = Array.isArray(cards) ? cards : []
    return arr.map((c) => {
      const card = this.ensureSnackCardId((c && typeof c === 'object') ? c : {})
      const foods = this.normalizeFoodList(card && Array.isArray(card.foods) ? card.foods : [])
      return { ...card, foods, level: Math.max(0, Number(card.level) || 0) }
    })
  },

  applyDayToUI(day) {
    const d = day && typeof day === 'object' ? day : {}
    const calorieTarget = Number(d.calorieTarget)
    const macroTargets = d.macroTargets && typeof d.macroTargets === 'object' ? d.macroTargets : null
    const next = {
      breakfastFoods: this.normalizeFoodList(d.breakfastFoods),
      lunchFoods: this.normalizeFoodList(d.lunchFoods),
      dinnerFoods: this.normalizeFoodList(d.dinnerFoods),
      snackCards: this.normalizeSnackCards(d.snackCards)
    }
    if (!Number.isNaN(calorieTarget) && calorieTarget > 0) next.calorieTarget = calorieTarget
    if (macroTargets) {
      next.macroTargets = {
        carb: Number(macroTargets.carb) || this.data.macroTargets.carb,
        protein: Number(macroTargets.protein) || this.data.macroTargets.protein,
        fat: Number(macroTargets.fat) || this.data.macroTargets.fat,
        fiber: Number(macroTargets.fiber) || this.data.macroTargets.fiber
      }
    }
    this.setData(next, () => this.updateDailyMacros())
  },

  cacheDayToLocal(memberKey, date, day) {
    const d = day && typeof day === 'object' ? day : {}
    const payload = {
      breakfastFoods: this.normalizeFoodList(d.breakfastFoods),
      lunchFoods: this.normalizeFoodList(d.lunchFoods),
      dinnerFoods: this.normalizeFoodList(d.dinnerFoods),
      snackCards: this.normalizeSnackCards(d.snackCards),
      calorieTarget: Number(d.calorieTarget) || this.data.calorieTarget,
      macroTargets: d.macroTargets && typeof d.macroTargets === 'object' ? {
        carb: Number(d.macroTargets.carb) || this.data.macroTargets.carb,
        protein: Number(d.macroTargets.protein) || this.data.macroTargets.protein,
        fat: Number(d.macroTargets.fat) || this.data.macroTargets.fat,
        fiber: Number(d.macroTargets.fiber) || this.data.macroTargets.fiber
      } : this.data.macroTargets
    }
    this.upsertLocalDayPayload(String(memberKey || this.data.selectedMemberKey || 'me'), String(date || this.data.selectedDate || this.getTodayDate()), payload)
  },

  async loadDayFromServer(memberId, date) {
    const memberKey = String(this.data.selectedMemberKey || 'me')
    const d = String(date || this.data.selectedDate || this.getTodayDate())
    try {
      const body = await get('/nutrition/day', { memberId: Number(memberId), date: d }, { loading: false, showError: false })
      const day = body && body.data && typeof body.data === 'object' ? body.data : null
      if (!day) {
        this.applyMemberMeals(memberKey, d)
        return
      }
      const serverDate = String(day.date || d)
      this.cacheDayToLocal(memberKey, serverDate, day)
      this.applyDayToUI(day)
    } catch (e) {
      this.applyMemberMeals(memberKey, d)
    }
  },

  buildDaySaveReq(memberId, date) {
    const macroTargets = this.data.macroTargets || { carb: 264, protein: 100, fat: 67, fiber: 25 }
    return {
      memberId: Number(memberId),
      date: String(date || this.data.selectedDate || this.getTodayDate()),
      calorieTarget: Number(this.data.calorieTarget) || 2000,
      carbTarget: Number(macroTargets.carb) || 264,
      proteinTarget: Number(macroTargets.protein) || 100,
      fatTarget: Number(macroTargets.fat) || 67,
      breakfastFoods: this.normalizeFoodList(this.data.breakfastFoods),
      lunchFoods: this.normalizeFoodList(this.data.lunchFoods),
      dinnerFoods: this.normalizeFoodList(this.data.dinnerFoods),
      snackCards: this.normalizeSnackCards(this.data.snackCards).map((c) => ({
        id: String(c.id || ''),
        level: Math.max(0, Number(c.level) || 0),
        foods: this.normalizeFoodList(c.foods)
      }))
    }
  },

  scheduleSaveDay() {
    if (this._saveDayTimer) clearTimeout(this._saveDayTimer)
    this._saveDayTimer = setTimeout(() => {
      this._saveDayTimer = null
      this.saveDayNow({ silent: true })
    }, 800)
  },

  async saveDayNow({ silent } = {}) {
    if (!this.data.serverReady) return
    const memberKey = String(this.data.selectedMemberKey || 'me')
    const memberId = this.getMemberIdByKey(memberKey) || this.data.selectedMemberId
    if (!memberId) return
    const date = String(this.data.selectedDate || this.getTodayDate())
    const payload = this.buildDaySaveReq(memberId, date)
    try {
      const body = await post('/nutrition/day', payload, { loading: !silent, showError: !silent })
      const day = body && body.data && typeof body.data === 'object' ? body.data : null
      if (day) {
        this.cacheDayToLocal(memberKey, String(day.date || date), day)
      }
    } catch (e) {}
  },

  onSelectedDateChanged() {
    const memberKey = String(this.data.selectedMemberKey || 'me')
    const date = String(this.data.selectedDate || this.getTodayDate())
    const memberId = this.getMemberIdByKey(memberKey)
    if (memberId && memberId !== this.data.selectedMemberId) {
      this.setData({ selectedMemberId: memberId })
    }
    if (this.data.serverReady && memberId) {
      this.loadDayFromServer(memberId, date)
      return
    }
    this.applyMemberMeals(memberKey, date)
  },

  onLoad() {
    this.initCalendar();
    this.tryRefreshVipInfo();
    this.loadLocalFamilyMembers();
    let memberKey = this.data.selectedMemberKey;
    try {
      const saved = wx.getStorageSync('nutrition_selected_member');
      if (saved) memberKey = String(saved);
    } catch (e) {}
    try {
      const savedMeals = wx.getStorageSync(NUTRITION_MEAL_DATA_KEY);
      if (savedMeals && typeof savedMeals === 'object') {
        this.setData({ mealDataByMember: { ...(this.data.mealDataByMember || {}), ...savedMeals } });
      }
    } catch (e) {}
    this.setData({ selectedMemberKey: memberKey }, () => {
      this.applyMemberMeals(memberKey, this.data.selectedDate)
    });
    this.tryLoadServerMembersAndDay();
  },

  onHide() {
    this.persistCurrentMemberMeals()
    this.saveDayNow({ silent: true })
  },

  onUnload() {
    this.persistCurrentMemberMeals()
    this.saveDayNow({ silent: true })
  },

  toggleFamilyDrawer() {
    this.setData({ familyDrawerOpen: !this.data.familyDrawerOpen });
  },

  closeFamilyDrawer() {
    this.setData({ familyDrawerOpen: false });
  },

  onFamilyCellTap(e) {
    if (this._familyCellTapBlockedUntil && Date.now() < this._familyCellTapBlockedUntil) return
    const key = e && e.currentTarget && e.currentTarget.dataset ? String(e.currentTarget.dataset.key || '') : '';
    if (!key) return;
    if (key === 'add') {
      if (!this.ensureCanAddMemberOrPrompt()) return
      this.closeFamilyDrawer();
      this.onAddMemberTap();
      return;
    }
    this.persistCurrentMemberMeals();
    this.saveDayNow({ silent: true });
    const memberId = this.getMemberIdByKey(key);
    this.setData({ selectedMemberKey: key, selectedMemberId: memberId, familyDrawerOpen: false });
    if (this.data.serverReady && memberId) {
      this.loadDayFromServer(memberId, this.data.selectedDate);
    } else {
      this.applyMemberMeals(key, this.data.selectedDate);
    }
    try { wx.setStorageSync('nutrition_selected_member', key); } catch (e2) {}
  },

  applyMemberMeals(memberKey, date) {
    const key = String(memberKey || '');
    const payload = this.resolveLocalDayPayload(key, String(date || this.data.selectedDate || this.getTodayDate()))
    const normFoods = (arr) => (Array.isArray(arr) ? arr : []).map((f) => {
      const item = (f && typeof f === 'object') ? f : {};
      const c = Number(item.calorie);
      return { ...item, calorie: Number.isNaN(c) ? 0 : c };
    });
    const snackCards = (Array.isArray(payload.snackCards) ? payload.snackCards : []).map((c) => {
      const card = this.ensureSnackCardId(c);
      const foods = normFoods(card && Array.isArray(card.foods) ? card.foods : []);
      return { ...card, foods, level: Math.max(0, Number(card.level) || 0) };
    });
    const next = {
      breakfastFoods: normFoods(payload.breakfastFoods),
      lunchFoods: normFoods(payload.lunchFoods),
      dinnerFoods: normFoods(payload.dinnerFoods),
      snackCards
    }
    if (payload && typeof payload === 'object') {
      const calorieTarget = Number(payload.calorieTarget)
      if (!Number.isNaN(calorieTarget) && calorieTarget > 0) next.calorieTarget = calorieTarget
      if (payload.macroTargets && typeof payload.macroTargets === 'object') {
        next.macroTargets = {
          carb: Number(payload.macroTargets.carb) || this.data.macroTargets.carb,
          protein: Number(payload.macroTargets.protein) || this.data.macroTargets.protein,
          fat: Number(payload.macroTargets.fat) || this.data.macroTargets.fat,
          fiber: Number(payload.macroTargets.fiber) || this.data.macroTargets.fiber
        }
      }
    }
    this.setData(next, () => this.updateDailyMacros());
  },

  persistCurrentMemberMeals() {
    const key = String(this.data.selectedMemberKey || 'me');
    const date = String(this.data.selectedDate || this.getTodayDate())
    const payload = {
      breakfastFoods: Array.isArray(this.data.breakfastFoods) ? this.data.breakfastFoods : [],
      lunchFoods: Array.isArray(this.data.lunchFoods) ? this.data.lunchFoods : [],
      dinnerFoods: Array.isArray(this.data.dinnerFoods) ? this.data.dinnerFoods : [],
      snackCards: Array.isArray(this.data.snackCards) ? this.data.snackCards : [],
      calorieTarget: Number(this.data.calorieTarget) || 2000,
      macroTargets: this.data.macroTargets || { carb: 264, protein: 100, fat: 67, fiber: 25 }
    }
    this.upsertLocalDayPayload(key, date, payload)
  },

  showTextPromptModal({ title, placeholderText, confirmText } = {}) {
    return new Promise((resolve) => {
      wx.showModal({
        title: String(title || '请输入'),
        editable: true,
        placeholderText: String(placeholderText || ''),
        confirmText: String(confirmText || '确定'),
        success: (res) => {
          let confirmed = false
          if (res) {
            if (Object.prototype.hasOwnProperty.call(res, 'confirm')) confirmed = !!res.confirm
            else confirmed = !res.cancel
          }
          if (!confirmed) return resolve({ ok: false, value: '' });
          const candidates = [res.inputValue, res.content, res.value, res.text]
          let raw = ''
          for (const c of candidates) {
            if (c == null) continue
            const s = String(c).trim()
            if (s) {
              raw = c
              break
            }
          }
          resolve({ ok: true, value: String(raw == null ? '' : raw).trim() });
        },
        fail: () => resolve({ ok: false, value: '' })
      })
    })
  },

  pickRelation() {
    const list = ['爸爸', '妈妈', '老公', '老婆', '孩子', '家人', '其他']
    return new Promise((resolve) => {
      let finished = false
      const finish = (payload) => {
        if (finished) return
        finished = true
        clearTimeout(hardTimer)
        resolve(payload)
      }
      const hardTimer = setTimeout(() => finish({ ok: false, code: 'timeout', value: '' }), 2600)

      const tryOnce = (delayMs, attempt) => {
        setTimeout(() => {
          const startedAt = Date.now()
          try {
            wx.showActionSheet({
              itemList: list,
              success: (res) => {
                const idx = res && typeof res.tapIndex === 'number' ? res.tapIndex : -1
                finish({ ok: true, value: idx >= 0 && idx < list.length ? list[idx] : '' })
              },
              fail: (err) => {
                const msg = String((err && err.errMsg) || '')
                const elapsed = Date.now() - startedAt
                const isCancel = msg.includes('cancel')
                const isFast = elapsed < 150
                const cancelLooksLikePenetration = isCancel && elapsed < 800
                const shouldRetry = attempt < 2 && (isFast || cancelLooksLikePenetration || msg.includes('can not') || msg.includes('system'))
                if (shouldRetry) {
                  tryOnce(520 + attempt * 240, attempt + 1)
                  return
                }
                if (isCancel && !cancelLooksLikePenetration) {
                  finish({ ok: false, code: 'cancel', value: '' })
                  return
                }
                finish({ ok: false, code: 'fail', value: '' })
              }
            })
          } catch (e) {
            finish({ ok: false, code: 'exception', value: '' })
          }
        }, Math.max(0, Number(delayMs) || 0))
      }
      tryOnce(420, 0)
    })
  },

  async onAddMemberTap() {
    this.closeFamilyDrawer()
    if (!this.ensureCanAddMemberOrPrompt()) return

    let nickname = ''
    for (let i = 0; i < 3; i++) {
      const resp = await this.showTextPromptModal({ title: '新增成员', placeholderText: '请输入昵称（例如：爸爸）', confirmText: '下一步' })
      if (!resp || !resp.ok) return
      nickname = String(resp.value || '').trim()
      if (nickname) break
      wx.showToast({ title: '昵称不能为空', icon: 'none' })
    }
    nickname = String(nickname || '').trim()
    if (!nickname) return

    const rel = await this.pickRelation()
    let relation = rel && rel.ok ? String(rel.value || '').trim() : ''
    if (!relation) {
      if (rel && rel.code === 'cancel') return
      relation = '家人'
      wx.showToast({ title: '关系选择未弹出，已默认家人', icon: 'none' })
    }

    try {
      const body = await post('/nutrition/members', { nickname, relation }, { loading: true, showError: true })
      const member = body && body.data && typeof body.data === 'object' ? body.data : null
      const memberKey = String(member && member.memberKey ? member.memberKey : '').trim()
      if (memberKey) {
        this.setData({ selectedMemberKey: memberKey })
        try { wx.setStorageSync('nutrition_selected_member', memberKey); } catch (e) {}
      }
      await this.tryLoadServerMembersAndDay()
      wx.showToast({ title: '已添加成员', icon: 'success' })
      return
    } catch (e) {}

    const memberKey = `m_${Date.now()}_${Math.floor(Math.random() * 10000)}`
    const members = Array.isArray(this.data.familyMembers) ? this.data.familyMembers : []
    const next = members.filter((m) => m && m.type !== 'add')
    next.push({ key: memberKey, name: nickname, type: 'member' })
    next.push({ key: 'add', name: '＋', type: 'add' })
    const mealDataByMember = { ...(this.data.mealDataByMember || {}), [memberKey]: {} }
    this.setData({ familyMembers: next, mealDataByMember, selectedMemberKey: memberKey }, () => {
      this.saveLocalFamilyMembers()
      this.refreshFamilyAddLock()
    })
    wx.showToast({ title: '已添加成员', icon: 'success' })
  },

  onFamilyCellLongPress(e) {
    const key = e && e.currentTarget && e.currentTarget.dataset ? String(e.currentTarget.dataset.key || '') : ''
    const id = e && e.currentTarget && e.currentTarget.dataset ? Number(e.currentTarget.dataset.id) : null
    if (!key || key === 'me' || key === 'add') return
    this._familyCellTapBlockedUntil = Date.now() + 900
    setTimeout(() => {
      wx.showActionSheet({
        itemList: ['删除成员'],
        success: (res) => {
          if (!res || res.tapIndex !== 0) return
          this.confirmDeleteMember({ key, id })
        },
        fail: () => {}
      })
    }, 80)
  },

  onMemberDeleteTap(e) {
    const key = e && e.currentTarget && e.currentTarget.dataset ? String(e.currentTarget.dataset.key || '') : ''
    const rawId = e && e.currentTarget && e.currentTarget.dataset ? Number(e.currentTarget.dataset.id) : null
    const id = Number.isFinite(rawId) && rawId > 0 ? rawId : null
    if (!key || key === 'me' || key === 'add') return
    this._familyCellTapBlockedUntil = Date.now() + 900
    this.confirmDeleteMember({ key, id })
  },

  confirmDeleteMember({ key, id }) {
    wx.showModal({
      title: '删除成员',
      content: '删除后该成员的饮食记录也会一并删除，是否继续？',
      confirmText: '删除',
      confirmColor: '#ef4444',
      success: async (res) => {
        if (!res || !res.confirm) return
        await this.deleteMember({ key, id })
      }
    })
  },

  async deleteMember({ key, id }) {
    const memberKey = String(key || '')
    if (!memberKey || memberKey === 'me') return

    const idNum = Number(id)
    const passedId = Number.isFinite(idNum) && idNum > 0 ? idNum : null
    const mappedId = this.getMemberIdByKey(memberKey)
    const resolvedId = passedId || mappedId || null
    const keyToId = this.data.serverMemberKeyToId || {}
    const hasServerMapping = !!(keyToId && typeof keyToId === 'object' && keyToId[memberKey])

    if (!this.data.serverReady || !resolvedId || !hasServerMapping) {
      const members = Array.isArray(this.data.familyMembers) ? this.data.familyMembers : []
      const next = members.filter((m) => m && m.key !== memberKey)
      const mealDataByMember = { ...(this.data.mealDataByMember || {}) }
      delete mealDataByMember[memberKey]
      const selectedMemberKey = this.data.selectedMemberKey === memberKey ? 'me' : this.data.selectedMemberKey
      const selectedMemberId = this.getMemberIdByKey(selectedMemberKey)
      this.setData({ familyMembers: next, mealDataByMember, selectedMemberKey, selectedMemberId }, () => {
        this.saveLocalFamilyMembers()
        this.refreshFamilyAddLock()
        this.onSelectedDateChanged()
      })
      wx.showToast({ title: '已删除', icon: 'success' })
      return
    }

    try {
      await del(`/nutrition/members/${Number(resolvedId)}`, {}, { loading: true, showError: false })
      await this.tryLoadServerMembersAndDay()
      const selectedMemberKey = this.data.selectedMemberKey === memberKey ? 'me' : this.data.selectedMemberKey
      this.setData({ selectedMemberKey }, () => this.onSelectedDateChanged())
      wx.showToast({ title: '已删除', icon: 'success' })
    } catch (e) {
      const msg = String((e && e.data && (e.data.message || e.data.msg)) || (e && e.message) || '').trim()
      wx.showModal({ title: '删除失败', content: msg || '请稍后重试', showCancel: false })
    }
  },

  onSyncTodayTap() {
    wx.showToast({ title: '已同步今日饮食', icon: 'none' });
    this.closeFamilyDrawer();
  },

  noop() {},

  openAddFoodModal(e) {
    const mealKey = e && e.currentTarget && e.currentTarget.dataset ? String(e.currentTarget.dataset.meal || '') : '';
    const snackIndex = e && e.currentTarget && e.currentTarget.dataset ? Number(e.currentTarget.dataset.snackIndex) : -1;
    let target = null;
    if (mealKey === 'snack') {
      target = { type: 'snack', snackIndex };
    } else {
      target = { type: 'meal', mealKey: mealKey || 'breakfastFoods' };
    }
    this.setData({
      addFoodModalOpen: true,
      addFoodTarget: target,
      familyDrawerOpen: false
    });
  },

  closeAddFoodModal() {
    this.setData({ addFoodModalOpen: false });
  },

  getSnackFoods(snackIndex) {
    const i = Number(snackIndex);
    const cards = Array.isArray(this.data.snackCards) ? this.data.snackCards : [];
    const card = i >= 0 && i < cards.length ? cards[i] : null;
    const foods = card && Array.isArray(card.foods) ? card.foods : [];
    return { cards, card, foods, index: i };
  },

  ensureSnackCardId(card) {
    const c = card && typeof card === 'object' ? card : {};
    if (c.id) return c;
    return { ...c, id: `snack_${Date.now()}_${Math.floor(Math.random() * 10000)}` };
  },

  appendFoodToTarget(target, food) {
    const t = target && typeof target === 'object' ? target : null;
    if (!t) return false;
    const item = food && typeof food === 'object' ? food : null;
    if (!item) return false;
    const calorie = Number(item.calorie);
    const normalizedItem = { ...item, calorie: Number.isNaN(calorie) ? 0 : calorie };

    if (t.type === 'meal') {
      const mealKey = String(t.mealKey || 'breakfastFoods');
      const list = Array.isArray(this.data[mealKey]) ? this.data[mealKey] : [];
      const next = [normalizedItem, ...list];
      this.setData({ [mealKey]: next });
      this.persistCurrentMemberMeals();
      this.updateDailyMacros();
      this.scheduleSaveDay();
      return true;
    }

    if (t.type === 'snack') {
      const { cards, card, foods, index } = this.getSnackFoods(t.snackIndex);
      if (!card || index < 0) return false;
      if (foods.length >= 2) {
        wx.showToast({ title: '加餐最多添加两道菜', icon: 'none' });
        return false;
      }
      const nextFoods = [normalizedItem, ...foods];
      const nextCards = cards.map((c, idx) => (idx === index ? { ...c, foods: nextFoods } : c));
      this.setData({ snackCards: nextCards });
      this.persistCurrentMemberMeals();
      this.updateDailyMacros();
      this.scheduleSaveDay();
      return true;
    }

    return false;
  },

  addPlaceholderFoodFromImage(filePath, source) {
    const img = String(filePath || '');
    if (!img) return;
    const item = {
      name: source === 'camera' ? '拍照识别中…' : '相册识别中…',
      unit: '',
      nutrient: '待识别',
      img,
      calorie: 0
    };
    this.appendFoodToTarget(this.data.addFoodTarget, item);
  },

  onAddByCamera() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['camera'],
      success: (res) => {
        const filePath = res && res.tempFiles && res.tempFiles[0] ? res.tempFiles[0].tempFilePath : '';
        this.closeAddFoodModal();
        if (!filePath) return;
        this.recognizeFoodFromImage(filePath, 'camera');
      },
      fail: () => {}
    });
  },

  onAddByAlbum() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album'],
      success: (res) => {
        const filePath = res && res.tempFiles && res.tempFiles[0] ? res.tempFiles[0].tempFilePath : '';
        this.closeAddFoodModal();
        if (!filePath) return;
        this.recognizeFoodFromImage(filePath, 'album');
      },
      fail: () => {}
    });
  },

  onAddBySelect() {
    const target = this.data.addFoodTarget;
    this.closeAddFoodModal();
    const memberKey = String(this.data.selectedMemberKey || 'me')
    const memberId = this.getMemberIdByKey(memberKey) || this.data.selectedMemberId || ''
    const date = String(this.data.selectedDate || this.getTodayDate())
    wx.navigateTo({
      url: `/pages/nutritionbrochure/FoodRecord/FoodRecord?from=DataPage&memberId=${encodeURIComponent(String(memberId || ''))}&date=${encodeURIComponent(date)}&target=${encodeURIComponent(JSON.stringify(target || {}))}`,
      success: (res) => {
        const ec = res && res.eventChannel;
        if (!ec || typeof ec.on !== 'function') return;
        ec.on('foodSelected', (payload) => {
          const p = payload && typeof payload === 'object' ? payload : {};
          this.acceptFoodFromRecord(p.target || this.data.addFoodTarget || {}, p.food || null);
        });
      }
    });
  },

  recognizeFoodFromImage(filePath, source) {
    uploadFile('/food/recognize', filePath, { loadingText: '识别中...', name: 'file' })
      .then((resp) => {
        const items = (resp && resp.data && Array.isArray(resp.data.items)) ? resp.data.items : [];
        const top = items.length ? (items[0] || {}) : null;
        if (!top || !top.name) {
          wx.showToast({ title: '未识别到食物', icon: 'none' });
          this.addPlaceholderFoodFromImage(filePath, source);
          return;
        }
        const probability = Number(top.probability);
        const calorie = Number(top.calorie);
        const carb = Number(top.carb);
        const protein = Number(top.protein);
        const fat = Number(top.fat);
        let nutrient = '';
        const hasMacros = (!Number.isNaN(carb) && carb > 0) || (!Number.isNaN(protein) && protein > 0) || (!Number.isNaN(fat) && fat > 0);
        if (hasMacros) {
          const parts = [];
          if (!Number.isNaN(carb) && carb > 0) parts.push(`${Math.round(carb)}克碳水`);
          if (!Number.isNaN(protein) && protein > 0) parts.push(`${Math.round(protein)}克蛋白`);
          if (!Number.isNaN(fat) && fat > 0) parts.push(`${Math.round(fat)}克脂肪`);
          nutrient = parts.join(' ');
        } else if (!Number.isNaN(calorie) && calorie > 0) nutrient = `约${Math.round(calorie)}千卡`;
        else if (!Number.isNaN(probability) && probability > 0) nutrient = `可信度${Math.round(probability * 100)}%`;
        else nutrient = '待识别';

        const item = {
          name: String(top.name || '未知食物'),
          unit: '',
          nutrient,
          img: String(filePath || ''),
          calorie: (!Number.isNaN(calorie) && calorie > 0) ? Math.round(calorie) : 0,
          carb: (!Number.isNaN(carb) && carb > 0) ? carb : 0,
          protein: (!Number.isNaN(protein) && protein > 0) ? protein : 0,
          fat: (!Number.isNaN(fat) && fat > 0) ? fat : 0,
          fiber: 0
        };
        this.appendFoodToTarget(this.data.addFoodTarget, item);
      })
      .catch(() => {
        this.addPlaceholderFoodFromImage(filePath, source);
      });
  },

  parseMacrosFromText(text) {
    const s = String(text || '');
    const out = { carb: 0, protein: 0, fat: 0, fiber: 0 };
    const matches = s.matchAll(/(\d+(?:\.\d+)?)\s*克?\s*(碳水|蛋白|脂肪|纤维)/g);
    for (const m of matches) {
      const num = Number(m[1]) || 0;
      const type = m[2];
      if (type === '碳水') out.carb += num;
      if (type === '蛋白') out.protein += num;
      if (type === '脂肪') out.fat += num;
      if (type === '纤维') out.fiber += num;
    }
    return out;
  },

  sumMealMacros(list) {
    const arr = Array.isArray(list) ? list : [];
    return arr.reduce((acc, it) => {
      const a = it && typeof it === 'object' ? it : {};
      const carb = Number(a.carb) || 0;
      const protein = Number(a.protein) || 0;
      const fat = Number(a.fat) || 0;
      const fiber = Number(a.fiber) || 0;
      if (carb || protein || fat || fiber) {
        acc.carb += carb;
        acc.protein += protein;
        acc.fat += fat;
        acc.fiber += fiber;
        return acc;
      }
      const parsed = this.parseMacrosFromText(a.nutrient);
      acc.carb += parsed.carb;
      acc.protein += parsed.protein;
      acc.fat += parsed.fat;
      acc.fiber += parsed.fiber;
      return acc;
    }, { carb: 0, protein: 0, fat: 0, fiber: 0 });
  },

  sumMealCalories(list) {
    const arr = Array.isArray(list) ? list : [];
    return arr.reduce((acc, it) => {
      const a = it && typeof it === 'object' ? it : {};
      const c = Number(a.calorie);
      if (!Number.isNaN(c) && c > 0) return acc + c;
      return acc;
    }, 0);
  },

  updateDailyMacros() {
    const totals = { carb: 0, protein: 0, fat: 0, fiber: 0 };
    const b = this.sumMealMacros(this.data.breakfastFoods);
    const l = this.sumMealMacros(this.data.lunchFoods);
    const d = this.sumMealMacros(this.data.dinnerFoods);
    const snackCards = Array.isArray(this.data.snackCards) ? this.data.snackCards : [];
    const s = snackCards.reduce((acc, card) => {
      const foods = card && Array.isArray(card.foods) ? card.foods : [];
      const m = this.sumMealMacros(foods);
      acc.carb += m.carb;
      acc.protein += m.protein;
      acc.fat += m.fat;
      acc.fiber += m.fiber;
      return acc;
    }, { carb: 0, protein: 0, fat: 0, fiber: 0 });
    totals.carb = b.carb + l.carb + d.carb + s.carb;
    totals.protein = b.protein + l.protein + d.protein + s.protein;
    totals.fat = b.fat + l.fat + d.fat + s.fat;
    totals.fiber = b.fiber + l.fiber + d.fiber + s.fiber;

    const kcalB = this.sumMealCalories(this.data.breakfastFoods);
    const kcalL = this.sumMealCalories(this.data.lunchFoods);
    const kcalD = this.sumMealCalories(this.data.dinnerFoods);
    const kcalS = snackCards.reduce((acc, card) => {
      const foods = card && Array.isArray(card.foods) ? card.foods : [];
      return acc + this.sumMealCalories(foods);
    }, 0);
    const calorieConsumed = Math.round(kcalB + kcalL + kcalD + kcalS);
    this.setData({ calorieConsumed });

    const t = this.data.macroTargets || { carb: 264, protein: 100, fat: 67, fiber: 25 };
    const clampPct = (x) => Math.max(0, Math.min(100, Math.round(x)));
    const items = [
      { key: 'carb', name: '碳水', percent: clampPct((totals.carb / (Number(t.carb) || 1)) * 100), value: `${Math.round(totals.carb)}/${Number(t.carb) || 0}克` },
      { key: 'fat', name: '脂肪', percent: clampPct((totals.fat / (Number(t.fat) || 1)) * 100), value: `${Math.round(totals.fat)}/${Number(t.fat) || 0}克` },
      { key: 'protein', name: '蛋白', percent: clampPct((totals.protein / (Number(t.protein) || 1)) * 100), value: `${Math.round(totals.protein)}/${Number(t.protein) || 0}克` },
      { key: 'fiber', name: '纤维', percent: clampPct((totals.fiber / (Number(t.fiber) || 1)) * 100), value: `${Math.round(totals.fiber)}/${Number(t.fiber) || 0}克` }
    ];
    this.setData({ macroItems: items });

    const key = String(this.data.selectedMemberKey || 'me');
    let byMember = {};
    try { byMember = wx.getStorageSync('nutrition_daily_macros_by_member') || {}; } catch (e) { byMember = {}; }
    byMember[key] = {
      date: String(this.data.selectedDate || this.getTodayDate()),
      totals,
      targets: t,
      percent: { carb: items[0].percent, fat: items[1].percent, protein: items[2].percent, fiber: items[3].percent },
      calorieConsumed,
      calorieTarget: Number(this.data.calorieTarget) || 2000
    };
    try { wx.setStorageSync('nutrition_daily_macros_by_member', byMember); } catch (e2) {}

    this.updateOrganImpacts({
      memberKey: key,
      date: String(this.data.selectedDate || ''),
      totals,
      macroPercent: byMember[key].percent,
      calorieConsumed,
      calorieTarget: Number(this.data.calorieTarget) || 2000
    });
  },

  initCalendar() {
    const now = new Date();
    const y = now.getFullYear();
    const m = now.getMonth() + 1;
    const d = now.getDate();
    this.setMonth(y, m, d);
  },

  format2(n) {
    return String(n).padStart(2, '0');
  },

  setMonth(year, month, selectedDay = 1) {
    const y = Number(year);
    const m = Number(month);
    const daysInMonth = new Date(y, m, 0).getDate();
    const safeDay = Math.max(1, Math.min(daysInMonth, Number(selectedDay) || 1));
    const dateTopLineW = (safeDay - 1) * (110 + 18) + 110;
    const dateScrollLeft = this.calcDateScrollLeft(safeDay);
    const selectedDate = `${y}-${this.format2(m)}-${this.format2(safeDay)}`;
    const monthPickerValue = `${y}-${this.format2(m)}-01`;
    const currentMonthText = `${y}年${m}月`;

    const monthDays = [];
    for (let day = 1; day <= daysInMonth; day++) {
      const dt = new Date(y, m - 1, day);
      const week = this.data.weekDays[dt.getDay()];
      monthDays.push({
        id: `${y}-${m}-${day}`,
        day,
        week,
        selected: day === safeDay
      });
    }

    this.setData({
      currentMonthText,
      monthPickerValue,
      monthDays,
      selectedDate,
      dateScrollLeft,
      dateTopLineW
    }, () => {
      this.onSelectedDateChanged();
    });
  },

  calcDateScrollLeft(day) {
    const safeDay = Math.max(1, Number(day) || 1)
    const index = safeDay - 1
    try {
      const sys = wx.getSystemInfoSync ? wx.getSystemInfoSync() : null
      const windowWidthPx = sys && sys.windowWidth ? Number(sys.windowWidth) : 375
      const ratio = windowWidthPx / 750
      const itemW = 110 * ratio
      const gap = 18 * ratio
      const pad = 22 * ratio
      const center = pad + index * (itemW + gap) + itemW / 2
      return Math.max(0, Math.round(center - windowWidthPx / 2))
    } catch (e) {
      return 0
    }
  },

  onMonthPickerChange(e) {
    this.persistCurrentMemberMeals()
    this.saveDayNow({ silent: true })
    const raw = (e && e.detail && e.detail.value) ? String(e.detail.value) : '';
    const ym = raw.slice(0, 7);
    const m = ym.match(/^(\d{4})-(\d{2})$/);
    if (!m) return;
    const y = Number(m[1]);
    const month = Number(m[2]);
    this.setMonth(y, month, 1);
  },

  prevMonth() {
    this.persistCurrentMemberMeals()
    this.saveDayNow({ silent: true })
    const raw = String(this.data.monthPickerValue || '');
    const m = raw.match(/^(\d{4})-(\d{2})/);
    if (!m) return;
    let y = Number(m[1]);
    let mo = Number(m[2]);
    mo -= 1;
    if (mo <= 0) { mo = 12; y -= 1; }
    this.setMonth(y, mo, 1);
  },

  nextMonth() {
    this.persistCurrentMemberMeals()
    this.saveDayNow({ silent: true })
    const raw = String(this.data.monthPickerValue || '');
    const m = raw.match(/^(\d{4})-(\d{2})/);
    if (!m) return;
    let y = Number(m[1]);
    let mo = Number(m[2]);
    mo += 1;
    if (mo >= 13) { mo = 1; y += 1; }
    this.setMonth(y, mo, 1);
  },

  onSelectDay(e) {
    this.persistCurrentMemberMeals()
    this.saveDayNow({ silent: true })
    const day = Number(e && e.currentTarget && e.currentTarget.dataset ? e.currentTarget.dataset.day : 0);
    if (!day) return;
    const raw = String(this.data.monthPickerValue || '');
    const m = raw.match(/^(\d{4})-(\d{2})/);
    if (!m) return;
    const y = Number(m[1]);
    const mo = Number(m[2]);
    this.setMonth(y, mo, day);
  },

  onFoodTap(e) {
    const mealKey = e && e.currentTarget && e.currentTarget.dataset ? String(e.currentTarget.dataset.meal || '') : '';
    const index = e && e.currentTarget && e.currentTarget.dataset ? Number(e.currentTarget.dataset.index) : -1;
    const snackIndex = e && e.currentTarget && e.currentTarget.dataset ? Number(e.currentTarget.dataset.snackIndex) : -1;
    let list = [];
    let target = null;
    if (mealKey === 'snack') {
      const { foods } = this.getSnackFoods(snackIndex);
      list = foods;
      target = { type: 'snack', snackIndex, foodIndex: index };
    } else {
      list = (mealKey && Array.isArray(this.data[mealKey])) ? this.data[mealKey] : [];
      target = { type: 'meal', mealKey, foodIndex: index };
    }
    if (!list.length || index < 0 || index >= list.length) return;
    if (!target) return;
    this.setData({
      deleteConfirmOpen: true,
      deleteFoodTarget: target,
      addFoodModalOpen: false,
      familyDrawerOpen: false
    });
  },

  closeDeleteConfirm() {
    this.setData({ deleteConfirmOpen: false, deleteFoodTarget: null });
  },

  confirmDeleteFood() {
    const t = this.data.deleteFoodTarget;
    if (!t || typeof t !== 'object') return;
    const type = String(t.type || '');
    const foodIndex = Number(t.foodIndex);
    if (Number.isNaN(foodIndex) || foodIndex < 0) return;

    if (type === 'meal') {
      const mealKey = String(t.mealKey || '');
      const list = (mealKey && Array.isArray(this.data[mealKey])) ? this.data[mealKey] : [];
      if (!list.length || foodIndex >= list.length) return;
      const next = list.filter((_, idx) => idx !== foodIndex);
      this.setData({ [mealKey]: next, deleteConfirmOpen: false, deleteFoodTarget: null });
      this.persistCurrentMemberMeals();
      this.updateDailyMacros();
      this.scheduleSaveDay();
      wx.showToast({ title: '已删除', icon: 'success' });
      return;
    }

    if (type === 'snack') {
      const snackIndex = Number(t.snackIndex);
      const { cards, card, foods, index } = this.getSnackFoods(snackIndex);
      if (!card || index < 0) return;
      if (!foods.length || foodIndex >= foods.length) return;
      const nextFoods = foods.filter((_, idx) => idx !== foodIndex);
      const nextCards = cards.map((c, idx) => (idx === index ? { ...c, foods: nextFoods } : c));
      this.setData({ snackCards: nextCards, deleteConfirmOpen: false, deleteFoodTarget: null });
      this.persistCurrentMemberMeals();
      this.updateDailyMacros();
      this.scheduleSaveDay();
      wx.showToast({ title: '已删除', icon: 'success' });
    }
  },

  addSnackCard() {
    const cards = Array.isArray(this.data.snackCards) ? this.data.snackCards : [];
    const newCard = this.ensureSnackCardId({ foods: [], level: 0 });
    const next = [...cards, newCard];
    this.setData({ snackCards: next });
    this.persistCurrentMemberMeals();
    this.updateDailyMacros();
    this.scheduleSaveDay();
  },

  onSnackCardTap(e) {
    const index = e && e.currentTarget && e.currentTarget.dataset ? Number(e.currentTarget.dataset.index) : -1;
    const cards = Array.isArray(this.data.snackCards) ? this.data.snackCards : [];
    if (index < 0 || index >= cards.length) return;
    const next = cards.map((c, i) => {
      if (i !== index) return c;
      const level = Math.max(0, Math.min(6, (Number(c.level) || 0) + 1));
      return { ...c, level };
    });
    this.setData({ snackCards: next });
    this.persistCurrentMemberMeals();
    this.scheduleSaveDay();
  },

  acceptFoodFromRecord(target, food) {
    const normalizeTarget = (input) => {
      let t = input;
      if (typeof t === 'string') {
        const raw = String(t || '').trim();
        if (!raw) return null;
        const candidates = [raw];
        try { candidates.unshift(decodeURIComponent(raw)); } catch (e) {}
        for (let i = 0; i < candidates.length; i++) {
          try {
            const parsed = JSON.parse(candidates[i]);
            if (parsed && typeof parsed === 'object') {
              t = parsed;
              break;
            }
          } catch (e) {}
        }
      }
      if (!t || typeof t !== 'object') return null;
      const type = String(t.type || '').trim();
      if (type === 'meal') return { type: 'meal', mealKey: String(t.mealKey || 'breakfastFoods') };
      if (type === 'snack') return { type: 'snack', snackIndex: Number(t.snackIndex) };
      return null;
    };

    const effectiveTarget = normalizeTarget(target) || normalizeTarget(this.data.addFoodTarget);
    const item = food && typeof food === 'object' ? food : null;
    if (!effectiveTarget || !item) return;
    this.appendFoodToTarget(effectiveTarget, item);
  },

  getTodayDate() {
    const d = new Date();
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  },

  async updateOrganImpacts(payload) {
    const p = payload && typeof payload === 'object' ? payload : {};
    const memberKey = String(p.memberKey || this.data.selectedMemberKey || 'me');
    const memberId = this.getMemberIdByKey(memberKey) || this.data.selectedMemberId;
    const date = String(p.date || this.data.selectedDate || '');

    if (this.data.serverReady && memberId) {
      try {
        const body = await get('/nutrition/organ-impact', { memberId: Number(memberId), date }, { loading: false, showError: false });
        const data = body && body.data && typeof body.data === 'object' ? body.data : null;
        const impacts = data && Array.isArray(data.impacts) ? data.impacts : null;
        const profile = data && data.profile && typeof data.profile === 'object' ? data.profile : null;
        const impactByKey = data && data.impactByKey && typeof data.impactByKey === 'object' ? data.impactByKey : null;
        if (impacts && profile && impactByKey) {
          this.setData({ organImpacts: impacts });
          let store = {};
          try { store = wx.getStorageSync('nutrition_organ_impact_by_member') || {}; } catch (e) { store = {}; }
          const byMember = (store && typeof store === 'object') ? store : {};
          const nextMember = (byMember[memberKey] && typeof byMember[memberKey] === 'object') ? byMember[memberKey] : {};
          nextMember[date || this.getTodayDate()] = { profile, impactByKey, impacts };
          const next = { ...byMember, [memberKey]: nextMember };
          try { wx.setStorageSync('nutrition_organ_impact_by_member', next); } catch (e2) {}
          return;
        }
      } catch (e) {}
    }

    const d = date || this.getTodayDate();
    let store = {};
    try { store = wx.getStorageSync('nutrition_organ_impact_by_member') || {}; } catch (e) { store = {}; }
    const byMember = (store && typeof store === 'object') ? store : {};
    const node = (byMember[memberKey] && typeof byMember[memberKey] === 'object') ? byMember[memberKey] : null;
    const cached = node && node[d] && typeof node[d] === 'object' ? node[d] : null;
    const cachedImpacts = cached && Array.isArray(cached.impacts) ? cached.impacts : [];
    this.setData({ organImpacts: cachedImpacts });
  },

  goOrganImpactDetail(e) {
    const key = e && e.currentTarget && e.currentTarget.dataset ? String(e.currentTarget.dataset.key || '') : '';
    if (!key) return;
    const member = String(this.data.selectedMemberKey || 'me');
    const memberId = this.getMemberIdByKey(member) || this.data.selectedMemberId;
    const date = String(this.data.selectedDate || '');
    const idPart = memberId ? `&memberId=${encodeURIComponent(String(memberId))}` : '';
    wx.navigateTo({ url: `/pages/nutritionbrochure/BodyImpactDetail/BodyImpactDetail?key=${encodeURIComponent(key)}&member=${encodeURIComponent(member)}&date=${encodeURIComponent(date)}${idPart}` });
  }

});

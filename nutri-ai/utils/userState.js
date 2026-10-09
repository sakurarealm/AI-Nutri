const STATE_KEY = 'user_state_v1'

function load() {
  try {
    const raw = wx.getStorageSync(STATE_KEY)
    return (raw && typeof raw === 'object') ? raw : {}
  } catch (e) {
    return {}
  }
}

function save(state) {
  try {
    wx.setStorageSync(STATE_KEY, Object.assign({ ts: Date.now() }, state))
  } catch (e) {}
}

function get(key, fallback) {
  const state = load()
  return state[key] !== undefined ? state[key] : fallback
}

function set(key, value) {
  const state = load()
  state[key] = value
  save(state)
}

function merge(partial) {
  const state = load()
  save(Object.assign({}, state, partial))
}

function getMany(keys) {
  const state = load()
  const result = {}
  keys.forEach((k) => { result[k] = state[k] })
  return result
}

function setMany(obj) {
  merge(obj)
}

function mapUser(raw) {
  const u = raw || {}
  return {
    id: u.id ?? u.userId ?? u.uid ?? '-',
    nickname: u.nickname ?? u.username ?? u.mobile ?? '禾康用户',
    avatar: u.avatar ?? u.avatarUrl ?? u.avatar_url ?? u.headImg ?? '/images/default_avatar.svg'
  }
}

function mapAssets(raw) {
  const a = raw || {}
  const k = a.balanceKc ?? a.kCoins ?? a.balance ?? a.kc ?? 0
  const by = a.balanceYuan ?? a.balanceCny ?? a.cnyBalance ?? a.balanceMoney ?? a.balanceRmb ?? null
  const n = by == null ? NaN : Number(by)
  return { kCoins: Number(k) || 0, balanceYuan: isNaN(n) ? '0.00' : n.toFixed(2) }
}

function mapVipInfo(raw) {
  const v = raw || {}
  const level = Number(v.level ?? v.vipLevel ?? 0) || 0
  const planId = String(v.planId ?? v.plan ?? v.planType ?? '').trim()
  let levelName = String(v.levelName ?? '').trim()
  if (!levelName) {
    if (planId === 'basic') levelName = '基础版'
    else if (planId === 'pro') levelName = '专业版'
    else if (level >= 2) levelName = '专业版'
    else if (level === 1) levelName = '基础版'
    else levelName = '普通会员'
  }
  return {
    level,
    planId,
    levelName,
    expireDate: v.expireDate ?? v.expireTime ?? v.expire ?? null,
    benefits: Array.isArray(v.benefits) ? v.benefits : [],
    familySeats: Number(v.familySeats ?? v.familyCount ?? 0) || 0
  }
}

function mapHealthOverview(raw) {
  const d = raw || {}
  const days = d.streakDays ?? d.continuousDays ?? d.streak ?? 0
  const levelLabel = d.levelName ?? d.healthLevel ?? (d.level != null ? `Lv.${d.level}` : 'Lv.1')
  const done = d.weeklyCompleted ?? d.weeklyDone ?? d.weeklyGoalsCompleted ?? (d.weeklyGoals && d.weeklyGoals.done) ?? 0
  const total = d.weeklyTarget ?? (d.weeklyGoals && d.weeklyGoals.target) ?? 7
  return {
    continuousDays: Number(days) || 0,
    healthLevel: String(levelLabel),
    weeklyGoals: `${done}/${total}`,
    totalRecords: Number(d.totalRecords ?? d.recordCount ?? 0) || 0
  }
}

function computeVipIcons(vipInfo) {
  const level = Number(vipInfo?.level) || 0
  const planId = String(vipInfo?.planId || '').trim()
  const isPro = planId === 'pro' || level >= 2
  const isBasic = planId === 'basic' || level === 1
  return {
    vipIconSrc: isPro ? '/static/profile/125_VIP.png' : isBasic ? '/static/profile/124_VIP.png' : '/static/profile/level.png',
    vipBadgeSrc: isPro ? '/static/profile/pro.png' : isBasic ? '/static/profile/pro.png' : ''
  }
}

module.exports = {
  load,
  save,
  get,
  set,
  merge,
  getMany,
  setMany,
  mapUser,
  mapAssets,
  mapVipInfo,
  mapHealthOverview,
  computeVipIcons
}

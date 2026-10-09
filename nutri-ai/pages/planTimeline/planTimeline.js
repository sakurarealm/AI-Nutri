const request = require('../../utils/request.js')

function toDateText(d) {
  const raw = String(d || '').trim()
  if (!raw) return ''
  return raw.replace(/-/g, '.')
}

function kindLabel(kind) {
  const k = String(kind || '').trim()
  if (k === 'REMINDER_SCHEDULE') return '提醒'
  if (k === 'PLAN_REMINDER') return '计划'
  if (k === 'SUMMARY') return '总结'
  return ''
}

function statusView(kind, status) {
  const k = String(kind || '').trim()
  const s = String(status || '').trim().toUpperCase()
  if (k === 'SUMMARY') {
    if (s === 'DONE') return { label: '已完成', cls: 'status-done' }
    return { label: '待生成', cls: 'status-scheduled' }
  }
  if (s === 'COMPLETED') return { label: '已完成', cls: 'status-done' }
  if (s === 'EXECUTED') return { label: '已提醒', cls: 'status-executed' }
  if (s === 'EXPIRED') return { label: '已过期', cls: 'status-disabled' }
  if (s === 'PENDING') return { label: '待执行', cls: 'status-scheduled' }
  if (s === 'DISABLED') return { label: '已关闭', cls: 'status-disabled' }
  if (s === 'SKIPPED') return { label: '已跳过', cls: 'status-disabled' }
  if (s === 'SCHEDULED' || s === 'PLANNED') return { label: '待执行', cls: 'status-scheduled' }
  return { label: s || '待执行', cls: 'status-scheduled' }
}

function buildPlanSummary(plan) {
  const p = plan && typeof plan === 'object' ? plan : null
  if (!p) return []
  const out = []
  const toText = (value) => {
    if (value == null) return ''
    if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') return String(value).trim()
    if (Array.isArray(value)) return value.map((x) => String(x == null ? '' : x).trim()).filter(Boolean).join('、')
    if (typeof value === 'object') {
      const title = String(value.title == null ? '' : value.title).trim()
      const rules = Array.isArray(value.rules) ? value.rules : []
      if (rules.length) return rules.slice(0, 2).map((x) => String(x == null ? '' : x).trim()).filter(Boolean).join('；')
      const intensityHint = String(value.intensityHint == null ? '' : value.intensityHint).trim()
      if (title && intensityHint) return `${title}（${intensityHint}）`
      return title
    }
    return ''
  }
  const pick = (label, value) => {
    const v = toText(value)
    if (!v) return
    out.push(`${label}：${v}`)
  }
  pick('早餐', p.breakfast)
  pick('午餐', p.lunch)
  pick('晚餐', p.dinner)
  pick('运动', p.exercise)
  return out.slice(0, 6)
}

Page({
  data: {
    dateText: '',
    planSummary: [],
    items: [],
    refresherTriggered: false
  },

  onSummaryChipTap(e) {
    const text = e && e.currentTarget && e.currentTarget.dataset ? String(e.currentTarget.dataset.text || '').trim() : ''
    if (!text) return
    wx.setClipboardData({
      data: text,
      success: () => {
        try { wx.showToast({ title: '已复制', icon: 'success' }) } catch (e2) {}
      }
    })
  },

  onLoad() {
    this.reload({ silent: true })
  },

  onShow() {
    this.reload({ silent: true })
  },

  onPullDownRefresh() {
    this.setData({ refresherTriggered: true })
    this.reload().finally(() => {
      this.setData({ refresherTriggered: false })
      try { wx.stopPullDownRefresh() } catch (e) {}
    })
  },

  async reload(opts = {}) {
    try {
      const resp = await request.get('/plan/today', {}, { loading: !opts.silent, showError: !opts.silent })
      const payload = (resp && resp.data) ? resp.data : (resp || {})
      const date = String(payload.date || '').trim()
      const plan = payload.plan || null
      const rawItems = Array.isArray(payload.items) ? payload.items : []
      const items = rawItems.map((it, idx) => {
        const kind = String(it.kind || '').trim()
        const decisionKey = String(it.decisionKey || '').trim()
        const scheduleKey = String(it.scheduleKey || '').trim()
        const time = String(it.time || '').trim()
        const title = String(it.title || '').trim()
        const subTitle = String(it.itemKey || '').trim()
        const view = statusView(kind, it.status)
        const canComplete = !!it.canComplete
        const canSkipToday = !!it.canSkipToday
        const canDisable = !!it.canDisable
        const showActions = !!decisionKey && (canComplete || canSkipToday || canDisable)

        return {
          rowKey: `${kind}-${decisionKey || scheduleKey || idx}`,
          kind,
          kindLabel: kindLabel(kind),
          time,
          title,
          subTitle,
          status: String(it.status || '').trim(),
          statusLabel: view.label,
          statusClass: view.cls,
          decisionKey,
          scheduleKey,
          showActions,
          showComplete: canComplete,
          showSkipToday: canSkipToday,
          showDisable: canDisable
        }
      })

      this.setData({
        dateText: toDateText(date) || toDateText(new Date().toISOString().slice(0, 10)),
        planSummary: buildPlanSummary(plan),
        items
      })
    } catch (e) {
      if (!opts.silent) {
        try { wx.showToast({ title: '加载失败', icon: 'none' }) } catch (e2) {}
      }
    }
  },

  async onComplete(e) {
    const decisionKey = e && e.currentTarget && e.currentTarget.dataset ? String(e.currentTarget.dataset.decisionkey || '') : ''
    if (!decisionKey) return
    try {
      await request.post('/reminder/complete', { decisionKey }, { loading: true, showError: true })
      this.updateItem(decisionKey, { status: 'COMPLETED' })
      try { wx.showToast({ title: '已记录', icon: 'success' }) } catch (e2) {}
    } catch (e) {}
  },

  async onSkipToday(e) {
    const decisionKey = e && e.currentTarget && e.currentTarget.dataset ? String(e.currentTarget.dataset.decisionkey || '') : ''
    if (!decisionKey) return
    try {
      await request.post('/reminder/skip-today', { decisionKey }, { loading: true, showError: true })
      this.updateItem(decisionKey, { status: 'SKIPPED' })
      try { wx.showToast({ title: '已跳过', icon: 'success' }) } catch (e2) {}
    } catch (e) {}
  },

  onDisable(e) {
    const decisionKey = e && e.currentTarget && e.currentTarget.dataset ? String(e.currentTarget.dataset.decisionkey || '') : ''
    if (!decisionKey) return
    wx.showModal({
      title: '关闭提醒',
      content: '确认关闭这条后续每天提醒？',
      confirmText: '关闭',
      confirmColor: '#ef4444',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await request.post('/reminder/schedule/disable', { decisionKey }, { loading: true, showError: true })
          this.updateItem(decisionKey, { status: 'DISABLED' })
          try { wx.showToast({ title: '已关闭', icon: 'success' }) } catch (e2) {}
        } catch (e) {}
      }
    })
  },

  updateItem(decisionKey, patch) {
    const dk = String(decisionKey || '').trim()
    if (!dk) return
    const list = Array.isArray(this.data.items) ? this.data.items : []
    const next = list.map((it) => {
      if (String(it.decisionKey || '') !== dk) return it
      const merged = { ...it, ...(patch || {}) }
      const view = statusView(merged.kind, merged.status)
      const up = String(merged.status || '').toUpperCase()
      const isDone = up === 'COMPLETED'
      const isDisabled = ['DISABLED', 'SKIPPED'].includes(String(merged.status || '').toUpperCase())
      const showComplete = merged.showComplete && !isDone && !isDisabled
      const showSkipToday = merged.showSkipToday && !isDone && !isDisabled
      const showDisable = merged.showDisable && !isDone && !isDisabled
      return {
        ...merged,
        statusLabel: view.label,
        statusClass: view.cls,
        showComplete,
        showSkipToday,
        showDisable,
        showActions: !!merged.decisionKey && (showComplete || showSkipToday || showDisable)
      }
    })
    this.setData({ items: next })
  }
})

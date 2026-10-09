const DAILY_SUMMARY_ARCHIVE_KEY = 'DAILY_SUMMARY_ARCHIVE_V1'
const request = require('../../utils/request.js')

Page({
  data: {
    tabIndex: 0,
    contentHeightPx: 0,
    safeBottomPx: 0,
    pickedDate: '',
    scrollIntoView: '',
    archiveItems: [],
    archiveCount: 0,
    detailOpen: false,
    detailItem: { date: '', text: '' }
  },

  onLoad() {
    try {
      const info = wx.getSystemInfoSync();
      const windowHeight = typeof info.windowHeight === 'number' ? info.windowHeight : 0;
      const safeBottomPx =
        info.safeArea && typeof info.safeArea.bottom === 'number'
          ? Math.max(0, Math.round(windowHeight - info.safeArea.bottom))
          : 0;
      this.setData({ contentHeightPx: windowHeight, safeBottomPx });
    } catch (e) {}
  },

  onReady() {
    this.recalcLayout();
    this._onResize = res => {
      const h = res && res.size && typeof res.size.windowHeight === 'number' ? res.size.windowHeight : undefined;
      this.recalcLayout(h);
    };
    wx.onWindowResize && wx.onWindowResize(this._onResize);
  },

  onShow() {
    this.syncTodaySummary().finally(() => {
      this.loadArchive();
    });
  },

  onUnload() {
    wx.offWindowResize && wx.offWindowResize(this._onResize);
  },

  noop() {},

  recalcLayout(windowHeightOverride) {
    let windowHeight = windowHeightOverride;
    let safeBottomPx = 0;

    try {
      const info = wx.getSystemInfoSync();
      if (typeof windowHeight !== 'number') windowHeight = info.windowHeight;
      if (info.safeArea && typeof info.safeArea.bottom === 'number' && typeof windowHeight === 'number') {
        safeBottomPx = Math.max(0, Math.round(windowHeight - info.safeArea.bottom));
      }
    } catch (e) {}

    const query = wx.createSelectorQuery().in(this);
    query.select('.tabs').boundingClientRect();
    query.exec(res => {
      const rect = res && res[0];
      const tabsHeightPx = rect && typeof rect.height === 'number' ? rect.height : 0;
      const contentHeightPx =
        typeof windowHeight === 'number' ? Math.max(0, Math.round(windowHeight - tabsHeightPx)) : 0;
      this.setData({ contentHeightPx, safeBottomPx });
    });
  },

  getNumericUserId() {
    let raw = {};
    try {
      raw = wx.getStorageSync('user_cache') || wx.getStorageSync('userProfile') || wx.getStorageSync('userInfo') || {};
    } catch (e) {
      raw = {};
    }
    const id = (raw && (raw.id || raw.userId || raw.uid) != null) ? String(raw.id || raw.userId || raw.uid).trim() : '';
    return /^\d+$/.test(id) ? id : '';
  },

  getTodayDate() {
    return new Date().toISOString().slice(0, 10)
  },

  async syncTodaySummary() {
    const userId = this.getNumericUserId();
    if (!userId) return;
    try {
      const resp = await request.get('/summary/today', {}, { loading: false, showError: false })
      const payload = (resp && resp.data) ? resp.data : (resp || {})
      const exists = !!payload.exists
      const date = String(payload.date || this.getTodayDate()).trim()
      const text = String(payload.userText || '').trim()
      if (!exists || !date || !text) return
      this.upsertArchive(userId, date, text)
    } catch (e) {
      try {
        const now = new Date()
        const h = now.getHours()
        if (h >= 22) {
          const date = this.getTodayDate()
          const resp = await request.post('/summary/run', { date, trigger: 'client_open_daily_summary' }, { loading: false, showError: false })
          const payload = (resp && resp.data) ? resp.data : (resp || {})
          const text = String(payload.userText || '').trim()
          if (date && text) this.upsertArchive(userId, date, text)
        }
      } catch (e2) {}
    }
  },

  upsertArchive(userId, date, text) {
    const uid = String(userId || '').trim()
    const d = String(date || '').trim()
    const t = String(text || '').trim()
    if (!uid || !d || !t) return

    let store = {}
    try { store = wx.getStorageSync(DAILY_SUMMARY_ARCHIVE_KEY) || {} } catch (e) { store = {} }
    if (!store || typeof store !== 'object' || Array.isArray(store)) store = {}
    const userStore = (store[uid] && typeof store[uid] === 'object' && !Array.isArray(store[uid])) ? store[uid] : {}
    const prev = userStore[d] || null
    const prevTs = prev ? Number(prev.ts || 0) : 0
    const nextTs = Date.now()
    if (!prev || !String(prev.text || '').trim() || nextTs >= prevTs) {
      userStore[d] = {
        userId: uid,
        date: d,
        text: t,
        createdAt: new Date().toISOString().slice(0, 19).replace('T', ' '),
        ts: nextTs,
        updatedAt: nextTs
      }
      store[uid] = userStore
      try { wx.setStorageSync(DAILY_SUMMARY_ARCHIVE_KEY, store) } catch (e) {}
    }
  },

  loadArchive() {
    const userId = this.getNumericUserId();
    let store = {};
    try { store = wx.getStorageSync(DAILY_SUMMARY_ARCHIVE_KEY) || {}; } catch (e) { store = {}; }
    const userStore = (store && userId && store[userId] && typeof store[userId] === 'object' && !Array.isArray(store[userId]))
      ? store[userId]
      : {};

    const list = Object.keys(userStore).map((date) => {
      const rec = userStore[date] || {};
      const text = String(rec.text || '').trim();
      const createdAt = String(rec.createdAt || '').trim();
      const ts = Number(rec.ts || 0);
      return {
        date: String(date || '').trim(),
        text,
        createdAt,
        ts: Number.isFinite(ts) ? ts : 0
      };
    }).filter((it) => it.date && it.text);

    list.sort((a, b) => {
      const ad = String(a.date || '');
      const bd = String(b.date || '');
      if (ad !== bd) return bd.localeCompare(ad);
      return Number(b.ts || 0) - Number(a.ts || 0);
    });

    let lastMonth = '';
    const mapped = list.map((it) => {
      const monthKey = it.date.slice(0, 7);
      const showMonthHeader = monthKey !== lastMonth;
      lastMonth = monthKey;
      const day = it.date.slice(8, 10);
      const weekday = this.toCnWeekday(this.dateToWeekday(it.date));
      const timeText = this.pickTimeText(it.createdAt, it.ts);
      const preview = this.buildPreview(it.text);
      return {
        ...it,
        monthTitle: `${monthKey.replace('-', '年')}月`,
        showMonthHeader,
        day,
        weekday,
        timeText,
        preview
      };
    });

    this.setData({ archiveItems: mapped, archiveCount: mapped.length });
  },

  buildPreview(text) {
    const s = String(text || '').trim();
    if (!s) return '';
    const compact = s.replace(/\n{3,}/g, '\n\n').trim();
    const lines = compact.split('\n').map(x => String(x || '').trim()).filter(Boolean);
    const joined = lines.slice(0, 4).join(' · ');
    if (joined.length <= 140) return joined;
    return joined.slice(0, 140) + '…';
  },

  pickTimeText(createdAt, ts) {
    const s = String(createdAt || '').trim();
    if (s.length >= 16) return s.slice(11, 16);
    if (Number.isFinite(Number(ts)) && Number(ts) > 0) {
      const d = new Date(Number(ts));
      const hh = String(d.getHours()).padStart(2, '0');
      const mm = String(d.getMinutes()).padStart(2, '0');
      return `${hh}:${mm}`;
    }
    return '22:00';
  },

  dateToWeekday(dateStr) {
    const s = String(dateStr || '').trim();
    const m = s.match(/^(\d{4})-(\d{2})-(\d{2})$/);
    if (!m) return null;
    const y = Number(m[1]);
    const mo = Number(m[2]) - 1;
    const d = Number(m[3]);
    const dt = new Date(y, mo, d);
    const idx = dt.getDay();
    return Number.isFinite(idx) ? idx : null;
  },

  toCnWeekday(idx) {
    const i = Number(idx);
    if (!Number.isFinite(i)) return '';
    const map = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
    return map[i] || '';
  },

  onTabTap(e) {
    const idx = Number(e.currentTarget.dataset.index);
    if (!isNaN(idx)) this.setData({ tabIndex: idx });
  },

  onSwiperChange(e) {
    const idx = Number(e.detail.current);
    if (!isNaN(idx)) this.setData({ tabIndex: idx });
  },

  onDatePick(e) {
    const date = e && e.detail ? String(e.detail.value || '').trim() : '';
    if (!date) return;
    const id = `d-${date}`;
    this.setData({ pickedDate: date, scrollIntoView: '' });
    setTimeout(() => this.setData({ scrollIntoView: id }), 16);
  },

  onResetPick() {
    this.setData({ pickedDate: '', scrollIntoView: '' });
  },

  onCardTap(e) {
    const date = e && e.currentTarget && e.currentTarget.dataset ? String(e.currentTarget.dataset.date || '').trim() : '';
    if (!date) return;
    const list = Array.isArray(this.data.archiveItems) ? this.data.archiveItems : [];
    const it = list.find(x => x && String(x.date) === date) || null;
    if (!it) return;
    this.setData({ detailOpen: true, detailItem: { date: it.date, text: it.text } });
  },

  onDetailClose() {
    this.setData({ detailOpen: false });
    setTimeout(() => this.setData({ detailItem: { date: '', text: '' } }), 220);
  },

  onDetailCopy() {
    const text = String(this.data.detailItem && this.data.detailItem.text ? this.data.detailItem.text : '').trim();
    if (!text) return;
    wx.setClipboardData({ data: text });
  }
});

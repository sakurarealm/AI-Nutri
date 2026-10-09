// app.js
const DEFAULT_API_BASE_URL = 'http://localhost:9192 '//https://api.ai-nutri.com 
                                                        //http://localhost:9192
App({
  onLaunch() {
    const logs = wx.getStorageSync('logs') || []
    logs.unshift(Date.now())
    wx.setStorageSync('logs', logs)

    try {
      const networkMonitor = require('./utils/networkMonitor.js')
      networkMonitor.start()
    } catch (e) {}

    try {
      const env = String(wx.getStorageSync('CLOUDBASE_ENV_ID') || '').trim()
      const useCallContainer = String(wx.getStorageSync('USE_CALLCONTAINER') || '').trim()
      const enabled = useCallContainer === '1' || useCallContainer === 'true'
      if (enabled && env && wx.cloud && typeof wx.cloud.init === 'function') {
        wx.cloud.init({ env })
      }
      this.globalData.cloudEnvId = env
      this.globalData.useCallContainer = enabled
    } catch (e) {}

    try {
      const svc = String(wx.getStorageSync('CLOUDBASE_SERVICE') || '').trim()
      this.globalData.cloudService = svc
    } catch (e) {}

    try {
      const raw = String(wx.getStorageSync('API_BASE_URL') || '').trim()
      const normalized = this.normalizeApiHost(raw)
      if (normalized) this.globalData.apiBaseUrl = normalized
    } catch (e) {}

    this.globalData.notification = { message: '', userName: '', visible: false, queue: [] }
    this.eventListeners = {}
    try {
      if (!this.__pageWrapped && typeof Page === 'function') {
        const OriginalPage = Page
        const app = this
        Page = function (options) {
          const o = options || {}
          const originalOnShow = o.onShow
          o.onShow = function (...args) {
            try { app.runLoginGuard() } catch (e) {}
            if (typeof originalOnShow === 'function') return originalOnShow.apply(this, args)
          }
          return OriginalPage(o)
        }
        this.__pageWrapped = true
      }
    } catch (e) {}
    try { this.ensureAuth({ silent: true }) } catch (e) {}
  },
  
  onError(msg) {
    console.log('小程序错误', msg)
  },
  
  globalData: {
    userInfo: null,
    loginPromptShown: false,
    proactiveAutoTimer: null,
    notification: null,
    aiInboxPoller: null,
    apiBaseUrl: DEFAULT_API_BASE_URL,
    cloudEnvId: '',
    cloudService: '',
    useCallContainer: false
  },

  onShow() {
    this.startAiInboxPolling();
    this.fetchAiInboxOnce({ silent: true });
    try {
      this.runLoginGuard()
    } catch (e) {}
  },

  onHide() {
    this.stopAiInboxPolling();
  },

  showNotification(messageOrPayload, userName) {
    const pushed = (messageOrPayload && typeof messageOrPayload === 'object')
      ? this.enqueueAiInboxPayload(messageOrPayload)
      : this.enqueueAiInboxMessage(messageOrPayload, userName);
    if (!pushed) return;
    try {
      const pages = getCurrentPages();
      const current = pages && pages[pages.length - 1];
      const route = current && current.route ? String(current.route) : '';
      if (route.indexOf('pages/aiAgent/aiAgent') !== -1) {
        return pushed;
      }
    } catch (e) {}
    const n = (this.globalData && this.globalData.notification) ? this.globalData.notification : { message: '', userName: '', visible: false, queue: [] };
    if (!n || !Array.isArray(n.queue)) {
      this.globalData.notification = { message: '', userName: '', visible: false, queue: [] };
      return pushed;
    }
    const t = String(pushed.systemMessageType || '').trim();
    const sticky = t === 'REMINDER' || t === 'SUMMARY';
    n.queue.push({
      id: pushed.id,
      message: pushed.message,
      userName: pushed.userName,
      deepLink: pushed.deepLink || '',
      systemMessageType: t,
      sticky
    })
    this.processNotificationQueue()
  },

  processNotificationQueue() {
    const n = (this.globalData && this.globalData.notification) ? this.globalData.notification : null
    if (!n || !Array.isArray(n.queue)) return
    if (n.queue.length === 0) return
    if (n.visible && n.state === 'expanded') return
    try {
      const pages = getCurrentPages();
      const current = pages && pages[pages.length - 1];
      const route = current && current.route ? String(current.route) : '';
      if (route.indexOf('pages/aiAgent/aiAgent') !== -1) return
    } catch (e) {}
    const item = n.queue.shift()
    n.id = item.id
    n.message = item.message
    n.userName = item.userName
    n.deepLink = item.deepLink || ''
    n.systemMessageType = item.systemMessageType || ''
    n.sticky = !!item.sticky
    n.visible = true
    n.state = 'expanded'
    n.token = Date.now()
    this.globalData.notification = { ...n }
    this.triggerEvent('notificationUpdate')
    if (this.globalData.proactiveAutoTimer) {
      clearTimeout(this.globalData.proactiveAutoTimer)
      this.globalData.proactiveAutoTimer = null
    }
    if (!n.sticky) {
      const displayTime = Math.max(8000, Math.min(String(n.message).length * 120, 15000))
      this.globalData.proactiveAutoTimer = setTimeout(() => this.setNotificationState('collapsed'), displayTime)
    }
  },

  hideNotification() {
    const n = (this.globalData && this.globalData.notification) ? this.globalData.notification : null
    if (!n) return
    n.visible = false
    n.state = 'hidden'
    if (Array.isArray(n.queue)) n.queue = []
    n.sticky = false
    this.globalData.notification = { ...n }
    if (this.globalData.proactiveAutoTimer) {
      clearTimeout(this.globalData.proactiveAutoTimer)
      this.globalData.proactiveAutoTimer = null
    }
    this.triggerEvent('notificationUpdate')
    this.processNotificationQueue()
  },

  setNotificationState(state) {
    const n = (this.globalData && this.globalData.notification) ? this.globalData.notification : null
    if (!n) return
    n.state = state
    n.visible = state !== 'hidden'
    this.globalData.notification = { ...n }
    this.triggerEvent('notificationUpdate')
    if (state === 'collapsed') {
      setTimeout(() => this.processNotificationQueue(), 200)
    }
  },

  triggerEvent(eventName, data) {
    const list = this.eventListeners[eventName] || []
    list.forEach(cb => { try { cb(data) } catch (e) {} })
  },

  onEvent(eventName, callback) {
    if (!this.eventListeners[eventName]) this.eventListeners[eventName] = []
    this.eventListeners[eventName].push(callback)
  },

  offEvent(eventName, callback) {
    const list = this.eventListeners[eventName] || []
    this.eventListeners[eventName] = list.filter(cb => cb !== callback)
  },

  getAiInbox() {
    try {
      const v = wx.getStorageSync('AI_CHAT_INBOX');
      return Array.isArray(v) ? v : [];
    } catch (e) {
      return [];
    }
  },

  setAuthToken(token) {
    const t = String(token || '').trim();
    if (!t || t === 'undefined' || t === 'null') return;
    try { wx.setStorageSync('token', t); } catch (e) {}
    try { wx.setStorageSync('AUTH_TOKEN', t); } catch (e) {}
  },

  setRefreshToken(token) {
    const t = String(token || '').trim();
    if (!t || t === 'undefined' || t === 'null') return;
    try { wx.setStorageSync('REFRESH_TOKEN', t); } catch (e) {}
    try { wx.setStorageSync('refreshToken', t); } catch (e) {}
  },

  getAuthToken() {
    let token = '';
    try {
      token = wx.getStorageSync('token') || wx.getStorageSync('AUTH_TOKEN') || '';
    } catch (e) {}
    token = String(token || '').trim();
    if (!token || token === 'undefined' || token === 'null' || token.length < 8) return '';
    try { wx.setStorageSync('token', token); } catch (e) {}
    try { wx.setStorageSync('AUTH_TOKEN', token); } catch (e) {}
    return token;
  },

  getRefreshToken() {
    let token = '';
    try {
      token = wx.getStorageSync('REFRESH_TOKEN') || wx.getStorageSync('refreshToken') || '';
    } catch (e) {}
    token = String(token || '').trim();
    if (!token || token === 'undefined' || token === 'null' || token.length < 8) return '';
    try { wx.setStorageSync('REFRESH_TOKEN', token); } catch (e) {}
    try { wx.setStorageSync('refreshToken', token); } catch (e) {}
    return token;
  },

  isLoggedIn() {
    try {
      const info = wx.getStorageSync('userInfo') || null
      return !!this.getAuthToken() || (!!info && typeof info === 'object' && !!info.isLoggedIn)
    } catch (e) {
      return !!this.getAuthToken()
    }
  },

  isPublicRoute(route) {
    const r = String(route || '').trim().replace(/^\/+/, '')
    if (!r) return true
    // 登录相关页面
    if (r.indexOf('pages/login/login') === 0) return true
    if (r.indexOf('pages/rules/rules') === 0) return true
    // 主要功能页面 - 允许未登录访问
    if (r.indexOf('pages/index/index') === 0) return true
    if (r.indexOf('pages/aiAgent/aiAgent') === 0) return true
    if (r.indexOf('pages/nutritionbrochure/nutritionbrochure') === 0) return true
    if (r.indexOf('pages/shop/shop') === 0) return true
    if (r.indexOf('pages/digitalHuman/digitalHuman') === 0) return true
    // 营养册子页面
    if (r.indexOf('pages/nutritionbrochure/BodyImpactDetail/BodyImpactDetail') === 0) return true
    if (r.indexOf('pages/nutritionbrochure/DataPage/DataPage') === 0) return true
    if (r.indexOf('pages/nutritionbrochure/FoodRecord/FoodRecord') === 0) return true
    if (r.indexOf('pages/nutritionbrochure/recognition-page/recognition-page') === 0) return true
    if (r.indexOf('pages/nutritionbrochure/foodpage/foodpage') === 0) return true
    if (r.indexOf('pages/nutritionbrochure/food-detail/food-detail') === 0) return true
    // VIP页面
    if (r.indexOf('pages/profile/vip') === 0) return true
    if (r.indexOf('pages/profile/vip-center') === 0) return true
    return false
  },

  shouldRequireLogin(route) {
    return !this.isPublicRoute(route)
  },

  buildRedirectUrl(route, options) {
    const r = String(route || '').trim().replace(/^\/+/, '')
    if (!r) return ''
    const o = (options && typeof options === 'object') ? options : {}
    const keys = Object.keys(o)
    if (!keys.length) return `/${r}`
    const query = keys
      .filter(k => o[k] != null && String(o[k]).trim() !== '')
      .map(k => `${encodeURIComponent(k)}=${encodeURIComponent(String(o[k]))}`)
      .join('&')
    return query ? `/${r}?${query}` : `/${r}`
  },

  navigateToLoginOnce({ redirect } = {}) {
    if (this.__loginRedirecting) return
    this.__loginRedirecting = true
    const r = String(redirect || '').trim()
    const url = r ? `/pages/login/login?auto=1&redirect=${encodeURIComponent(r)}` : '/pages/login/login?auto=1'
    wx.navigateTo({
      url,
      complete: () => {
        setTimeout(() => { this.__loginRedirecting = false }, 800)
      }
    })
  },

  runLoginGuard() {
    let route = ''
    let options = null
    try {
      const pages = getCurrentPages()
      const current = pages && pages[pages.length - 1]
      route = current && current.route ? String(current.route) : ''
      options = current && current.options ? current.options : null
    } catch (e) {
      route = ''
      options = null
    }

    if (!route) return
    if (!this.shouldRequireLogin(route)) return
    if (this.isLoggedIn()) return
    if (this.__loginGuardInflight) return

    this.__loginGuardInflight = true
    const redirect = this.buildRedirectUrl(route, options)
    Promise.resolve()
      .then(() => this.ensureAuth({ silent: true }))
      .then((ok) => {
        if (ok) return
        this.navigateToLoginOnce({ redirect })
      })
      .catch(() => this.navigateToLoginOnce({ redirect }))
      .finally(() => { this.__loginGuardInflight = false })
  },

  clearAuth() {
    try { wx.removeStorageSync('token'); } catch (e) {}
    try { wx.removeStorageSync('AUTH_TOKEN'); } catch (e) {}
    try { wx.removeStorageSync('REFRESH_TOKEN'); } catch (e) {}
    try { wx.removeStorageSync('refreshToken'); } catch (e) {}
    try { wx.removeStorageSync('AI_INBOX_CURSOR'); } catch (e) {}
    try {
      const user = wx.getStorageSync('userInfo') || {};
      if (user && typeof user === 'object') {
        user.isLoggedIn = false;
        wx.setStorageSync('userInfo', user);
      } else {
        wx.removeStorageSync('userInfo');
      }
    } catch (e) {}
    try { wx.removeStorageSync('user_cache'); } catch (e) {}
    try { wx.removeStorageSync('user_state_v1'); } catch (e) {}
  },

  ensureAuth({ silent } = {}) {
    if (this._ensureAuthPromise) return this._ensureAuthPromise;
    this._ensureAuthPromise = (async () => {
      const token = this.getAuthToken();
      if (token) return true;

      const refreshToken = this.getRefreshToken();
      if (refreshToken) {
        try {
          const request = require('./utils/request.js');
          const resp = await request.post('/auth/token/refresh', { refreshToken }, { loading: false, showError: false });
          const data = (resp && (resp.data || resp)) || {};
          const at = data.accessToken || data.token || '';
          const rt = data.refreshToken || data.refresh_token || '';
          if (at) {
            this.setAuthToken(at);
            if (rt) this.setRefreshToken(rt);
            try {
              const u = wx.getStorageSync('userInfo') || {};
              if (u && typeof u === 'object') {
                u.isLoggedIn = true;
                u.loginTime = u.loginTime || Date.now();
                wx.setStorageSync('userInfo', u);
              }
            } catch (e) {}
            return true;
          }
        } catch (e) {}
      }

      let user = null;
      try { user = wx.getStorageSync('userInfo') || null; } catch (e) { user = null; }
      const hasEver = !!refreshToken || (!!user && typeof user === 'object' && (user.isLoggedIn || user.uid || user.id));
      if (!hasEver) return false;

      const code = await new Promise((resolve, reject) => {
        wx.login({
          success: (res) => resolve(res && res.code ? res.code : ''),
          fail: reject
        });
      });
      if (!code) return false;

      const request = require('./utils/request.js');
      const resp = await request.post('/auth/login/wechat', { code }, { loading: false, showError: false });
      const data = (resp && (resp.data || resp)) || {};
      const at = data.accessToken || '';
      const rt = data.refreshToken || '';
      if (at) this.setAuthToken(at);
      if (rt) this.setRefreshToken(rt);

      const rawUser = data.user || {};
      const mergedUser = {
        ...(user && typeof user === 'object' ? user : {}),
        ...rawUser,
        isLoggedIn: true,
        loginTime: Date.now()
      };
      try { wx.setStorageSync('userInfo', mergedUser); } catch (e) {}
      try { wx.setStorageSync('user_cache', mergedUser); } catch (e) {}
      return !!this.getAuthToken();
    })()
      .catch(() => false)
      .finally(() => { this._ensureAuthPromise = null; });

    return this._ensureAuthPromise;
  },

  setAiInbox(list) {
    try { wx.setStorageSync('AI_CHAT_INBOX', Array.isArray(list) ? list : []); } catch (e) {}
  },

  getAiInboxSeen() {
    try {
      const v = wx.getStorageSync('AI_CHAT_INBOX_SEEN');
      return Array.isArray(v) ? v : [];
    } catch (e) {
      return [];
    }
  },

  setAiInboxSeen(list) {
    try { wx.setStorageSync('AI_CHAT_INBOX_SEEN', Array.isArray(list) ? list : []); } catch (e) {}
  },

  markAiInboxSeen(key) {
    if (!key) return;
    const list = this.getAiInboxSeen();
    list.push(String(key));
    const trimmed = list.length > 200 ? list.slice(list.length - 200) : list;
    this.setAiInboxSeen(trimmed);
  },

  hasAiInboxSeen(key) {
    if (!key) return false;
    const list = this.getAiInboxSeen();
    return list.indexOf(String(key)) !== -1;
  },

  enqueueAiInboxMessage(message, userName) {
    const item = {
      id: `push_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`,
      message: String(message || ''),
      userName: userName || '',
      ts: Date.now()
    };
    const list = this.getAiInbox();
    list.push(item);
    const trimmed = list.length > 50 ? list.slice(list.length - 50) : list;
    this.setAiInbox(trimmed);
    this.triggerEvent('aiInboxUpdate', item);
    return item;
  },

  enqueueAiInboxPayload(payload) {
    const serverId = payload.serverMessageId || payload.id || payload.messageId || '';
    const dedupeKey = payload.dedupeKey || serverId || '';
    if (dedupeKey && this.hasAiInboxSeen(dedupeKey)) return null;

    const message = String(payload.text || payload.message || payload.content || '');
    if (!message) return null;

    const item = {
      id: serverId || `push_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`,
      serverId: serverId || '',
      dedupeKey: dedupeKey || '',
      text: String(payload.text || ''),
      message,
      userName: payload.userName || payload.fromName || '',
      ts: Number(payload.ts || payload.createdAt || Date.now()) || Date.now(),
      createdAt: payload.createdAt || '',
      type: payload.type || 'text',
      source: payload.source || 'server',
      deepLink: payload.deepLink || payload.link || '',
      systemMessageType: payload.systemMessageType || '',
      explanationCode: payload.explanationCode || '',
      explanationArgs: payload.explanationArgs || null,
      decisionKey: payload.decisionKey || '',
      timeSlot: payload.timeSlot || '',
      subject: payload.subject || ''
    };

    if (item.dedupeKey) this.markAiInboxSeen(item.dedupeKey);

    const list = this.getAiInbox();
    list.push(item);
    const trimmed = list.length > 50 ? list.slice(list.length - 50) : list;
    this.setAiInbox(trimmed);
    this.triggerEvent('aiInboxUpdate', item);
    return item;
  },

  normalizeApiHost(url) {
    let u = String(url || '').trim();
    if (!u) return '';
    u = u.replace(/\/+$/, '');
    if (u.endsWith('/api')) u = u.slice(0, -4);
    return u;
  },

  getApiBase() {
    try {
      const raw = (this.globalData && this.globalData.apiBaseUrl) || wx.getStorageSync('API_BASE_URL') || DEFAULT_API_BASE_URL;
      return this.normalizeApiHost(raw) || DEFAULT_API_BASE_URL;
    } catch (e) {
      return DEFAULT_API_BASE_URL;
    }
  },

  setApiBaseUrl(url) {
    try {
      const normalized = this.normalizeApiHost(url);
      if (!normalized) return;
      if (!this.globalData) this.globalData = {};
      this.globalData.apiBaseUrl = normalized;
      try { wx.setStorageSync('API_BASE_URL', normalized); } catch (e) {}
    } catch (e) {}
  },

  clearApiBaseUrl() {
    try { wx.removeStorageSync('API_BASE_URL'); } catch (e) {}
    try {
      if (!this.globalData) this.globalData = {};
      this.globalData.apiBaseUrl = DEFAULT_API_BASE_URL;
    } catch (e) {}
  },

  startAiInboxPolling() {
    if (this.globalData.aiInboxPoller) return;
    const state = this.globalData.aiInboxPollState || { intervalMs: 25000, emptyStreak: 0 };
    state.intervalMs = Math.max(8000, Number(state.intervalMs) || 25000);
    state.emptyStreak = Math.max(0, Number(state.emptyStreak) || 0);
    this.globalData.aiInboxPollState = state;

    const loop = () => {
      this.fetchAiInboxOnce({ silent: true })
        .then((r) => {
          const count = r && Number.isFinite(r.messageCount) ? r.messageCount : 0;
          if (count > 0) {
            state.emptyStreak = 0;
            state.intervalMs = 25000;
          } else {
            state.emptyStreak += 1;
            if (state.emptyStreak >= 4) {
              state.intervalMs = 60000;
            } else if (state.emptyStreak >= 2) {
              state.intervalMs = 40000;
            } else {
              state.intervalMs = 30000;
            }
          }
        })
        .catch(() => {})
        .finally(() => {
          if (!this.globalData.aiInboxPoller) return;
          this.globalData.aiInboxPoller = setTimeout(loop, state.intervalMs);
        });
    };

    this.globalData.aiInboxPoller = setTimeout(loop, 1200);
  },

  stopAiInboxPolling() {
    if (!this.globalData.aiInboxPoller) return;
    try { clearTimeout(this.globalData.aiInboxPoller); } catch (e) {}
    this.globalData.aiInboxPoller = null;
  },

  fetchAiInboxOnce({ silent } = {}) {
    try {
      const token = this.getAuthToken();
      if (!token) return Promise.resolve({ messageCount: 0 });
      const cursor = wx.getStorageSync('AI_INBOX_CURSOR') || '';
      const onAuthExpired = () => {
        this.clearAuth();
        try {
          const pages = getCurrentPages();
          const current = pages && pages[pages.length - 1];
          const route = current && current.route ? String(current.route) : '';
          if (route.indexOf('pages/login/login') === -1) {
            wx.navigateTo({ url: '/pages/login/login?auto=1' });
          }
        } catch (e) {}
      };

      const request = require('./utils/request.js');
      return request.get('/inbox/pull', cursor ? { cursor } : {}, { loading: false, showError: false })
        .then((body) => {
          const data = (body && (body.data || body)) || {};
          const messages = data.messages || data.items || [];
          const nextCursor = data.nextCursor || data.cursor || '';
          if (nextCursor) {
            try { wx.setStorageSync('AI_INBOX_CURSOR', nextCursor); } catch (e) {}
          }
          if (Array.isArray(messages) && messages.length) {
            messages.forEach((m) => this.showNotification(m));
            if (!silent) {
              try { wx.vibrateShort({ type: 'light' }); } catch (e) {}
            }
          }
          const ids = Array.isArray(messages)
            ? messages.map(m => (m && (m.serverMessageId || m.id || m.messageId)) ? String(m.serverMessageId || m.id || m.messageId) : '').filter(Boolean)
            : [];
          const ackToken = (typeof data.ackToken === 'string' && data.ackToken) ? data.ackToken : '';
          if (ackToken || ids.length) this.ackAiInbox({ ackToken, ids });
          return { messageCount: Array.isArray(messages) ? messages.length : 0 };
        })
        .catch((err) => {
          const status = err && err.statusCode
          if (status === 401 || status === 403) {
            onAuthExpired()
          }
          return { messageCount: 0 };
        });
    } catch (e) {
      return Promise.resolve({ messageCount: 0 });
    }
  },

  ackAiInbox({ ackToken, ids } = {}) {
    try {
      const token = this.getAuthToken();
      if (!token) return;
      const request = require('./utils/request.js');
      request.post('/inbox/ack', {
        ackToken: ackToken || '',
        ids: Array.isArray(ids) ? ids : []
      }, { loading: false, showError: false }).catch(() => {});
    } catch (e) {}
  },

  consumeAiInbox() {
    const list = this.getAiInbox();
    this.setAiInbox([]);
    return list;
  }
})

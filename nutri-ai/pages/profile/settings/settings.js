const DEFAULT_USER = {
  avatar: '/images/default_avatar.svg',
  nickname: '游客',
  id: '-'
};

const KEEP_KEYS_ON_CLEAR = new Set([
  'token',
  'AUTH_TOKEN',
  'REFRESH_TOKEN',
  'refreshToken',
  'API_BASE_URL',
  'userInfo',
  'user_cache',
  'userProfile',
  'nutrition_profile',
  'user_state_v1',
  'setting_notify',
  'setting_haptic',
  'setting_personalize'
]);

function formatSizeKb(kb) {
  const n = Number(kb) || 0;
  if (n < 1024) return `${n}KB`;
  return `${(n / 1024).toFixed(2)}MB`;
}

Page({
  data: {
    user: DEFAULT_USER,
    cacheSize: '0KB',
    notifyEnabled: true,
    hapticEnabled: true,
    personalizeEnabled: false,
    capabilityStates: null,
    capabilityEnabledMap: {},
    capabilityLoading: false
  },

  onLoad() {
    this.refresh();
  },

  onShow() {
    this.refresh();
  },

  refresh() {
    const us = require('../../../utils/userState.js')
    const state = us.load()
    const raw = (state && state.user) || wx.getStorageSync('user_cache') || wx.getStorageSync('userProfile') || wx.getStorageSync('userInfo') || {};
    const user = {
      avatar: raw.avatar || raw.avatarUrl || DEFAULT_USER.avatar,
      nickname: raw.nickname || DEFAULT_USER.nickname,
      id: raw.id || raw.uid || raw.userId || DEFAULT_USER.id
    };
    const notifyEnabled = wx.getStorageSync('setting_notify');
    const hapticEnabled = wx.getStorageSync('setting_haptic');
    const personalizeEnabled = wx.getStorageSync('setting_personalize');

    let cacheSize = '0KB';
    try {
      const info = wx.getStorageInfoSync();
      cacheSize = formatSizeKb(info.currentSize);
    } catch (e) {}

    this.setData({
      user,
      cacheSize,
      notifyEnabled: typeof notifyEnabled === 'boolean' ? notifyEnabled : true,
      hapticEnabled: typeof hapticEnabled === 'boolean' ? hapticEnabled : true,
      personalizeEnabled: typeof personalizeEnabled === 'boolean' ? personalizeEnabled : false
    });

    this.refreshCapabilityStates();
  },

  async refreshCapabilityStates() {
    if (this.data.capabilityLoading) return;
    this.setData({ capabilityLoading: true });
    try {
      const request = require('../../../utils/request.js');
      const resp = await request.get('/user/capability', {}, { loading: false, showError: false });
      const body = (resp && (resp.data || resp)) || {};
      const data = body && (body.data || body) ? (body.data || body) : {};
      const states = data && data.states ? data.states : null;
      const safeStates = states && typeof states === 'object' ? states : null;
      const enabledMap = {};
      if (safeStates) {
        ['PROACTIVE_REMINDER', 'MEDICATION_REMINDER', 'SUPPLEMENT_REMINDER', 'PROACTIVE_SUMMARY'].forEach((k) => {
          enabledMap[k] = String(safeStates[k] || '').trim() === 'ENABLED';
        });
      }
      this.setData({ capabilityStates: safeStates, capabilityEnabledMap: enabledMap });
    } catch (e) {
      this.setData({ capabilityStates: null, capabilityEnabledMap: {} });
    } finally {
      this.setData({ capabilityLoading: false });
    }
  },

  async onToggleCapability(e) {
    const key = e && e.currentTarget && e.currentTarget.dataset ? String(e.currentTarget.dataset.capkey || '') : '';
    const enabled = !!(e && e.detail && e.detail.value);
    if (!key) return;
    try {
      const request = require('../../../utils/request.js');
      await request.post('/user/capability/set', {
        capabilityKey: key,
        state: enabled ? 'ENABLED' : 'OFF',
        reason: 'user_toggle'
      }, { loading: true, showError: true });
    } catch (err) {
      try { wx.showToast({ title: '更新失败', icon: 'none' }); } catch (e2) {}
    } finally {
      this.refreshCapabilityStates();
    }
  },

  openProfileSetup() {
    wx.navigateTo({ url: '/pages/profile/profile-setup/profile-setup' });
  },

  copyUserId() {
    wx.setClipboardData({
      data: String(this.data.user.id || '-'),
      success: () => wx.showToast({ title: 'ID已复制', icon: 'success' })
    });
    if (this.data.hapticEnabled) wx.vibrateShort({ type: 'light' });
  },

  onToggleNotify(e) {
    const v = !!e.detail.value;
    wx.setStorageSync('setting_notify', v);
    this.setData({ notifyEnabled: v });
    if (this.data.hapticEnabled) wx.vibrateShort({ type: 'light' });
  },

  onToggleHaptic(e) {
    const v = !!e.detail.value;
    wx.setStorageSync('setting_haptic', v);
    this.setData({ hapticEnabled: v });
    if (v) wx.vibrateShort({ type: 'light' });
  },

  onTogglePersonalize(e) {
    const v = !!e.detail.value;
    wx.setStorageSync('setting_personalize', v);
    this.setData({ personalizeEnabled: v });
    if (this.data.hapticEnabled) wx.vibrateShort({ type: 'light' });
  },

  cancelAccount() {
    wx.showModal({
      title: '确认注销',
      content: '注销后无法恢复，请谨慎操作',
      confirmText: '注销',
      confirmColor: '#ef4444',
      success: (res) => {
        if (!res.confirm) return;
        wx.showToast({ title: '注销功能开发中', icon: 'none' });
        if (this.data.hapticEnabled) wx.vibrateShort({ type: 'light' });
      }
    });
  },

  openPaySettings() {
    wx.showToast({ title: '功能开发中', icon: 'none' });
    if (this.data.hapticEnabled) wx.vibrateShort({ type: 'light' });
  },

  showPrivacySummary() {
    wx.showModal({
      title: '隐私政策摘要',
      content: '我们会在提供服务所必需的范围内收集与使用信息，并采取合理措施保护您的数据安全。',
      showCancel: false,
      confirmText: '我知道了'
    });
    if (this.data.hapticEnabled) wx.vibrateShort({ type: 'light' });
  },

  showInfoCollectList() {
    wx.showModal({
      title: '个人信息收集清单',
      content: '用于登录、基础服务提供与体验优化。具体以实际业务场景为准。',
      showCancel: false,
      confirmText: '我知道了'
    });
    if (this.data.hapticEnabled) wx.vibrateShort({ type: 'light' });
  },

  showInfoShareList() {
    wx.showModal({
      title: '个人信息共享清单',
      content: '为完成必要服务可能与第三方服务商共享必要信息。具体以实际业务场景为准。',
      showCancel: false,
      confirmText: '我知道了'
    });
    if (this.data.hapticEnabled) wx.vibrateShort({ type: 'light' });
  },

  contactService() {
    wx.showModal({
      title: '联系客服',
      content: '客服入口开发中，您可先通过「投诉与建议」反馈问题。',
      showCancel: false,
      confirmText: '知道了'
    });
    if (this.data.hapticEnabled) wx.vibrateShort({ type: 'light' });
  },

  clearCache() {
    wx.showModal({
      title: '清理缓存',
      content: '将清理本地缓存数据，不会退出登录。',
      confirmText: '清理',
      success: (res) => {
        if (!res.confirm) return;
        try {
          const info = wx.getStorageInfoSync();
          (info.keys || []).forEach((k) => {
            if (!KEEP_KEYS_ON_CLEAR.has(k)) {
              try { wx.removeStorageSync(k); } catch (e) {}
            }
          });
        } catch (e) {}
        this.refresh();
        wx.showToast({ title: '已清理', icon: 'success' });
        if (this.data.hapticEnabled) wx.vibrateShort({ type: 'light' });
      }
    });
  },

  showPrivacyPolicy() {
    wx.showModal({
      title: '隐私政策',
      content: '我们会在提供服务所必需的范围内收集与使用信息，并采取合理措施保护您的数据安全。',
      showCancel: false,
      confirmText: '我知道了'
    });
  },

  showUserAgreement() {
    wx.showModal({
      title: '用户协议',
      content: '使用本产品即表示您同意遵守相关服务条款与使用规范。',
      showCancel: false,
      confirmText: '我知道了'
    });
  },

  showAbout() {
    wx.showModal({
      title: '关于 AI 营养师',
      content: 'AI 营养师为您提供饮食记录、营养分析与健康建议等服务。',
      showCancel: false,
      confirmText: '好的'
    });
  },

  logout() {
    wx.showModal({
      title: '退出登录',
      content: '确认退出当前账号？',
      confirmText: '退出',
      confirmColor: '#ef4444',
      success: async (res) => {
        if (!res.confirm) return;
        let refreshToken = '';
        try { refreshToken = wx.getStorageSync('REFRESH_TOKEN') || wx.getStorageSync('refreshToken') || ''; } catch (e) { refreshToken = ''; }
        refreshToken = String(refreshToken || '').trim();

        wx.showLoading({ title: '退出中...', mask: true });
        try {
          const request = require('../../../utils/request.js');
          if (refreshToken) {
            await request.post('/auth/logout', { refreshToken }, { loading: false, showError: false });
          }
        } catch (e) {} finally {
          try { wx.hideLoading(); } catch (e) {}
        }

        try {
          const app = getApp();
          if (app && app.clearAuth) app.clearAuth();
        } catch (e) {}

        ['token', 'AUTH_TOKEN', 'REFRESH_TOKEN', 'refreshToken', 'userInfo', 'user_cache', 'userProfile', 'nutrition_profile', 'user_state_v1'].forEach((k) => {
          try { wx.removeStorageSync(k); } catch (e) {}
        });
        wx.reLaunch({ url: '/pages/login/login?auto=1' });
      }
    });
  }
});

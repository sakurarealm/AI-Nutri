Page({
  data: {
    tabIndex: 0,
    contentHeightPx: 0,
    safeBottomPx: 0,
    ruleItems: [
      { key: 'vip', title: '会员服务协议' },
      { key: 'privacy', title: '隐私政策' },
      { key: 'points', title: '积分规则' },
      { key: 'terms', title: '使用条款' },
      { key: 'license', title: '经营证照公示' },
      { key: 'service', title: '各类服务公示' },
      { key: 'biz', title: '各类业务规则' }
    ]
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

  onUnload() {
    wx.offWindowResize && wx.offWindowResize(this._onResize);
  },

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

  onTabTap(e) {
    const idx = Number(e.currentTarget.dataset.index);
    if (!isNaN(idx)) this.setData({ tabIndex: idx });
  },

  onSwiperChange(e) {
    const idx = Number(e.detail.current);
    if (!isNaN(idx)) this.setData({ tabIndex: idx });
  },

  onRuleTap(e) {
    const key = String(e.currentTarget.dataset.key || '');
    wx.showToast({ title: '内容待完善', icon: 'none' });
    if (!key) return;
  }
});

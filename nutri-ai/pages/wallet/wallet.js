Page({
  data: {
    balance: '0.00',
    tabIndex: 0
  },

  onTabTap(e) {
    const idx = Number(e.currentTarget.dataset.index);
    if (!Number.isFinite(idx)) return;
    if (idx === this.data.tabIndex) return;
    this.setData({ tabIndex: idx });
  },

  onSwiperChange(e) {
    const idx = Number(e.detail.current);
    if (!Number.isFinite(idx)) return;
    if (idx === this.data.tabIndex) return;
    this.setData({ tabIndex: idx });
  },

  onCardAction(e) {
    const action = String(e.currentTarget.dataset.action || '');
    if (action === 'recharge') {
      wx.showToast({ title: '充值功能开发中', icon: 'none' });
      return;
    }
    if (action === 'manage') {
      wx.showToast({ title: '管理功能开发中', icon: 'none' });
      return;
    }
  },

  onHistory() {
    wx.showToast({ title: '历史记录开发中', icon: 'none' });
  }
});

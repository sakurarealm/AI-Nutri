Page({
  data: {
    selectedKey: '',
    types: [
      { key: 'shop', label: '商城相关' },
      { key: 'nutritionist', label: '营养师使用' },
      { key: 'vip', label: '会员相关' },
      { key: 'kcoin', label: 'K币相关' },
      { key: 'coupon', label: '优惠券相关' },
      { key: 'quality', label: '保健品质量' },
      { key: 'other', label: '其他问题' }
    ]
  },

  onTypeTap(e) {
    const key = String(e.currentTarget.dataset.key || '');
    const types = Array.isArray(this.data.types) ? this.data.types : [];
    const hit = types.find((t) => t && t.key === key) || null;
    const subject = hit && hit.label ? hit.label : '';
    this.setData({ selectedKey: key });
    wx.navigateTo({
      url: `/pages/feedback/detail/detail?subject=${encodeURIComponent(subject) || encodeURIComponent('其他问题')}`,
      fail: () => wx.showToast({ title: '功能开发中', icon: 'none' })
    });
  }
});

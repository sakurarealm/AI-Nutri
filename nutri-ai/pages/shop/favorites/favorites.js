Page({
  data: {
    favorites: []
  },

  onLoad() {
    const favorites = [
      { id: 1, name: '维生素C', spec: '1000mg/瓶', price: 29.9, image: '/static/index/image/1.jpg' },
      { id: 2, name: '钙镁锌', spec: '500mg/瓶', price: 39.9, image: '/static/index/image/2.jpg' },
      { id: 6, name: '褪黑素', spec: '3mg/瓶', price: 79.9, image: '/static/index/image/6.jpg' }
    ];
    this.setData({ favorites });
  },

  onBuy(e) {
    const id = String(e.currentTarget.dataset.id || '');
    if (!id) return;
    wx.showToast({ title: '跳转购买开发中', icon: 'none' });
  },

  onUnfavorite(e) {
    const id = String(e.currentTarget.dataset.id || '');
    if (!id) return;
    const next = (this.data.favorites || []).filter((p) => String(p.id) !== id);
    this.setData({ favorites: next });
    wx.showToast({ title: '已取消收藏', icon: 'none' });
  },

  goShop() {
    wx.switchTab({ url: '/pages/shop/shop' });
  }
});


Page({
  data: {
    addresses: [
      {
        id: 'a1',
        name: '张先生',
        phone: '138****1024',
        address: '上海市 浦东新区 世纪大道 100 号 8F（Ai-nutri 健康中心）'
      },
      {
        id: 'a2',
        name: '李女士',
        phone: '186****7788',
        address: '上海市 静安区 南京西路 999 号 2 栋 1602（周末可配送）'
      }
    ]
  },

  onShow() {
    let picked = null;
    try {
      picked = wx.getStorageSync('ADDRESS_PICK_RESULT') || null;
    } catch (e) {}
    if (!picked || !picked.location) return;

    const location = picked.location || {};
    const regionShort = picked.regionShort || '';
    const name = location.name || '';
    const address = location.address || '';
    const full = [regionShort, name, address].filter(Boolean).join(' ');

    const id = `a_${Date.now()}`;
    const next = [{ id, name: '新地址', phone: '', address: full }, ...(this.data.addresses || [])];
    this.setData({ addresses: next });

    try {
      wx.removeStorageSync('ADDRESS_PICK_RESULT');
    } catch (e) {}
  },

  onEdit() {
    wx.showToast({ title: '功能开发中', icon: 'none' });
  },

  onAdd() {
    wx.navigateTo({
      url: '/pages/address/picker/picker',
      fail: () => wx.showToast({ title: '功能开发中', icon: 'none' })
    });
  }
});

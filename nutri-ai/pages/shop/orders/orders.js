function sumQty(items) {
  return (items || []).reduce((acc, it) => acc + (Number(it.qty) || 0), 0);
}

function sumAmount(items) {
  return (items || []).reduce((acc, it) => acc + (Number(it.qty) || 0) * (Number(it.price) || 0), 0).toFixed(2);
}

function statusText(status) {
  if (status === 'unpaid') return '未付款';
  if (status === 'toShip') return '待发货';
  if (status === 'shipped') return '已发货';
  if (status === 'delivered') return '已送达';
  return '全部';
}

function buildOrder(base) {
  const items = base.items || [];
  return {
    ...base,
    statusText: statusText(base.status),
    totalQty: sumQty(items),
    totalAmount: sumAmount(items)
  };
}

Page({
  data: {
    tabIndex: 0,
    tabs: [
      { key: 'all', label: '全部' },
      { key: 'unpaid', label: '未付款' },
      { key: 'toShip', label: '待发货' },
      { key: 'shipped', label: '已发货' },
      { key: 'delivered', label: '已送达' }
    ],
    tabPages: []
  },

  onLoad() {
    const allOrders = [
      buildOrder({
        id: 'AN202601120001',
        status: 'unpaid',
        time: '今天 10:26',
        items: [
          { skuId: 'sku-vc-30', name: '维生素C', spec: '1000mg/瓶', price: 29.9, qty: 1, image: '/static/index/image/1.jpg' },
          { skuId: 'sku-zinc', name: '钙镁锌', spec: '500mg/瓶', price: 39.9, qty: 1, image: '/static/index/image/2.jpg' }
        ],
        primaryAction: { key: 'pay', text: '去支付' },
        secondaryAction: { key: 'cancel', text: '取消' }
      }),
      buildOrder({
        id: 'AN202601110045',
        status: 'toShip',
        time: '昨天 21:03',
        items: [
          { skuId: 'sku-d3', name: '维生素D3', spec: '2000IU/瓶', price: 35.9, qty: 1, image: '/static/index/image/10.jpg' }
        ],
        primaryAction: { key: 'remind', text: '提醒发货' }
      }),
      buildOrder({
        id: 'AN202601100118',
        status: 'shipped',
        time: '01-10 13:12',
        items: [
          { skuId: 'sku-melatonin', name: '褪黑素', spec: '3mg/瓶', price: 79.9, qty: 1, image: '/static/index/image/6.jpg' },
          { skuId: 'sku-probiotic', name: '双歧杆菌', spec: '10亿活菌/瓶', price: 69.9, qty: 1, image: '/static/index/image/5.jpg' }
        ],
        shipMap: {
          center: { longitude: 121.4737, latitude: 31.2304 },
          caption: '上海转运中心 → 苏州派送站',
          markers: [
            { id: 1, longitude: 121.4737, latitude: 31.2304, width: 18, height: 18 },
            { id: 2, longitude: 120.5853, latitude: 31.2989, width: 18, height: 18 }
          ],
          polyline: [
            { points: [{ longitude: 121.4737, latitude: 31.2304 }, { longitude: 120.5853, latitude: 31.2989 }], color: '#0ea5e9', width: 4 }
          ]
        },
        primaryAction: { key: 'logistics', text: '查看物流' }
      }),
      buildOrder({
        id: 'AN202601080032',
        status: 'delivered',
        time: '01-08 09:45',
        items: [
          { skuId: 'sku-collagen', name: '胶原蛋白肽', spec: '5g/袋', price: 128.9, qty: 1, image: '/static/index/image/11.jpg' }
        ],
        primaryAction: { key: 'rebuy', text: '再来一单' }
      })
    ];

    const byTab = this.buildOrdersByTab(allOrders);
    const tabPages = this.buildTabPages(this.data.tabs, byTab);
    this.setData({ tabPages });
  },

  buildOrdersByTab(allOrders) {
    const list = Array.isArray(allOrders) ? allOrders : [];
    const unpaid = list.filter((o) => o.status === 'unpaid');
    const toShip = list.filter((o) => o.status === 'toShip');
    const shipped = list.filter((o) => o.status === 'shipped');
    const delivered = list.filter((o) => o.status === 'delivered');
    return [list, unpaid, toShip, shipped, delivered];
  },

  buildTabPages(tabs, ordersByTab) {
    const safeTabs = Array.isArray(tabs) ? tabs : [];
    const safeOrders = Array.isArray(ordersByTab) ? ordersByTab : [];
    return safeTabs.map((t, idx) => {
      const orders = Array.isArray(safeOrders[idx]) ? safeOrders[idx] : [];
      return {
        key: t.key,
        label: t.label,
        orders,
        empty: orders.length === 0
      };
    });
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

  onOrderAction(e) {
    const action = String(e.currentTarget.dataset.action || '');
    if (action === 'pay') {
      wx.showToast({ title: '支付功能开发中', icon: 'none' });
      return;
    }
    if (action === 'cancel') {
      wx.showToast({ title: '取消订单功能开发中', icon: 'none' });
      return;
    }
    if (action === 'remind') {
      wx.showToast({ title: '已提醒发货', icon: 'none' });
      return;
    }
    if (action === 'logistics') {
      wx.showToast({ title: '物流详情开发中', icon: 'none' });
      return;
    }
    if (action === 'rebuy') {
      wx.showToast({ title: '已加入购物车（示例）', icon: 'none' });
      return;
    }
  },

  goShop() {
    wx.switchTab({ url: '/pages/shop/shop' });
  }
});

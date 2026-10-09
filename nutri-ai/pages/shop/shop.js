Page({
  data: {
    maskVisible: true,
    isRefreshing: false, // 添加刷新状态
    isLoadingMore: false, // 添加加载更多状态
    hasMore: true, // 还有更多数据
    page: 1, // 当前页码
    pageSize: 6, // 每页数量
    activeTab: 0,
    activeCategory: 0,
    activeNutritionCategory: 0,
    filteredProducts: [],
    activeCategoryName: '维生素',
    topTabs: [
      { label: '自选搭配' },
      { label: '推荐搭配' },
      { label: '营养包' }
    ],
    categories: [
      { name: '维生素', icon: '../../static/index/VC.png' },
      { name: '矿物质', icon: '../../static/index/矿物质.png' },
      { name: '草本', icon: '../../static/index/leaf.png' },
      { name: '功能', icon: '../../static/index/health.png' },
      { name: '益生菌', icon: '../../static/index/ysj.png' },
    ],
    nutritionCategories: [
      { name: '营养饮品', icon: '../../static/index/drink.png' },
      { name: '30天功能', icon: '../../static/index/30day.png' },
      { name: '五天应急', icon: '../../static/index/emergency.png' }
    ],
    products: [
      // 维生素类
      { id: 1, name: '维生素C', desc: '增强免疫力', price: 29.9, spec: '1000mg/瓶', category: '维生素', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/1.jpg' },
      { id: 10, name: '维生素D3', desc: '促进钙吸收', price: 35.9, spec: '2000IU/瓶', category: '维生素', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/1.jpg' },
      { id: 11, name: '维生素E', desc: '抗氧化美容', price: 42.9, spec: '400IU/瓶', category: '维生素', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/11.jpg' },
      { id: 12, name: 'B族维生素复合', desc: '提升精神状态', price: 38.9, spec: '100片/瓶', category: '维生素', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/12.jpg' },
      { id: 13, name: '维生素A', desc: '保护视力', price: 33.9, spec: '5000IU/瓶', category: '维生素', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/13.jpg' },
      { id: 14, name: '维生素K2', desc: '骨骼健康', price: 45.9, spec: '100mcg/瓶', category: '维生素', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/14.jpg' },
      
      // 矿物质类
      { id: 2, name: '钙镁锌', desc: '强健骨骼', price: 39.9, spec: '500mg/瓶', category: '矿物质', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/2.jpg' },
      { id: 15, name: '铁元素', desc: '补血养颜', price: 28.9, spec: '18mg/瓶', category: '矿物质', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/15.jpg' },
      { id: 16, name: '硒元素', desc: '抗氧化防癌', price: 52.9, spec: '200mcg/瓶', category: '矿物质', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/10.jpg' },
      { id: 17, name: '铬元素', desc: '调节血糖', price: 36.9, spec: '200mcg/瓶', category: '矿物质', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/2.jpg' },
      { id: 18, name: '钾元素', desc: '维持电解质平衡', price: 31.9, spec: '99mg/瓶', category: '矿物质', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/3.jpg' },
      
      // 草本类
      { id: 3, name: '银杏叶提取物', desc: '改善记忆', price: 49.9, spec: '200mg/瓶', category: '草本', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/3.jpg' },
      { id: 19, name: '人参提取物', desc: '增强体力', price: 68.9, spec: '500mg/瓶', category: '草本', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/4.jpg' },
      { id: 20, name: '西洋参', desc: '滋阴补气', price: 78.9, spec: '300mg/瓶', category: '草本', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/5.jpg' },
      { id: 21, name: '当归提取物', desc: '补血调经', price: 55.9, spec: '400mg/瓶', category: '草本', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/6.jpg' },
      { id: 22, name: '枸杞提取物', desc: '明目养肝', price: 43.9, spec: '500mg/瓶', category: '草本', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/7.jpg' },
      { id: 23, name: '灵芝孢子粉', desc: '增强免疫', price: 89.9, spec: '250mg/瓶', category: '草本', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/8.jpg' },
      
      // 功能类
      { id: 6, name: '褪黑素', desc: '改善睡眠', price: 79.9, spec: '3mg/瓶', category: '功能', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/6.jpg' },
      { id: 24, name: 'CoQ10辅酶', desc: '心脏保护', price: 98.9, spec: '100mg/瓶', category: '功能', image: '	cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/9.jpg' },
      { id: 25, name: '叶黄素', desc: '保护眼睛', price: 65.9, spec: '20mg/瓶', category: '功能', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/10.jpg' },
      { id: 26, name: '胶原蛋白肽', desc: '美容抗衰', price: 128.9, spec: '5g/袋', category: '功能', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/11.jpg' },
      { id: 27, name: '透明质酸', desc: '保湿润肤', price: 88.9, spec: '100mg/瓶', category: '功能', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/12.jpg' },
      
      // 益生菌类
      { id: 5, name: '双歧杆菌', desc: '肠道健康', price: 69.9, spec: '10亿活菌/瓶', category: '益生菌', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/5.jpg' },
      { id: 28, name: '乳酸菌复合', desc: '调节肠道菌群', price: 75.9, spec: '50亿活菌/瓶', category: '益生菌', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/13.jpg' },
      { id: 29, name: '嗜酸乳杆菌', desc: '改善消化', price: 58.9, spec: '20亿活菌/瓶', category: '益生菌', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/14.jpg' },
      { id: 30, name: '益生元纤维', desc: '促进益生菌生长', price: 48.9, spec: '5g/袋', category: '益生菌', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/15.jpg' },
      
      // 其他分类产品
      { id: 4, name: '葡萄籽', desc: '抗氧化', price: 59.9, spec: '300mg/瓶', category: '抗氧化', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/4.jpg' },
      { id: 7, name: '膳食纤维粉', desc: '促进消化', price: 89.9, spec: '500g/瓶', category: '膳食纤维', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/7.jpg' },
      { id: 8, name: '支链氨基酸', desc: '肌肉恢复', price: 99.9, spec: '400g/瓶', category: '氨基酸', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/8.jpg' },
      { id: 9, name: '乳清蛋白', desc: '增肌补能', price: 109.9, spec: '1kg/瓶', category: '蛋白质', image: '	cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/9.jpg' },
      // 更多模拟商品
      { id: 31, name: '螺旋藻片', desc: '全面营养补充', price: 59.9, spec: '500mg/瓶', category: '维生素', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/1.jpg' },
      { id: 32, name: '鱼油软胶囊', desc: '富含Omega-3', price: 89.9, spec: '1000mg/瓶', category: '功能', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/2.jpg' },
      { id: 33, name: '大蒜精油', desc: '增强抵抗力', price: 39.9, spec: '500mg/瓶', category: '草本', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/3.jpg' },
      { id: 34, name: '蜂胶胶囊', desc: '天然抗生素', price: 79.9, spec: '500mg/瓶', category: '功能', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/4.jpg' },
      { id: 35, name: '芦荟软胶囊', desc: '排毒养颜', price: 49.9, spec: '500mg/瓶', category: '草本', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/5.jpg' },
      { id: 36, name: '左旋肉碱', desc: '燃脂塑形', price: 69.9, spec: '500mg/瓶', category: '功能', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/6.jpg' },
      { id: 37, name: '番茄红素', desc: '男性健康', price: 89.9, spec: '10mg/瓶', category: '功能', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/7.jpg' },
      { id: 38, name: '纳豆激酶', desc: '血管清道夫', price: 109.9, spec: '2000FU/瓶', category: '功能', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/8.jpg' },
      { id: 39, name: '角鲨烯', desc: '供氧抗疲劳', price: 129.9, spec: '500mg/瓶', category: '功能', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/9.jpg' },
      { id: 40, name: '月见草油', desc: '调节内分泌', price: 59.9, spec: '500mg/瓶', category: '草本', image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/10.jpg' }
    ],
    recommendCombos: [
      {
        title: '免疫提升套装',
        desc: '维生素C + 锌 + 益生菌',
        price: 99.9,
        bgImage: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/1.jpg',
        pills: [
          { image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/1.jpg' },
          { image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/2.jpg' },
          { image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/3.jpg' }
        ]
      },
      {
        title: '骨骼健康套装',
        desc: '钙 + 维生素D + 镁',
        price: 89.9,
        bgImage: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/11.jpg',
        pills: [
          { image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/4.jpg' },
          { image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/5.jpg' },
          { image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/6.jpg' }
        ]
      },
      {
        title: '抗疲劳套装',
        desc: 'B族维生素 + 铁 + 绿茶提取物',
        price: 109.9,
        bgImage: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/15.jpg',
        pills: [
          { image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/7.jpg' },
          { image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/8.jpg' },
          { image: '	cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/9.jpg' }
        ]
      }
    ],
    functionalDrinks: [
      {
        name: '复合维生素能量饮料',
        desc: '富含B族维生素，帮助恢复活力',
        minPrice: 15.9,
        maxPrice: 129.9,
        image: '../../static/index/image/1.jpg'
      },
      {
        name: '电解质补充饮料',
        desc: '快速补充水分和矿物质',
        minPrice: 12.9,
        maxPrice: 99.9,
        image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/2.jpg'
      },
      {
        name: '胶原蛋白美容饮料',
        desc: '促进皮肤弹性，美容养颜',
        minPrice: 19.9,
        maxPrice: 159.9,
        image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/3.jpg'
      }
    ],
    thirtyDayPacks: [
      {
        name: '全面营养包',
        price: 299.9,
        desc: '30天全方位营养补充，适合日常保健',
        features: ['维生素', '矿物质', '抗氧化'],
        image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/4.jpg'
      },
      {
        name: '免疫增强包',
        price: 259.9,
        desc: '提升免疫力，预防感冒病毒',
        features: ['维C', '锌元素', '益生菌'],
        image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/5.jpg'
      },
      {
        name: '骨骼健康包',
        price: 279.9,
        desc: '强化骨骼，预防骨质疏松',
        features: ['钙镁', '维D3', 'K2'],
        image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/6.jpg'
      },
      {
        name: '美容养颜包',
        price: 319.9,
        desc: '由内而外美容，延缓衰老',
        features: ['胶原蛋白', '透明质酸', '维E'],
        image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/7.jpg'
      },
      {
        name: '心脏保护包',
        price: 389.9,
        desc: '保护心血管健康，降低风险',
        features: ['CoQ10', '欧米伽3', '叶酸'],
        image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/8.jpg'
      },
      {
        name: '肝脏排毒包',
        price: 269.9,
        desc: '清肝排毒，保护肝脏功能',
        features: ['奶蓟草', '姜黄素', 'NAC'],
        image: '	cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/9.jpg'
      },
      {
        name: '眼部护理包',
        price: 229.9,
        desc: '保护视力，缓解眼疲劳',
        features: ['叶黄素', '花青素', '维A'],
        image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/10.jpg'
      },
      {
        name: '运动恢复包',
        price: 349.9,
        desc: '加速运动后恢复，增强体能',
        features: ['BCAA', '肌酸', '蛋白质'],
        image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/11.jpg'
      },
      {
        name: '女性调理包',
        price: 299.9,
        desc: '调节内分泌，改善女性健康',
        features: ['月见草', '大豆异黄酮', '铁'],
        image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/12.jpg'
      },
      {
        name: '男性活力包',
        price: 329.9,
        desc: '提升男性活力，增强体质',
        features: ['玛卡', '锌硒', 'B族'],
        image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/13.jpg'
      }
    ],
    emergencyPacks: [
      {
        name: '五天应急营养包',
        desc: '快速补充全面营养，适合旅行/紧急情况',
        price: 99.9,
        image: '../../static/index/image/8.jpg'
      },
      {
        name: '五天免疫急救包',
        desc: '增强抵抗力，预防感冒',
        price: 89.9,
        image: '	cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/9.jpg'
      },
      {
        name: '五天能量补充包',
        desc: '恢复活力，抗疲劳',
        price: 109.9,
        image: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/image/10.jpg'
      }
    ]
  },

  // 页面生命周期函数
  onLoad(options) {
    // 获取系统信息和胶囊位置
    try {
      const systemInfo = wx.getSystemInfoSync();
      const menuButton = wx.getMenuButtonBoundingClientRect();
      const statusBarHeight = systemInfo.statusBarHeight;
      const navBarHeight = (menuButton.top - statusBarHeight) * 2 + menuButton.height;
      
      this.setData({
        statusBarHeight,
        navBarHeight,
        menuButtonHeight: menuButton.height,
        menuButtonTop: menuButton.top,
        menuButtonBottom: menuButton.bottom,
        // TabBar 高度估算：自定义TabBar通常为50px左右，加上底部安全区
        tabbarHeight: 120 // 60px * 2rpx 左右，这里作为基础值，wxss中会加上 safe-area
      });
    } catch (e) {
      console.error('获取系统信息失败', e);
    }

    // 初始化页面数据
    this.updateFilteredProducts();
    
    if (options.tab) {
      this.setData({
        activeTab: parseInt(options.tab)
      });
    }
  },

  onMaskBrowseOnly() {
    this.setData({ maskVisible: false });
  },

  onSubmitSuggestion() {
    wx.navigateTo({
      url: `/pages/feedback/detail/detail?subject=${encodeURIComponent('商城选品建议')}`
    });
  },

  onShow() {
    console.log('Shop页面显示');
    // 设置自定义tabbar的当前页面
    const tabbar = this.selectComponent('#tabbar');
    if (tabbar) {
      tabbar.setCurrent();
    }
    const app = getApp();
    if (app && app.triggerEvent) app.triggerEvent('notificationUpdate');
  },

  onReady() {
    console.log('Shop页面初次渲染完成');
  },

  onHide() {
    console.log('Shop页面隐藏');
  },

  onUnload() {
    console.log('Shop页面卸载');
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.onRefresherRefresh();
  },

  // ScrollView 下拉刷新
  onRefresherRefresh() {
    if (this.data.isRefreshing) return;
    this.setData({ isRefreshing: true });
    this.fetchProducts().then(() => {
      this.setData({ isRefreshing: false, page: 1, hasMore: true });
      wx.stopPullDownRefresh();
    }).catch(() => {
      this.setData({ isRefreshing: false });
      wx.stopPullDownRefresh();
      wx.showToast({ title: '刷新失败', icon: 'none' });
    });
  },

  // ScrollView 上拉加载更多
  onLoadMore() {
    if (this.data.isLoadingMore || !this.data.hasMore) return;
    this.setData({ isLoadingMore: true });
    const nextPage = this.data.page + 1;
    this.fetchProducts(nextPage).then((hasMore) => {
      this.setData({ page: nextPage, isLoadingMore: false, hasMore: !!hasMore });
    }).catch(() => {
      this.setData({ isLoadingMore: false });
      wx.showToast({ title: '加载失败', icon: 'none' });
    });
  },

  // 页面分享
  onShareAppMessage() {
    return {
      title: 'AI营养师 - 营养商城',
      path: '/pages/shop/shop',
      imageUrl: '../../static/index/share-shop.jpg'
    };
  },

  // 分享到朋友圈
  onShareTimeline() {
    return {
      title: 'AI营养师 - 营养商城',
      imageUrl: '../../static/index/share-shop.jpg'
    };
  },

  // 标签切换
  switchTab(e) {
    const index = parseInt(e.currentTarget.dataset.index);
    this.setData({
      activeTab: index,
      activeCategory: 0,
      activeNutritionCategory: 0,
      page: 1,
      hasMore: true,
      isLoadingMore: false
    });
    if (index === 0) {
      this.updateFilteredProducts();
    }
  },

  // 分类切换
  switchCategory(e) {
    const index = parseInt(e.currentTarget.dataset.index);
    this.setData({
      activeCategory: index,
      // 切换分类重置分页状态
      page: 1,
      hasMore: true,
      isLoadingMore: false
    });
    this.updateFilteredProducts();
  },

  // 营养包分类切换
  switchNutritionCategory(e) {
    const index = e.currentTarget.dataset.index;
    this.setData({ activeNutritionCategory: index });
    
    // 埋点统计
    wx.reportAnalytics('nutrition_category_switch', {
      category_index: index,
      category_name: this.data.nutritionCategories[index].name
    });
  },

  addToCart(e) {
    const product = e.currentTarget && e.currentTarget.dataset ? e.currentTarget.dataset.product : null
    if (!product || !product.id) return
    wx.showToast({ title: '已添加到购物车', icon: 'success' })
    try {
      let cart = wx.getStorageSync('shop_cart') || []
      const idx = cart.findIndex((it) => (it || {}).id === product.id)
      if (idx >= 0) {
        cart[idx].qty = (cart[idx].qty || 1) + 1
      } else {
        cart.push({ ...product, qty: 1 })
      }
      wx.setStorageSync('shop_cart', cart)
    } catch (e) {}
  },

  buyNow(e) {
    const product = e.currentTarget && e.currentTarget.dataset ? e.currentTarget.dataset.product : null
    if (!product || !product.id) return
    wx.navigateTo({
      url: `/pages/shop/checkout/checkout?product=${encodeURIComponent(JSON.stringify(product))}`
    })
  },

  // 查看商品详情
  viewProductDetail(e) {
    const item = e.currentTarget.dataset.product || 
                 e.currentTarget.dataset.combo || 
                 e.currentTarget.dataset.drink || 
                 e.currentTarget.dataset.emergency;
    
    if (!item) return;
    
    wx.navigateTo({
      url: `/pages/product-detail/product-detail?item=${JSON.stringify(item)}`
    });
  },

  onSearchTap() {
    wx.navigateTo({ url: '/pages/shop/search/search' })
  },

  // 获取当前分类名称
  getCurrentCategory() {
    const { activeTab, activeCategory, activeNutritionCategory } = this.data;
    
    if (activeTab === 0) {
      return this.data.categories[activeCategory]?.name || '自选搭配';
    } else if (activeTab === 1) {
      return '推荐搭配';
    } else {
      return this.data.nutritionCategories[activeNutritionCategory]?.name || '营养包';
    }
  },

  // 更新过滤后的产品列表
  updateFilteredProducts() {
    const { activeCategory, categories, products, activeTab } = this.data
    const categoryName = activeTab === 0 ? (categories[activeCategory] || {}).name : ''
    let filtered = products
    if (activeTab === 0 && categoryName) {
      filtered = products.filter(p => (p.category || '') === categoryName)
    }
    const start = 0
    const end = Math.min(this.data.page * this.data.pageSize, filtered.length)
    this.setData({
      filteredProducts: filtered.slice(start, end),
      activeCategoryName: categoryName || '',
      hasMore: end < filtered.length
    })
  },

  fetchProducts(pageNum) {
    const pg = pageNum || 1
    const self = this
    const apiBase = (getApp() || {}).globalData?.apiBase || ''

    if (apiBase) {
      return self._requestGet(`${apiBase}/shop/products`, {
        page: pg,
        pageSize: self.data.pageSize,
        category: self.data.activeCategory > 0 ? (self.data.categories[self.data.activeCategory] || {}).name : '',
        tab: self.data.activeTab
      }).then((res) => {
        const list = (res && res.data && res.data.list) || (res && res.list) || []
        if (pg === 1) {
          self.setData({ products: list })
        } else {
          const merged = [...self.data.products, ...list]
          const seen = new Set()
          const deduped = merged.filter(p => { const k = p.id; if (seen.has(k)) return false; seen.add(k); return true })
          self.setData({ products: deduped })
        }
        self.updateFilteredProducts()
        return (list && list.length >= self.data.pageSize)
      }).catch(() => {
        self.updateFilteredProducts()
        return true
      })
    }

    return new Promise((resolve) => {
      setTimeout(() => {
        self.updateFilteredProducts()
        resolve(true)
      }, 300)
    })
  },

  _requestGet(url, data) {
    return new Promise((resolve, reject) => {
      wx.request({
        url,
        data,
        method: 'GET',
        header: { 'Authorization': `Bearer ${(getApp() || {}).globalData?.token || ''}` },
        success: (res) => (res.statusCode === 200 ? resolve(res.data) : reject(res)),
        fail: reject
      })
    })
  },

  // 获取过滤后的商品列表
  getFilteredProducts() {
    const { activeCategory, categories, products } = this.data;
    const categoryName = categories[activeCategory]?.name;
    
    if (!categoryName) return products;
    
    return products.filter(product => product.category === categoryName);
  },

  // 获取当前营养包分类的商品
  getCurrentNutritionProducts() {
    const { activeNutritionCategory } = this.data;
    
    switch (activeNutritionCategory) {
      case 0:
        return this.data.functionalDrinks;
      case 1:
        return this.data.thirtyDayPacks;
      case 2:
        return this.data.emergencyPacks;
      default:
        return [];
    }
  }
});

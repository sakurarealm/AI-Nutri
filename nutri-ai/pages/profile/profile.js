const request = require('../../utils/request.js')
const us = require('../../utils/userState.js')

Page({
  data: {
    userInfo: {
      avatar: '/images/default_avatar.svg',
      nickname: 'AI-nutri用户',
      id: '-'
    },
    userAssets: {
      kCoins: 0,
      balanceYuan: '0.00'
    },
    vipInfo: {
      level: 0,
      levelName: '普通会员',
      expireDate: null,
      benefits: []
    },
    vipBadgeSrc: '',
    vipIconSrc: '/static/profile/level.png',
    healthData: {
      continuousDays: 0,
      healthLevel: 'Lv.1',
      weeklyGoals: '0/7',
      totalRecords: 0
    },
    nutritionProfile: null
  },

  onShow() {
    const tabbar = this.selectComponent('#tabbar')
    if (tabbar) {
      tabbar.setCurrent()
    }
    const app = getApp()
    if (app && app.triggerEvent) app.triggerEvent('notificationUpdate')
    this.loadFromCache()
    this.refreshAll()
  },

  onLoad() {
    this.loadFromCache()
    this.refreshAll()
  },

  onPullDownRefresh() {
    this.refreshAll().finally(() => {
      wx.stopPullDownRefresh()
    })
  },

  loadFromCache() {
    const state = us.load()
    if (!state || !state.user) return

    const u = us.mapUser(state.user)
    if (u && u.id && u.id !== '-') {
      this.setData({ userInfo: { ...this.data.userInfo, ...u } })
    }

    if (state.assets) {
      const a = us.mapAssets(state.assets)
      if (a) {
        this.setData({ userAssets: { ...this.data.userAssets, ...a } })
      }
    }

    if (state.vip) {
      const vipInfo = us.mapVipInfo(state.vip)
      const icons = us.computeVipIcons(vipInfo)
      this.setData({ vipInfo: { ...this.data.vipInfo, ...vipInfo }, ...icons })
    } else {
      const icons = us.computeVipIcons(this.data.vipInfo)
      this.setData({ ...icons })
    }

    if (state.nutritionProfile) {
      this.setData({ nutritionProfile: state.nutritionProfile })
    }

    if (state.health) {
      const h = us.mapHealthOverview(state.health)
      this.setData({ healthData: { ...this.data.healthData, ...h } })
    }
  },

  async refreshAll() {
    await Promise.allSettled([
      this.fetchUserMe(),
      this.fetchWalletOverview(),
      this.fetchHealthOverview(),
      this.fetchVipInfo()
    ])
  },

  async fetchUserMe() {
    try {
      const resp = await request.get('/user/me', {}, { loading: false, showError: false })
      const data = (resp && (resp.data || resp)) || null
      if (data) {
        const user = us.mapUser(data)
        this.setData({ userInfo: { ...this.data.userInfo, ...user } })
        us.merge({ user: data })
      }
    } catch (e) {
      console.warn('fetch /api/user/me 失败', e)
    }
  },

  async fetchWalletOverview() {
    try {
      let resp = null
      try {
        resp = await request.get('/wallet', {}, { loading: false, showError: false })
      } catch (e) {
        if (e && e.statusCode === 404) {
          resp = await request.get('/wallet/overview', {}, { loading: false, showError: false })
        } else {
          throw e
        }
      }
      const data = (resp && (resp.data || resp)) || null
      if (data) {
        const mapped = us.mapAssets(data)
        this.setData({ userAssets: { ...this.data.userAssets, ...mapped } })
        us.set('assets', data)
      }
    } catch (e) {
      console.warn('fetch wallet 概览失败', e)
    }
  },

  async fetchVipInfo() {
    try {
      const resp = await request.get('/user/vip', {}, { loading: false, showError: false })
      const data = (resp && (resp.data || resp)) || null
      if (data) {
        const vipInfo = us.mapVipInfo(data)
        const icons = us.computeVipIcons(vipInfo)
        this.setData({ vipInfo, ...icons })
        us.set('vip', data)
      }
    } catch (e) {
      console.warn('fetch vip info 失败', e)
    }
  },

  async fetchHealthOverview() {
    try {
      const resp = await request.get('/health/overview', {}, { loading: false, showError: false })
      const d = (resp && (resp.data || resp)) || null
      if (d) {
        const healthData = us.mapHealthOverview(d)
        this.setData({ healthData: { ...this.data.healthData, ...healthData } })
        us.set('health', d)
      }
    } catch (e) {
      console.warn('fetch 健康概览失败', e)
    }
  },

  relTime(ts) {
    const t = typeof ts === 'number' ? ts : Date.parse(ts)
    const diff = Math.max(0, Date.now() - (isNaN(t) ? Date.now() : t))
    const m = Math.floor(diff / 60000), h = Math.floor(diff / 3600000), d = Math.floor(diff / 86400000)
    if (m < 1) return '刚刚'
    if (m < 60) return `${m}分钟前`
    if (h < 24) return `${h}小时前`
    return `${d}天前`
  },

  handleAvatarClick() {
    wx.navigateTo({
      url: '/pages/profile/profile-setup/profile-setup',
      fail: (err) => {
        console.warn('跳转编辑资料页失败', err)
        wx.showToast({ title: '功能开发中', icon: 'none' })
      }
    })
  },

  navigateTo(e) {
    const url = e.currentTarget.dataset.url
    if (url) {
      wx.navigateTo({
        url,
        fail: () => wx.showToast({ title: '功能开发中', icon: 'none' })
      })
    }
  },

  navigateToSettings() {
    wx.navigateTo({
      url: '/pages/profile/settings/settings',
      fail: () => wx.showToast({ title: '功能开发中', icon: 'none' })
    })
  },

  navigateToWallet() {
    wx.navigateTo({
      url: '/pages/wallet/wallet',
      fail: () => wx.showToast({ title: '钱包开发中', icon: 'none' })
    })
  },

  navigateToVip() {
    wx.navigateTo({
      url: '/pages/profile/vip-center/vip-center',
      fail: () => wx.showToast({ title: 'VIP中心开发中', icon: 'none' })
    })
  },

  navigateToOrders() {
    wx.navigateTo({
      url: '/pages/shop/orders/orders',
      fail: () => wx.showToast({ title: '订单页开发中', icon: 'none' })
    })
  },

  navigateToFavorites() {
    wx.navigateTo({
      url: '/pages/shop/favorites/favorites',
      fail: () => wx.showToast({ title: '收藏页开发中', icon: 'none' })
    })
  },

  navigateToHealthSpace() {
    wx.navigateTo({ url: '/pages/profile/health-space/health-space' })
  },

  navigateToHealthData() {
    this.navigateToHealthSpace()
  }
})

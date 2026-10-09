Page({
  data: {
    ready: false,
    animating: true
  },

  onLoad() {
    const app = getApp()
    const isNew = app && !app.isLoggedIn ? !(app.isLoggedIn()) : false
    
    this.setData({ ready: true })
    
    setTimeout(() => {
      this.setData({ animating: false })
      wx.switchTab({ url: '/pages/aiAgent/aiAgent' })
    }, 1800)
  }
})

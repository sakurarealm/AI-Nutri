Component({
  properties: {},

  data: {
    state: 'hidden',
    personalizedMessage: '',
    messageQueue: [],
    timestamp: '' // 新增时间戳字段
    , deepLink: ''
    , _lastToken: 0
    , topPx: 100
  },

  lifetimes: {
    attached() {
      try {
        const info = wx.getSystemInfoSync();
        const safeTop = info && info.safeArea ? info.safeArea.top : 0;
        const statusTop = typeof info.statusBarHeight === 'number' ? info.statusBarHeight : 0;
        const baseTop = Math.max(safeTop, statusTop);
        const topPx = Math.round(baseTop + 72);
        this.setData({ topPx });
      } catch (e) {}

      this.audioContext = wx.createInnerAudioContext();
      this.audioContext.src = '/static/sound/notification.mp3';
      this.vibrateEnabled = true;
      const app = getApp();
      this._updateFromGlobal = () => {
        const gn = app.globalData.notification || { visible: false };
        if (!gn.visible) {
          this.setData({ state: 'hidden' });
          return;
        }
        const personalized = gn.userName ? `${gn.userName}，${gn.message}` : gn.message;
        const shouldPlay = gn.state === 'expanded' && gn.token && gn.token !== this.data._lastToken;
        this.setData({
          personalizedMessage: personalized,
          timestamp: this.formatTime(new Date()),
          state: gn.state,
          deepLink: gn.deepLink || ''
        });
        if (shouldPlay) {
          try {
            this.audioContext.play();
            if (this.vibrateEnabled) wx.vibrateShort({ type: 'medium' });
          } catch (e) {}
          this.setData({ _lastToken: gn.token });
        }
      };
      app.onEvent && app.onEvent('notificationUpdate', this._updateFromGlobal);
      this._updateFromGlobal();
    },
    detached() {
      clearTimeout(this.collapseTimer);
      clearTimeout(this.showDelayTimer);
      const app = getApp();
      app.offEvent && app.offEvent('notificationUpdate', this._updateFromGlobal);
    }
  },

  methods: {
    isTabPage(url) {
      const u = String(url || '');
      if (!u) return false;
      return [
        '/pages/aiAgent/aiAgent',
        '/pages/profile/profile',
        '/pages/digitalHuman/digitalHuman',
        '/pages/nutritionbrochure/nutritionbrochure',
        '/pages/shop/shop'
      ].some(p => u === p || u.startsWith(p + '?'));
    },

    show(newMessage, userName) {
      const personalized = userName ? `${userName}，${newMessage}` : newMessage;
      this.setData({
        personalizedMessage: personalized,
        state: 'expanded',
        timestamp: this.formatTime(new Date())
      });
    },

    collapse() {
      clearTimeout(this.collapseTimer);
      this.setData({ state: 'collapsed' });
      const app = getApp();
      app.setNotificationState && app.setNotificationState('collapsed');
    },

    expand() {
      this.setData({ state: 'expanded' });
      clearTimeout(this.collapseTimer);
      const app = getApp();
      app.setNotificationState && app.setNotificationState('expanded');
    },

    toggleState() {
      if (this.data.state === 'expanded') {
        this.collapse();
      } else if (this.data.state === 'collapsed') {
        this.expand();
      }
    },

    onBarTap() {
      wx.vibrateShort({ type: 'medium' });
      this.setData({ state: 'hidden' });
      this.data.messageQueue = []; // 清空队列，防止导航回来后又弹出
      clearTimeout(this.collapseTimer);
      clearTimeout(this.showDelayTimer);
      const app = getApp();
      app.hideNotification && app.hideNotification();
      const url = this.data.deepLink || '/pages/aiAgent/aiAgent';
      if (this.isTabPage(url)) {
        wx.switchTab({ url });
      } else {
        wx.navigateTo({ url });
      }
    },

    toggleVibrate(enabled) {
      this.vibrateEnabled = enabled;
    },

    formatTime(date) {
      const h = date.getHours().toString().padStart(2, '0');
      const m = date.getMinutes().toString().padStart(2, '0');
      return `${h}:${m}`;
    }
  }
});

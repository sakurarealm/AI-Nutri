Component({
  properties: {},

  data: {
    current: 0,
    showTabbar: true,
    tabList: [
      {
        path: '/pages/shop/shop',
        text: '商店',
        iconNormal: '/static/tabBar/shop.png',
        iconActive: '/static/tabBar/selectshop.png'
      },
      {
        path: '/pages/nutritionbrochure/nutritionbrochure',
        text: '营养册',
        iconNormal: '/static/tabBar/rise.png',
        iconActive: '/static/tabBar/selectrise.png'
      },
      {
        path: '/pages/aiAgent/aiAgent',
        text: 'AI助手',
        iconNormal: '/static/tabBar/AI.png',
        iconActive: '/static/tabBar/AI.png'
      },
      {
        path: '/pages/digitalHuman/digitalHuman',
        text: '空间',
        iconNormal: '/static/tabBar/note.png',
        iconActive: '/static/tabBar/selectnote.png'
      },
      {
        path: '/pages/profile/profile',
        text: '个人主页',
        iconNormal: '/static/tabBar/profile.png',
        iconActive: '/static/tabBar/selectprofile.png'
      }
    ]
  },

  lifetimes: {
    attached() {
      this.setCurrent();
    }
  },

  pageLifetimes: {
    show() {
      this.setCurrent();
    },
    hide() {
      this.setData({ showTabbar: false });
    }
  },

  methods: {
    setCurrent() {
      setTimeout(() => {
        const pages = getCurrentPages();
        if (pages.length > 0) {
          const page = pages[pages.length - 1];
          const pagePath = page.route;
          const index = this.data.tabList.findIndex(tab => tab.path === `/${pagePath}`);
          if (index !== -1) {
            this.setData({ current: index, showTabbar: true });
          }
        }
      }, 50);
    },

    switchTab(e) {
      const index = Number(e.currentTarget.dataset.index);
      if (!Number.isFinite(index)) return;
      if (this.data.current === index) return;
      const target = this.data.tabList[index];
      const targetPath = target && target.path ? String(target.path) : '';
      if (!targetPath) return;

      const app = getApp();
      const route = targetPath.replace(/^\/+/, '');
      const requireLogin = !!(app && app.shouldRequireLogin && app.shouldRequireLogin(route));

      const doSwitch = () => {
        wx.switchTab({
          url: targetPath,
          success: () => {
            this.setData({ current: index });
            setTimeout(() => {
              this.setCurrent();
            }, 100);
          },
          fail: (err) => {
            console.error('Tab切换失败:', err);
            const pages = getCurrentPages();
            if (pages.length > 0) {
              const currentPage = pages[pages.length - 1].route;
              const currentIndex = this.data.tabList.findIndex(tab => tab.path === `/${currentPage}`);
              this.setData({ current: currentIndex });
            }
          }
        });
      };

      if (!requireLogin) {
        doSwitch();
        return;
      }

      const logged = !!(app && app.isLoggedIn && app.isLoggedIn());
      if (logged) {
        doSwitch();
        return;
      }

      Promise.resolve()
        .then(() => (app && app.ensureAuth ? app.ensureAuth({ silent: true }) : false))
        .then((ok) => {
          if (ok) {
            doSwitch();
            return;
          }
          wx.navigateTo({ url: `/pages/login/login?auto=1&redirect=${encodeURIComponent(targetPath)}` });
        })
        .catch(() => wx.navigateTo({ url: `/pages/login/login?auto=1&redirect=${encodeURIComponent(targetPath)}` }));
    }
  }
});

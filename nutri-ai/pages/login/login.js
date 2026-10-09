Page({
  data: {
    loginMode: 'wechat', // 'wechat' 或 'phone'
    phoneNumber: '',
    verificationCode: '',
    countryCode: '+86',
    countdown: 0,
    isCountingDown: false,
    agreed: false,
    canLogin: false,
    welcomeTitle: '欢迎使用',
    welcomeSubtitle: '登录后享受更好的个性化服务体验',
    showLogin: false, // 控制登录表单显示
    startY: 0, // 触摸开始位置
    currentY: 0, // 当前触摸位置
    translateY: 0, // 登录表单的Y轴偏移
    isDragging: false, // 是否正在拖拽
    userInfo: {},
    showProfileComplete: false,
    redirect: '',
    introCards: [
      { title: '营养师', desc: '一句话目标\n一步步做到', svg: '/static/login_svg/打招呼.svg' },
      { title: '饮食', desc: '这一餐拍下\n建议即刻到', svg: '/static/login_svg/饮水饮食.svg' },
      { title: '记录', desc: '少一点记录\n多一点趋势', svg: '/static/login_svg/记录.svg' },
      { title: '人体', desc: '身体在反馈\n你会看得懂', svg: '/static/login_svg/7-心脏.svg' },
      { title: '空间', desc: '找到你的节奏\n保持就够了', svg: '/static/login_svg/加油.svg' }
    ],
    introCurrent: 0,
    svgCurrent: 0,
    svgPrev: null,
    svgTransitioning: false,
    svgIdle: true,
    svgIdlePhase: 'loop',
    svgLayers: [
      [
        { src: '/static/login_svg/打招呼.svg', cls: 'z0', idleCls: 'idle-greet' }
      ],
      [
        { src: '/static/login_svg/layers/diet_plate.svg', cls: 'z0', idleCls: '' },
        { src: '/static/login_svg/layers/diet_food.svg', cls: 'z1', idleCls: 'idle-diet-food' },
        { src: '/static/login_svg/layers/diet_bowl_outline.svg', cls: 'z2', idleCls: 'idle-diet-food' },
        { src: '/static/login_svg/layers/diet_cup.svg', cls: 'z1', idleCls: 'idle-diet-cup' },
        { src: '/static/login_svg/layers/diet_cup_outline.svg', cls: 'z2', idleCls: 'idle-diet-cup' }
      ],
      [
        { src: '/static/login_svg/layers/record_sheet.svg', cls: 'z0', idleCls: '' },
        { src: '/static/login_svg/layers/record_lines.svg', cls: 'z1', idleCls: 'idle-record-lines' },
        { src: '/static/login_svg/layers/record_pen.svg', cls: 'z2', idleCls: 'idle-record-pen' }
      ],
      [
        { src: '/static/login_svg/layers/heart_rest.svg', cls: 'z0', idleCls: '' },
        { src: '/static/login_svg/layers/heart_main.svg', cls: 'z1', idleCls: 'idle-heart-a' },
        { src: '/static/login_svg/layers/heart_second.svg', cls: 'z2', idleCls: 'idle-heart-b' }
      ],
      [
        { src: '/static/login_svg/加油.svg', cls: 'z0', idleCls: 'idle-flex' }
      ]
    ]
  },

  onLoad(options) {
    this.setData({
      showLogin: false,
      userInfo: wx.getStorageSync('userInfo') || {},
      showProfileComplete: false,
      redirect: (options && options.redirect) ? String(options.redirect || '') : '',
      introCurrent: 0,
      svgCurrent: 0,
      svgPrev: null,
      svgTransitioning: false,
      svgIdle: true,
      svgIdlePhase: 'loop'
    });
    if (options && options.auto) {
      wx.showToast({
        title: '登录可享更好服务',
        icon: 'none'
      });
    }
  },

  onShow() {
    const app = getApp();
    if (app && app.triggerEvent) app.triggerEvent('notificationUpdate');
  },

  // 显示登录表单
  showLoginForm() {
    this.setData({
      showLogin: true,
      translateY: 0,
      isDragging: false
    });
  },

  // 隐藏登录表单
  hideLoginForm() {
    this.setData({
      showLogin: false,
      translateY: 0,
      isDragging: false
    });
  },

  // 介绍区域滚动事件
  onIntroScroll(e) {
    // 可以在这里添加滚动相关的逻辑
  },

  onIntroCardChange(e) {
    const idx = e && e.detail && Number.isFinite(e.detail.current) ? e.detail.current : 0;
    this.setData({ introCurrent: idx });
    this.runSvgTransition(idx);
  },

  clearSvgTimers() {
    if (this._svgInTimer) {
      clearTimeout(this._svgInTimer);
      this._svgInTimer = null;
    }
    if (this._svgEndTimer) {
      clearTimeout(this._svgEndTimer);
      this._svgEndTimer = null;
    }
    if (this._svgIdlePhaseTimer) {
      clearTimeout(this._svgIdlePhaseTimer);
      this._svgIdlePhaseTimer = null;
    }
  },

  runSvgTransition(nextIndex) {
    const next = Number.isFinite(nextIndex) ? nextIndex : 0;
    const current = Number.isFinite(this.data.svgCurrent) ? this.data.svgCurrent : 0;
    if (next === current) return;

    this.clearSvgTimers();

    this.setData({
      svgPrev: current,
      svgCurrent: next,
      svgTransitioning: false,
      svgIdle: false
    });

    this._svgInTimer = setTimeout(() => {
      this.setData({ svgTransitioning: true });
    }, 20);

    this._svgEndTimer = setTimeout(() => {
      this.setData({
        svgPrev: null,
        svgTransitioning: false,
        svgIdle: true,
        svgIdlePhase: 'enter'
      });
      this._svgIdlePhaseTimer = setTimeout(() => {
        this.setData({ svgIdlePhase: 'loop' });
      }, 1100);
    }, 420);
  },

  // 登录表单触摸开始
  onLoginTouchStart(e) {
    this.setData({
      startY: e.touches[0].clientY,
      currentY: e.touches[0].clientY,
      isDragging: true
    });
  },

  // 登录表单触摸移动
  onLoginTouchMove(e) {
    if (!this.data.isDragging) return;
    
    const currentY = e.touches[0].clientY;
    const deltaY = currentY - this.data.startY;
    
    // 只允许向下拖拽
    if (deltaY > 0) {
      this.setData({
        currentY: currentY,
        translateY: deltaY
      });
    }
  },

  // 登录表单触摸结束
  onLoginTouchEnd(e) {
    if (!this.data.isDragging) return;
    
    const { startY, currentY, translateY } = this.data;
    const deltaY = currentY - startY;
    
    this.setData({
      isDragging: false
    });
    
    // 向下滑动超过150px时隐藏登录表单
    if (deltaY > 150) {
      this.hideLoginForm();
    } else {
      // 回弹到原位置
      this.setData({
        translateY: 0
      });
    }
  },

  // 返回上一页
  goBack: function() {
    wx.navigateBack({
      delta: 1
    });
  },

  // 切换登录模式
  switchToPhone: function() {
    this.setData({
      loginMode: 'phone',
      welcomeTitle: '手机号登录',
      welcomeSubtitle: '请输入您的手机号码完成登录'
    });
  },

  switchToWechat: function() {
    this.setData({
      loginMode: 'wechat',
      welcomeTitle: '欢迎使用',
      welcomeSubtitle: '登录后享受更好的个性化服务体验'
    });
  },

  // 微信一键登录
  wechatLogin: function() {
    const that = this;
    // 1. 先判断是否勾选协议
    if (!this.data.agreed) {
      wx.showToast({ title: '请先同意用户协议', icon: 'none' });
      return;
    }

    wx.login({
      success: (res) => {
        if (res.code) {
          wx.showLoading({ title: '登录中...' });
          
          const request = require('../../utils/request.js');
          request.post('/auth/login/wechat', {
            code: res.code
          }).then(res => {
            wx.hideLoading();
            const ok = res && (res.code === 0 || res.code === 200 || res.success === true);
            const data = ok ? (res.data || res) : null;
            
            if (data) { 
               try {
                 const app = getApp();
                 if (data.accessToken && app && app.setAuthToken) app.setAuthToken(data.accessToken);
                 if (data.refreshToken && app && app.setRefreshToken) app.setRefreshToken(data.refreshToken);
               } catch (e) {}
               if (data.accessToken) { wx.setStorageSync('token', data.accessToken); wx.setStorageSync('AUTH_TOKEN', data.accessToken); }
               if (data.refreshToken) { wx.setStorageSync('REFRESH_TOKEN', data.refreshToken); wx.setStorageSync('refreshToken', data.refreshToken); }
               const rawUser = data.user || {};
               const user = { ...rawUser, isLoggedIn: true, loginTime: Date.now() };
               wx.setStorageSync('userInfo', user);
               wx.setStorageSync('user_cache', user);
               that.setData({ userInfo: user });
               try {
                 const app = getApp();
                 app.fetchAiInboxOnce && app.fetchAiInboxOnce({ silent: false });
               } catch (e) {}

               const isDefaultNickname = !user.nickname || user.nickname === '微信用户' || String(user.nickname).includes('微信用户') || String(user.nickname).startsWith('User_');
               const isDefaultAvatar = !user.avatarUrl || String(user.avatarUrl).trim() === '' || String(user.avatarUrl).includes('default');
               const uid = that.getUidFromUser(user);
               let completedMap = {};
               try { completedMap = wx.getStorageSync('profile_completed_uids') || {}; } catch (e) { completedMap = {}; }
               const completedByLocal = !!(uid && completedMap && typeof completedMap === 'object' && completedMap[uid]);
               const hasBasicProfile = !isDefaultNickname && !isDefaultAvatar;
               const isCompleted = completedByLocal || hasBasicProfile;
               if (!isCompleted) {
                 wx.navigateTo({
                   url: '/pages/profile/profile-setup/profile-setup',
                   fail: () => wx.switchTab({ url: '/pages/profile/profile' })
                 });
                 return;
               }

               wx.showToast({ title: '登录成功', icon: 'success' });
               setTimeout(() => that.finishLoginRedirect(), 800);
            } else {
               wx.showToast({ title: res.message || '登录失败', icon: 'none' });
            }
          }).catch(err => {
            wx.hideLoading();
            console.error('Login error:', err);
            wx.showToast({ title: '登录请求失败', icon: 'none' });
          });
        } else {
          wx.showToast({ title: '获取登录凭证失败', icon: 'none' });
        }
      }
    });
  },

  getUidFromUser(user) {
    const u = user && typeof user === 'object' ? user : {};
    const uid = String(u.uid || u.id || u.userId || '').trim();
    return uid || '';
  },

  readNutritionProfileByUid(user) {
    const uid = this.getUidFromUser(user);
    if (!uid) return null;
    let map = {};
    try { map = wx.getStorageSync('nutrition_profile_by_uid') || {}; } catch (e) { map = {}; }
    if (!map || typeof map !== 'object') return null;
    const np = map[uid];
    return (np && typeof np === 'object') ? np : null;
  },

  syncNutritionProfileByUid(user) {
    const uid = this.getUidFromUser(user);
    if (!uid) return;
    const np = this.readNutritionProfileByUid(user);
    if (!np) return;
    try { wx.setStorageSync('nutrition_profile', np); } catch (e) {}
  },

  finishLoginRedirect() {
    let redirect = String(this.data.redirect || '').trim();
    if (redirect) {
      try { redirect = decodeURIComponent(redirect); } catch (e) {}
      if (redirect[0] !== '/') redirect = `/${redirect}`;
      wx.reLaunch({
        url: redirect,
        fail: () => {
          wx.navigateTo({ url: redirect, fail: () => wx.switchTab({ url: '/pages/index/index' }) });
        }
      });
      return;
    }
    wx.navigateBack({ fail: () => wx.switchTab({ url: '/pages/index/index' }) });
  },

  handleChooseAvatar() {
    const choose = () => new Promise((resolve, reject) => {
      if (typeof wx.chooseMedia === 'function') {
        wx.chooseMedia({
          count: 1,
          mediaType: ['image'],
          sourceType: ['album', 'camera'],
          sizeType: ['compressed'],
          success: resolve,
          fail: reject
        });
        return;
      }
      wx.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: resolve,
        fail: reject
      });
    });

    choose().then(res => {
      const tempFilePath = res?.tempFiles?.[0]?.tempFilePath || res?.tempFilePaths?.[0];
      if (!tempFilePath) {
        wx.showToast({ title: '未获取到图片', icon: 'none' });
        return;
      }
      try {
        const fs = wx.getFileSystemManager()
        const oldPath = this.data.userInfo && this.data.userInfo.avatarUrl
        const rawExt = String(tempFilePath).split('.').pop() || 'jpg'
        const safeExt = String(rawExt).replace(/[^a-z0-9]/ig, '').toLowerCase().slice(0, 5) || 'jpg'
        const destPath = `${wx.env.USER_DATA_PATH}/avatar_latest.${safeExt}`
        const unlinkSafe = (filePath) => new Promise((resolve) => {
          if (!filePath) { resolve(false); return }
          fs.unlink({ filePath, success: () => resolve(true), fail: () => resolve(false) })
        })
        const copyToDest = () => new Promise((resolve, reject) => {
          fs.copyFile({ srcPath: tempFilePath, destPath, success: () => resolve(true), fail: reject })
        })
        Promise.resolve()
          .then(() => unlinkSafe(destPath))
          .then(() => copyToDest())
          .then(() => {
            if (oldPath && oldPath !== destPath && String(oldPath).startsWith('wxfile://')) unlinkSafe(oldPath)
            this.setData({ 'userInfo.avatarUrl': destPath })
          })
          .catch(() => {
            wx.saveFile({
              tempFilePath,
              filePath: destPath,
              success: (r) => {
                const next = r.savedFilePath || destPath || tempFilePath
                if (oldPath && oldPath !== next && String(oldPath).startsWith('wxfile://')) unlinkSafe(oldPath)
                this.setData({ 'userInfo.avatarUrl': next })
              },
              fail: () => this.setData({ 'userInfo.avatarUrl': tempFilePath })
            })
          })
      } catch (e) {
        this.setData({ 'userInfo.avatarUrl': tempFilePath })
      }
    }).catch(() => {
      wx.showToast({ title: '选择头像失败', icon: 'none' });
    });
  },

  handleSyncWechat() {
    if (typeof wx.getUserProfile !== 'function') {
      wx.showToast({ title: '当前基础库不支持同步微信资料', icon: 'none' });
      return;
    }
    wx.getUserProfile({
      desc: '用于完善昵称与头像',
      success: (res) => {
        const user = res?.userInfo || {};
        const avatarUrl = user.avatarUrl || '';
        const nickname = user.nickName || user.nickname || '';
        this.setData({
          'userInfo.avatarUrl': avatarUrl || this.data.userInfo.avatarUrl,
          'userInfo.nickname': nickname || this.data.userInfo.nickname
        });
      },
      fail: () => wx.showToast({ title: '已取消', icon: 'none' })
    });
  },

  // 昵称输入回调
  onNicknameChange(e) {
    const nickname = e.detail.value;
    console.log('Input nickname:', nickname);
    this.setData({
      'userInfo.nickname': nickname
    });
  },

  // 提交完善信息
  submitProfile() {
    const that = this;
    const { userInfo } = this.data;
    
    if (!userInfo.nickname) {
      wx.showToast({ title: '请输入昵称', icon: 'none' });
      return;
    }
    if (!userInfo.avatarUrl) {
       wx.showToast({ title: '请选择头像', icon: 'none' });
       return;
    }
    
    wx.showLoading({ title: '保存中...' });

    // 重新获取 code 以更新用户信息 (利用登录接口更新)
    wx.login({
      success: (loginRes) => {
        if (loginRes.code) {
          const request = require('../../utils/request.js');
          const isRemoteAvatar = /^https?:\/\//.test(String(userInfo.avatarUrl || ''));
          request.post('/auth/login/wechat', {
            code: loginRes.code,
            nickname: userInfo.nickname,
            avatarUrl: isRemoteAvatar ? userInfo.avatarUrl : undefined
          }).then(res => {
            wx.hideLoading();
            const ok = res && (res.code === 0 || res.code === 200 || res.success === true);
            const data = ok ? (res.data || res) : null;
            
            if (data) {
              try {
                const app = getApp();
                if (data.accessToken && app && app.setAuthToken) app.setAuthToken(data.accessToken);
                if (data.refreshToken && app && app.setRefreshToken) app.setRefreshToken(data.refreshToken);
              } catch (e) {}
              if (data.accessToken) { wx.setStorageSync('token', data.accessToken); wx.setStorageSync('AUTH_TOKEN', data.accessToken); }
              if (data.refreshToken) { wx.setStorageSync('REFRESH_TOKEN', data.refreshToken); wx.setStorageSync('refreshToken', data.refreshToken); }
              const rawUser = data.user || {};
              const finalAvatar = userInfo.avatarUrl || rawUser.avatarUrl || rawUser.avatar;
              const user = {
                ...rawUser,
                nickname: userInfo.nickname || rawUser.nickname,
                avatarUrl: finalAvatar,
                avatar: finalAvatar,
                isLoggedIn: true,
                loginTime: Date.now()
              };
              wx.setStorageSync('userInfo', user);
              wx.setStorageSync('user_cache', user);
              that.setData({ userInfo: user });
              try {
                const app = getApp();
                app.fetchAiInboxOnce && app.fetchAiInboxOnce({ silent: false });
              } catch (e) {}
               
              that.setData({ showProfileComplete: false });
              wx.showToast({ title: '保存成功', icon: 'success' });
              
              setTimeout(() => {
                 wx.navigateTo({
                   url: '/pages/profile/profile-setup/profile-setup',
                   fail: () => wx.switchTab({ url: '/pages/profile/profile' })
                 });
              }, 1000);
            } else {
              wx.showToast({ title: res.message || '保存失败', icon: 'none' });
            }
          }).catch(err => {
            wx.hideLoading();
            console.error('Update profile error:', err);
            wx.showToast({ title: '保存失败', icon: 'none' });
          });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '登录凭证获取失败', icon: 'none' });
      }
    });
  },

  // 手机号输入
  onPhoneInput: function(e) {
    const phoneNumber = e.detail.value;
    this.setData({
      phoneNumber: phoneNumber
    });
    this.updateLoginButtonState();
  },

  // 验证码输入
  onVerificationInput: function(e) {
    const verificationCode = e.detail.value;
    this.setData({
      verificationCode: verificationCode
    });
    this.updateLoginButtonState();
  },

  // 验证手机号格式
  isValidPhoneNumber(phone) {
    const phoneRegex = /^1[3-9]\d{9}$/;
    return phoneRegex.test(phone);
  },

  // 发送验证码
  sendVerificationCode() {
    if (this.data.isCountingDown) return;
    const { phoneNumber } = this.data;
    if (!this.isValidPhoneNumber(phoneNumber)) {
      wx.showToast({
        title: '请输入正确的手机号',
        icon: 'none'
      });
      return;
    }
    wx.showLoading({ title: '发送中...' });
    setTimeout(() => {
      wx.hideLoading();
      wx.showToast({ title: '验证码已发送', icon: 'success' });
      this.startCountdown();
    }, 1000);
  },

  // 开始倒计时
  startCountdown() {
    this.setData({ isCountingDown: true, countdown: 60 });
    const timer = setInterval(() => {
      const next = this.data.countdown - 1;
      if (next <= 0) {
        clearInterval(timer);
        this.setData({ isCountingDown: false, countdown: 0 });
      } else {
        this.setData({ countdown: next });
      }
    }, 1000);
  },

  handleLogin() {
    if (!this.data.canLogin) return;
    const { phoneNumber, verificationCode } = this.data;
    if (!this.isValidPhoneNumber(phoneNumber) || !verificationCode || verificationCode.length < 4) {
      wx.showToast({ title: '请填写正确信息', icon: 'none' });
      return;
    }
    wx.showLoading({ title: '登录中...' });
    setTimeout(() => {
      wx.hideLoading();
      wx.showToast({ title: '登录成功', icon: 'success' });
      wx.setStorageSync('userInfo', { phone: phoneNumber, isLoggedIn: true, loginTime: Date.now() });
      setTimeout(() => {
        wx.navigateBack({
          fail: () => {
            wx.switchTab({ url: '/pages/index/index' });
          }
        });
      }, 1200);
    }, 1200);
  },

  onAgreementChange(e) {
    const agreed = e.detail.value.length > 0;
    this.setData({ agreed });
    this.updateLoginButtonState();
  },

  // 显示国家代码选择器
  showCountryPicker() {
    wx.showActionSheet({
      itemList: ['+86 中国大陆', '+852 香港', '+853 澳门', '+886 台湾'],
      success: (res) => {
        const codes = ['+86', '+852', '+853', '+886'];
        console.log('选择了国家代码:', codes[res.tapIndex]);
        // 这里可以更新国家代码
      }
    });
  },

  // 显示隐私政策
  showPrivacyPolicy() {
    wx.showModal({
      title: 'AI易面隐私保护声明',
      content: '这里是隐私保护声明的内容...',
      showCancel: false,
      confirmText: '我知道了'
    });
  },

  // 显示用户协议
  showUserAgreement() {
    wx.showModal({
      title: 'AI易面用户协议',
      content: '这里是用户协议的内容...',
      showCancel: false,
      confirmText: '我知道了'
    });
  },
  // 更新登录按钮状态
  updateLoginButtonState() {
    let canLogin = false;
    if (this.data.loginMode === 'wechat') {
      canLogin = this.data.agreed;
    } else if (this.data.loginMode === 'phone') {
      canLogin = this.isValidPhoneNumber(this.data.phoneNumber) &&
                 !!this.data.verificationCode && this.data.verificationCode.length >= 4 &&
                 this.data.agreed;
    }
    this.setData({ canLogin });
  },

  // 跳过登录
  skipLogin() {
    wx.switchTab({ url: '/pages/aiAgent/aiAgent' });
  }
});

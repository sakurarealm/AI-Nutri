const request = require('../../../utils/request.js');
const us = require('../../../utils/userState.js');

const PROFILE_SETUP_DRAFT_KEY = 'profile_setup_draft';

Page({
  data: {
    avatarUrl: '',
    nickname: '',
    genderOptions: ['请选择', '男', '女'],
    genderIndex: 0,
    age: '',
    height: '',
    weight: '',
    goalOptions: ['减脂', '增肌', '保持健康', '控糖', '孕期/哺乳'],
    goalIndex: -1,
    activityOptions: ['久坐', '轻度活动', '中度活动', '高强度活动'],
    activityIndex: -1,
    dietOptions: ['无', '素食', '蛋奶素', '低碳', '低脂', '清真'],
    dietIndex: -1,
    allergies: ''
  },

  async onLoad() {
    const state = us.load()
    const user = (state && state.user) || wx.getStorageSync('userInfo') || {}
    const nutrition = (state && state.nutritionProfile) || wx.getStorageSync('nutrition_profile') || {}
    const draft = wx.getStorageSync(PROFILE_SETUP_DRAFT_KEY) || {};

    this.setData({
      avatarUrl: (draft && draft.avatarUrl) || user.avatarUrl || user.avatar || '',
      nickname: (draft && String(draft.nickname || '').trim()) || user.nickname || '',
      genderIndex: draft.genderIndex != null ? draft.genderIndex : (nutrition.genderIndex != null ? nutrition.genderIndex : 0),
      age: draft.age != null && String(draft.age) !== '' ? String(draft.age) : (nutrition.age != null ? String(nutrition.age) : ''),
      height: draft.height != null && String(draft.height) !== '' ? String(draft.height) : (nutrition.height != null ? String(nutrition.height) : ''),
      weight: draft.weight != null && String(draft.weight) !== '' ? String(draft.weight) : (nutrition.weight != null ? String(nutrition.weight) : ''),
      goalIndex: draft.goalIndex != null ? draft.goalIndex : (nutrition.goalIndex != null ? nutrition.goalIndex : -1),
      activityIndex: draft.activityIndex != null ? draft.activityIndex : (nutrition.activityIndex != null ? nutrition.activityIndex : -1),
      dietIndex: draft.dietIndex != null ? draft.dietIndex : (nutrition.dietIndex != null ? nutrition.dietIndex : -1),
      allergies: draft.allergies != null ? String(draft.allergies) : (nutrition.allergies || '')
    });

    try { await this.tryLoadRemoteProfile(); } catch (e) {}
  },

  onHide() {
    this.persistDraftNow();
  },

  onUnload() {
    this.persistDraftNow();
  },

  getUidFromUser(user) {
    const u = user && typeof user === 'object' ? user : {};
    const uid = String(u.uid || u.id || u.userId || '').trim();
    return uid || '';
  },

  buildDraft() {
    const {
      avatarUrl,
      nickname,
      genderIndex,
      age,
      height,
      weight,
      goalIndex,
      activityIndex,
      dietIndex,
      allergies
    } = this.data;
    return {
      avatarUrl: avatarUrl || '',
      nickname: nickname || '',
      genderIndex,
      age: age || '',
      height: height || '',
      weight: weight || '',
      goalIndex,
      activityIndex,
      dietIndex,
      allergies: allergies || '',
      updatedAt: Date.now()
    };
  },

  scheduleDraftPersist() {
    if (this._draftTimer) clearTimeout(this._draftTimer);
    this._draftTimer = setTimeout(() => this.persistDraftNow(), 400);
  },

  persistDraftNow() {
    if (this._draftTimer) {
      clearTimeout(this._draftTimer);
      this._draftTimer = null;
    }
    try {
      wx.setStorageSync(PROFILE_SETUP_DRAFT_KEY, this.buildDraft());
    } catch (e) {}
  },

  clearDraft() {
    if (this._draftTimer) {
      clearTimeout(this._draftTimer);
      this._draftTimer = null;
    }
    try { wx.removeStorageSync(PROFILE_SETUP_DRAFT_KEY); } catch (e) {}
  },

  getAuthToken() {
    try {
      const app = getApp && getApp();
      if (app && typeof app.getAuthToken === 'function') return String(app.getAuthToken() || '').trim();
    } catch (e) {}
    try {
      return String(wx.getStorageSync('token') || wx.getStorageSync('AUTH_TOKEN') || '').trim();
    } catch (e) {
      return '';
    }
  },

  toYmd(d) {
    const pad2 = (n) => (n < 10 ? '0' + n : String(n));
    const y = d.getFullYear();
    const m = pad2(d.getMonth() + 1);
    const day = pad2(d.getDate());
    return `${y}-${m}-${day}`;
  },

  birthdayFromAge(age) {
    const n = Number(age);
    if (!n || n <= 0) return '';
    const d = new Date();
    d.setFullYear(d.getFullYear() - Math.floor(n));
    return this.toYmd(d);
  },

  ageFromBirthday(birthday) {
    const s = String(birthday || '').trim();
    if (!s) return '';
    const parts = s.split('-').map((x) => Number(x));
    if (parts.length !== 3 || parts.some((x) => !x || x <= 0)) return '';
    const [y, m, d] = parts;
    const bd = new Date(y, m - 1, d);
    if (Number.isNaN(bd.getTime())) return '';
    const now = new Date();
    let years = now.getFullYear() - bd.getFullYear();
    const md = (now.getMonth() + 1) * 100 + now.getDate();
    const bdmd = (bd.getMonth() + 1) * 100 + bd.getDate();
    if (md < bdmd) years -= 1;
    if (years < 0) years = 0;
    return years ? String(years) : '';
  },

  genderIndexFromSex(sex) {
    const s = String(sex || '').trim().toLowerCase();
    if (s === 'male' || s === 'm' || s === 'man' || s === '男') return 1;
    if (s === 'female' || s === 'f' || s === 'woman' || s === '女') return 2;
    return 0;
  },

  sexFromGenderIndex(genderIndex) {
    const idx = Number(genderIndex);
    if (idx === 1) return 'male';
    if (idx === 2) return 'female';
    return null;
  },

  async tryLoadRemoteProfile() {
    const token = this.getAuthToken();
    if (!token) return;

    const draft = wx.getStorageSync(PROFILE_SETUP_DRAFT_KEY) || {};
    const draftHasBasics =
      (draft && (draft.genderIndex > 0))
      || (draft && String(draft.age || '').trim() !== '')
      || (draft && String(draft.height || '').trim() !== '')
      || (draft && String(draft.weight || '').trim() !== '');
    if (draftHasBasics) return;

    const resp = await request.get('/health/profile', {}, { loading: false, showError: false });
    const p = resp && resp.data ? resp.data : null;
    if (!p) return;

    const next = {};
    if (p.sex != null) next.genderIndex = this.genderIndexFromSex(p.sex);
    if (p.birthday != null) next.age = this.ageFromBirthday(p.birthday);
    if (p.heightCm != null) next.height = String(p.heightCm);
    if (p.lastWeightKg != null) next.weight = String(p.lastWeightKg);
    if (Object.keys(next).length) this.setData(next);
  },

  async saveRemoteProfile({ genderIndex, age, height, weight }) {
    const token = this.getAuthToken();
    if (!token) throw new Error('未登录');

    const birthday = this.birthdayFromAge(age);
    const payload = {
      heightCm: height != null && String(height).trim() !== '' ? Number(height) : undefined,
      lastWeightKg: weight != null && String(weight).trim() !== '' ? Number(weight) : undefined,
      sex: this.sexFromGenderIndex(genderIndex),
      birthday: birthday || undefined
    };
    await request.post('/health/profile', payload, { loading: false });
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
        const oldPath = this.data.avatarUrl
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
            this.setData({ avatarUrl: destPath })
            this.scheduleDraftPersist();
          })
          .catch(() => {
            wx.saveFile({
              tempFilePath,
              filePath: destPath,
              success: (r) => {
                const next = r.savedFilePath || destPath || tempFilePath
                if (oldPath && oldPath !== next && String(oldPath).startsWith('wxfile://')) unlinkSafe(oldPath)
                this.setData({ avatarUrl: next })
                this.scheduleDraftPersist();
              },
              fail: () => { this.setData({ avatarUrl: tempFilePath }); this.scheduleDraftPersist(); }
            })
          })
      } catch (e) {
        this.setData({ avatarUrl: tempFilePath })
        this.scheduleDraftPersist();
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
        const nickname = String(user.nickName || user.nickname || '').trim();
        this.setData({
          avatarUrl: avatarUrl || this.data.avatarUrl,
          nickname: nickname || this.data.nickname
        });
        this.scheduleDraftPersist();
      },
      fail: () => wx.showToast({ title: '已取消', icon: 'none' })
    });
  },

  onNicknameInput(e) {
    const nickname = String(e?.detail?.value || '');
    this.setData({ nickname });
    this.scheduleDraftPersist();
  },

  onGenderChange(e) {
    this.setData({ genderIndex: Number(e.detail.value) });
    this.scheduleDraftPersist();
  },

  onAgeInput(e) {
    this.setData({ age: e.detail.value });
    this.scheduleDraftPersist();
  },

  onHeightInput(e) {
    this.setData({ height: e.detail.value });
    this.scheduleDraftPersist();
  },

  onWeightInput(e) {
    this.setData({ weight: e.detail.value });
    this.scheduleDraftPersist();
  },

  onGoalChange(e) {
    this.setData({ goalIndex: Number(e.detail.value) });
    this.scheduleDraftPersist();
  },

  onActivityChange(e) {
    this.setData({ activityIndex: Number(e.detail.value) });
    this.scheduleDraftPersist();
  },

  onDietChange(e) {
    this.setData({ dietIndex: Number(e.detail.value) });
    this.scheduleDraftPersist();
  },

  onAllergiesInput(e) {
    this.setData({ allergies: e.detail.value });
    this.scheduleDraftPersist();
  },

  async onSubmit() {
    const {
      avatarUrl,
      nickname: rawNickname,
      genderIndex,
      age,
      height,
      weight,
      goalIndex,
      activityIndex,
      dietIndex,
      allergies
    } = this.data;
    const nickname = String(rawNickname || '').trim();

    if (!nickname) {
      wx.showToast({ title: '请填写昵称', icon: 'none' });
      return;
    }
    if (!avatarUrl) {
      wx.showToast({ title: '请选择头像', icon: 'none' });
      return;
    }
    if (genderIndex <= 0) {
      wx.showToast({ title: '请选择性别', icon: 'none' });
      return;
    }
    if (!age || Number(age) <= 0) {
      wx.showToast({ title: '请填写年龄', icon: 'none' });
      return;
    }
    if (!height || Number(height) <= 0) {
      wx.showToast({ title: '请填写身高', icon: 'none' });
      return;
    }
    if (!weight || Number(weight) <= 0) {
      wx.showToast({ title: '请填写体重', icon: 'none' });
      return;
    }
    if (goalIndex < 0) {
      wx.showToast({ title: '请选择目标', icon: 'none' });
      return;
    }
    if (activityIndex < 0) {
      wx.showToast({ title: '请选择活动水平', icon: 'none' });
      return;
    }
    if (dietIndex < 0) {
      wx.showToast({ title: '请选择饮食偏好', icon: 'none' });
      return;
    }

    const existingUser = wx.getStorageSync('userInfo') || {};
    const localAvatar = avatarUrl || existingUser.avatarUrl || existingUser.avatar || '';
    const localUser = {
      ...existingUser,
      nickname: nickname || existingUser.nickname,
      avatarUrl: localAvatar,
      avatar: localAvatar,
      isLoggedIn: true,
      loginTime: existingUser.loginTime || Date.now()
    };
    wx.setStorageSync('userInfo', localUser);
    wx.setStorageSync('user_cache', localUser);
    wx.setStorageSync('userProfile', localUser);

    const nutritionProfile = {
      genderIndex,
      gender: this.data.genderOptions[genderIndex],
      age: Number(age),
      height: Number(height),
      weight: Number(weight),
      goalIndex,
      goal: this.data.goalOptions[goalIndex],
      activityIndex,
      activity: this.data.activityOptions[activityIndex],
      dietIndex,
      diet: this.data.dietOptions[dietIndex],
      allergies: allergies || '',
      updatedAt: Date.now()
    };
    wx.setStorageSync('nutrition_profile', nutritionProfile);

    us.merge({ user: localUser, nutritionProfile })

    try {
      const uid = this.getUidFromUser(localUser);
      if (uid) {
        let map = {};
        try { map = wx.getStorageSync('nutrition_profile_by_uid') || {}; } catch (e) { map = {}; }
        if (!map || typeof map !== 'object') map = {};
        map[uid] = nutritionProfile;
        wx.setStorageSync('nutrition_profile_by_uid', map);
      }
    } catch (e) {}

    try {
      const uid = this.getUidFromUser(localUser);
      if (uid) {
        let map = {};
        try { map = wx.getStorageSync('profile_completed_uids') || {}; } catch (e) { map = {}; }
        if (!map || typeof map !== 'object') map = {};
        map[uid] = true;
        wx.setStorageSync('profile_completed_uids', map);
      }
    } catch (e) {}

    wx.showLoading({ title: '保存中...' });

    try {
      const existingToken = this.getAuthToken();
      const loginRes = await new Promise((resolve, reject) => {
        wx.login({ success: resolve, fail: reject });
      });

      if (!loginRes.code) {
        if (existingToken) {
          await this.saveRemoteProfile({ genderIndex, age, height, weight });
        }
      } else {
        const isRemoteAvatar = /^https?:\/\//.test(String(avatarUrl || ''));
        const resp = await request.post('/auth/login/wechat', {
          code: loginRes.code,
          nickname,
          avatarUrl: isRemoteAvatar ? avatarUrl : undefined
        }, { loading: false });

        const ok = resp && (resp.code === 0 || resp.code === 200 || resp.success === true);
        const data = ok ? (resp.data || resp) : null;

        if (data) {
          try {
            const app = getApp();
            if (data.accessToken && app && app.setAuthToken) app.setAuthToken(data.accessToken);
            if (data.refreshToken && app && app.setRefreshToken) app.setRefreshToken(data.refreshToken);
          } catch (e) {}
          if (data.accessToken) { wx.setStorageSync('token', data.accessToken); wx.setStorageSync('AUTH_TOKEN', data.accessToken); }
          if (data.refreshToken) { wx.setStorageSync('REFRESH_TOKEN', data.refreshToken); wx.setStorageSync('refreshToken', data.refreshToken); }
          const rawUser = data.user || {};
          const finalAvatar = avatarUrl || rawUser.avatarUrl || rawUser.avatar;
          const user = {
            ...rawUser,
            nickname: nickname || rawUser.nickname,
            avatarUrl: finalAvatar,
            avatar: finalAvatar,
            isLoggedIn: true,
            loginTime: Date.now()
          };
          wx.setStorageSync('userInfo', user);
          wx.setStorageSync('user_cache', user);
          wx.setStorageSync('userProfile', user);
          us.merge({ user, nutritionProfile })
        }
        try {
          const uid = this.getUidFromUser(wx.getStorageSync('userInfo') || {});
          if (uid) {
            let map = {};
            try { map = wx.getStorageSync('nutrition_profile_by_uid') || {}; } catch (e) { map = {}; }
            if (!map || typeof map !== 'object') map = {};
            map[uid] = nutritionProfile;
            wx.setStorageSync('nutrition_profile_by_uid', map);
          }
        } catch (e) {}

        await this.saveRemoteProfile({ genderIndex, age, height, weight });
      }

      wx.hideLoading();
      wx.showToast({ title: '已保存', icon: 'success' });
      this.clearDraft();
      setTimeout(() => {
        wx.switchTab({ url: '/pages/nutritionbrochure/nutritionbrochure' });
      }, 600);
    } catch (e) {
      wx.hideLoading();
      wx.showToast({ title: '保存失败，请重试', icon: 'none' });
    }
  },

  onSkip() {
    this.persistDraftNow();
    wx.switchTab({ url: '/pages/nutritionbrochure/nutritionbrochure' });
  }
});

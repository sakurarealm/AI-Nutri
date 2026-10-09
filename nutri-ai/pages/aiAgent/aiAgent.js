// pages/aiAgent/aiAgent.js

const tts = require('../../utils/tts.js')
const stt = require('../../utils/stt.js')
const request = require('../../utils/request.js')
const { uploadFile } = require('../../utils/request')
const { compressOrPassthrough } = require('../../utils/imageCompress.js')
const networkMonitor = require('../../utils/networkMonitor.js')
const us = require('../../utils/userState.js')
const AI_INBOX_KEY = 'AI_CHAT_INBOX'
const CHAT_CACHE_PREFIX = 'CHAT_MESSAGES_'
const CHAT_CURSOR_PREFIX = 'CHAT_HISTORY_CURSOR_'
const FACT_CACHE_PREFIX = 'FACT_RECORDS_'
const NUTRITION_MEAL_DATA_KEY = 'nutrition_meal_data_by_member'
const DAILY_SUMMARY_ARCHIVE_KEY = 'DAILY_SUMMARY_ARCHIVE_V1'

const RUNTIME_STATE_KEY = 'AI_AGENT_RUNTIME_STATE'
const PROFILE_SETUP_DRAFT_KEY = 'profile_setup_draft'
const AIAGENT_ONBOARDING_FORCE_KEY_PREFIX = 'AIAGENT_ONBOARDING_FORCE_'
const AIAGENT_ONBOARDING_DONE_KEY_PREFIX = 'AIAGENT_ONBOARDING_DONE_'
const AIAGENT_ONBOARDING_STATE_KEY_PREFIX = 'AIAGENT_ONBOARDING_STATE_'
const ONBOARDING_SESSION_ID_PREFIX = 'onb_'
const ONBOARDING_KICKOFF_TOKEN = '__ONBOARDING_KICKOFF__'

const ONBOARDING_GOAL_OPTIONS = ['减脂', '增肌', '保持健康', '控糖', '孕期/哺乳']
const ONBOARDING_ACTIVITY_OPTIONS = ['久坐', '轻度活动', '中度活动', '高强度活动']
const ONBOARDING_DIET_OPTIONS = ['无', '素食', '蛋奶素', '低碳', '低脂', '清真']

const VOICE_ONBOARDING_STEPS = [
  { step: 'INTRO', label: '欢迎', question: '你好呀，我是你的专属AI营养师小云。接下来我会问你几个简单的问题，帮你建立专属的健康档案。你只需要像朋友聊天一样回答我就好，准备好了吗？' },
  { step: 'ASK_GENDER', label: '性别', question: '第一个问题，你的性别是男还是女呢？' },
  { step: 'ASK_AGE', label: '年龄', question: '你今年多大啦？' },
  { step: 'ASK_HEIGHT', label: '身高', question: '你的身高是多少厘米呢？' },
  { step: 'ASK_WEIGHT', label: '体重', question: '现在体重是多少公斤呢？' },
  { step: 'ASK_GOAL', label: '目标', question: '你的健康目标是什么？比如减脂、增肌、保持健康、或者控糖？' },
  { step: 'ASK_ACTIVITY', label: '活动量', question: '你平时的活动量怎么样？久坐比较多，还是有轻度活动，或者经常运动？' },
  { step: 'ASK_DIET', label: '饮食偏好', question: '有没有什么饮食偏好？比如素食、蛋奶素、低碳、低脂，或者没有特别的？' },
  { step: 'ASK_ALLERGIES', label: '过敏', question: '最后一个问题，有没有什么忌口或者过敏的食物？没有的话直接说没有就行。' }
]

const AI_CHAT_AVATAR_URL = 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/aiagent/ai-nutri.png'

const CHAT_INITIAL_PULL_LIMIT = 20
const CHAT_CACHE_MAX_ITEMS = 24

const SystemMessageType = {
  CHAT: 'CHAT',
  PROACTIVE_INQUIRY: 'PROACTIVE_INQUIRY',
  SYSTEM_ACK: 'SYSTEM_ACK',
  IMAGE_EVENT: 'IMAGE_EVENT',
  IMAGE_ANALYSIS: 'IMAGE_ANALYSIS',
  FACT_RECORD: 'FACT_RECORD',
  PLAN_FEEDBACK: 'PLAN_FEEDBACK',
  CAPABILITY_GUIDE: 'CAPABILITY_GUIDE',
  ONBOARDING_GATE: 'ONBOARDING_GATE',
  FEATURE_GUIDE: 'FEATURE_GUIDE',
  REMINDER: 'REMINDER',
  SUMMARY: 'SUMMARY'
}
Page({
  data: {
    statusBarHeight: 0,
    safeAreaBottom: 0,
    current: 0,
    contentBottomPaddingPx: 260,
    inputBarBottomPx: 60,
    
    timeline: [],
    renderTimeline: [],
    factRecords: [],
    runtimeState: null,
    runtimeStateUpdatedAt: 0,
    capabilityBlocked: false,
    capabilityGuideKey: '',
    inputText: '',
    isLoading: false,
    scrollIntoView: '',
    scrollWithAnimation: false,
    refresherTriggered: false,
    loadingOlderHistory: false,
    historyMinServerId: '',
    historyTailLoaded: false,
    historyHasMore: true,
    quickReplies: [],
    quickReplyAnchorId: '',
    
    userInfo: null,
    isVoiceMode: false,
    isHoldingVoice: false,
    voiceRecordDuration: 0,

    // AI电话
    isCalling: false,
    callStatus: 'idle', // connecting | connected | ended
    callDuration: 0,
    callDurationStr: '00:00',
    callAvatarImageUrl: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/aiagent/ai-nutri.png',
    callAvatarVideoUrl: 'cloud://prod-8gp55z7w28be1fad.7072-prod-8gp55z7w28be1fad-1395409956/aiagent/豆包 (1).mp4',
    isUserSpeaking: false,
    isAISpeaking: false,
    userSpectrum: new Array(24).fill(10),
    aiSpectrum: new Array(24).fill(10),
    userColors: [],
    _userTargetBars: new Array(24).fill(10),
    // 工作流入口
    workflowItems: [],
    managerActions: [],
    // 控制状态
    isMicMuted: false,
    isSpeakerOn: true,
    callLastUserText: '',
    callLastAiText: '',
    
    // 数字人常驻
    dhAvatarUrl: AI_CHAT_AVATAR_URL,
    dhGreeting: '',
    
    // 万用键智能建议
    showSmartSuggestions: true,
    suggestionScenario: 'greet',
    lastAiText: '',
    userHasMessaged: false,
    
    // AI分析动画
    showAnalysisAnimation: false,
    analysisText: '',
    chatActionIcons: {
      copy: '/static/aiAgentPage/copy2.png',
      tts: '/static/aiAgentPage/readaloud.png',
      share: '/static/aiAgentPage/share.png'
    }
    ,
    ttsActiveId: '',
    ttsActivePhase: '',
    sessionId: ''
    ,
    apiBase: ''
    
    // 语音入网
    ,
    isVoiceOnboarding: false,
    voiceOnboardingStep: '',
    voiceOnboardingProgress: 0,
    voiceOnboardingTotal: 9,
    voiceOnboardingStepLabel: ''
  },

  onChipTap(e) {
    const url = e.currentTarget.dataset.url;
    wx.navigateTo({ url });
  },

  onManagerActionTap(e) {
    const id = e.currentTarget.dataset.id;
    const list = Array.isArray(this.data.managerActions) ? this.data.managerActions : [];
    const action = list.find(it => String(it.id) === String(id));
    if (!action) return;

    const type = action.action || 'navigate';
    if (type === 'send') {
      const text = String(action.text || '').trim();
      if (!text || this.data.isLoading) return;
      this.sendTextMessage(text);
      return;
    }

    const url = String(action.url || '').trim();
    if (!url) return;
    wx.navigateTo({ url });
  },

  navigateToPlanTimeline() {
    wx.navigateTo({ url: '/pages/planTimeline/planTimeline' });
  },

  onDhAvatarError() {
    this.setData({ dhAvatarUrl: '/images/default_avatar.svg' })
  },

  updateDhGreeting() {
    const h = new Date().getHours()
    let greeting = '您好，我是您的专属AI营养师'
    if (h < 9) {
      greeting = '早安呀，新的一天开始啦。昨晚睡得好吗？'
    } else if (h < 12) {
      greeting = '上午好，别忘了吃一份营养早餐哦。'
    } else if (h < 14) {
      greeting = '中午好，午餐吃得还满意吗？'
    } else if (h < 18) {
      greeting = '下午好，来点健康加餐补充能量吧。'
    } else if (h < 21) {
      greeting = '晚上好，晚餐要清淡一点哦。'
    } else {
      greeting = '夜深了，早点休息，明天继续加油。'
    }
    this.setData({ dhGreeting: greeting })
  },

  updateSuggestionState() {
    const { userHasMessaged, timeline } = this.data
    if (!userHasMessaged) {
      this.setData({ suggestionScenario: 'greet', showSmartSuggestions: true })
      return
    }
    const items = Array.isArray(timeline) ? timeline : []
    const lastAi = [...items].reverse().find(it => (it || {}).role === 'ai')
    const lastAiText = lastAi ? (lastAi.text || '').slice(-160) : ''
    this.setData({
      suggestionScenario: 'followup',
      lastAiText,
      showSmartSuggestions: true
    })
  },

  onSmartSuggestionSelect(e) {
    const text = (e && e.detail && e.detail.text) || ''
    if (!text.trim() || this.data.isLoading) return
    this.setData({ showSmartSuggestions: false, userHasMessaged: true })
    this.sendTextMessage(text.trim())
  },

  onSmartSuggestionDismiss() {
    this.setData({ showSmartSuggestions: false })
  },

  onGoBrochure() {
    if (this.data.showAnalysisAnimation) return;
    try {
      wx.switchTab({ url: '/pages/nutritionbrochure/nutritionbrochure' });
    } catch (e) {}
  },

  updateManagerActionsForMessage() {
    this.setData({ managerActions: [] });
  },

  getCachedUserInfo() {
    const state = us.load()
    const raw = (state && state.user) || {}
    return this.normalizeUserInfo(raw);
  },

  getUserKey() {
    const u = this.data.userInfo || this.getCachedUserInfo() || {};
    const id = String((u && (u.id || u.userId || u.uid)) != null ? (u.id || u.userId || u.uid) : '').trim();
    return id || 'guest';
  },

  getOnboardingForceKey() {
    return `${AIAGENT_ONBOARDING_FORCE_KEY_PREFIX}${this.getUserKey()}`;
  },

  getOnboardingDoneKey() {
    return `${AIAGENT_ONBOARDING_DONE_KEY_PREFIX}${this.getUserKey()}`;
  },

  getOnboardingStateKey() {
    return `${AIAGENT_ONBOARDING_STATE_KEY_PREFIX}${this.getUserKey()}`;
  },

  getSessionStorageKey(mode) {
    const m = String(mode || this._sessionMode || 'default').trim();
    if (m === 'onboarding') return `CHAT_SESSION_ID_ONBOARDING_${this.getUserKey()}`;
    return 'CHAT_SESSION_ID';
  },

  initSessionMode(options) {
    const opt = options && typeof options === 'object' ? options : {};
    const q = String(opt.onboarding || opt.debugOnboarding || opt.forceOnboarding || '').trim();
    const forcedByQuery = q === '1' || q === 'true';
    let forcedAlways = false;
    try { forcedAlways = !!wx.getStorageSync(this.getOnboardingForceKey()); } catch (e) { forcedAlways = false; }
    this._sessionMode = (forcedByQuery || forcedAlways) ? 'onboarding' : 'default';
  },

  isOnboardingMode() {
    return String(this._sessionMode || 'default') === 'onboarding';
  },

  clearSessionCacheByMode(mode, { clearSessionId } = {}) {
    const key = this.getSessionStorageKey(mode);
    let sid = '';
    try { sid = String(wx.getStorageSync(key) || '').trim(); } catch (e) { sid = ''; }
    if (sid) {
      try { wx.removeStorageSync(`${CHAT_CACHE_PREFIX}${sid}`); } catch (e) {}
      try { wx.removeStorageSync(`${CHAT_CURSOR_PREFIX}${sid}`); } catch (e) {}
      try { wx.removeStorageSync(`${FACT_CACHE_PREFIX}${sid}`); } catch (e) {}
    }
    if (clearSessionId) {
      try { wx.removeStorageSync(key); } catch (e) {}
    }
  },

  reloadSessionView({ silent } = {}) {
    const patch = {
      timeline: [],
      renderTimeline: [],
      quickReplies: [],
      quickReplyAnchorId: '',
      scrollIntoView: '',
      isLoading: false,
      loadingOlderHistory: false,
      historyMinServerId: '',
      historyTailLoaded: false,
      historyHasMore: true
    };
    this.setData(patch);
    this.ensureSessionId();
    this.restoreChatFromCache();
    this.restoreFactFromCache();
    this.ensureChatCursorFromCache();
    this.syncChatHistoryFromServer({ silent: true }).finally(() => {
      setTimeout(() => {
        this.drainInboxStorageToTimeline();
        this.ensureWelcomeMessage();
        this.scrollToBottomLater();
      }, 80);
    });
  },

  readOnboardingState() {
    try {
      const v = wx.getStorageSync(this.getOnboardingStateKey());
      if (v && typeof v === 'object') return v;
    } catch (e) {}
    return null;
  },

  writeOnboardingState(state) {
    try { wx.setStorageSync(this.getOnboardingStateKey(), state || {}); } catch (e) {}
  },

  clearOnboardingState() {
    try { wx.removeStorageSync(this.getOnboardingStateKey()); } catch (e) {}
  },

  getProfileSetupDraft() {
    try {
      const v = wx.getStorageSync(PROFILE_SETUP_DRAFT_KEY) || {};
      return v && typeof v === 'object' ? v : {};
    } catch (e) {
      return {};
    }
  },

  persistProfileSetupDraft(patch) {
    const prev = this.getProfileSetupDraft();
    const next = { ...(prev || {}), ...(patch || {}), updatedAt: Date.now() };
    try { wx.setStorageSync(PROFILE_SETUP_DRAFT_KEY, next); } catch (e) {}
    return next;
  },

  createLocalChatVm({ role, text }) {
    const now = Date.now();
    const u = this.data.userInfo || {};
    const userAvatar = u.avatarUrl || u.avatar || '/images/default_avatar.svg';
    const timeText = this.getCurrentTime();
    const sessionId = this.data.sessionId || '';
    const userId = this.getNumericUserId();
    const id = `${role === 'user' ? 'local_chat_user' : 'local_chat_ai'}_${now.toString(36)}_${Math.random().toString(36).slice(2, 7)}`;
    return {
      id,
      source: 'chat',
      role,
      systemMessageType: SystemMessageType.CHAT,
      text: String(text || ''),
      ts: now,
      sessionId,
      userId,
      timeText,
      avatar: role === 'user' ? userAvatar : AI_CHAT_AVATAR_URL
    };
  },

  pushAssistantText(text, { quickReplies } = {}) {
    const vm = this.createLocalChatVm({ role: 'assistant', text });
    this.pushTimelineItem(vm);
    if (Array.isArray(quickReplies)) {
      const list = quickReplies.map(s => String(s || '').trim()).filter(Boolean).slice(0, 6);
      if (list.length) {
        this.setData({ quickReplies: list, quickReplyAnchorId: vm.id });
      } else {
        this.setData({ quickReplies: [], quickReplyAnchorId: '' });
      }
    }
    return vm.id;
  },

  pushUserText(text) {
    const vm = this.createLocalChatVm({ role: 'user', text });
    this.pushTimelineItem(vm);
    return vm.id;
  },

  waitMs(ms) {
    const n = Number(ms || 0);
    return new Promise((resolve) => setTimeout(resolve, Number.isFinite(n) ? Math.max(0, n) : 0));
  },

  enqueueOnboardingMessage(task) {
    const fn = typeof task === 'function' ? task : null;
    if (!fn) return;
    if (!this._onboardingMessageQueue) this._onboardingMessageQueue = Promise.resolve();
    this._onboardingMessageQueue = this._onboardingMessageQueue.then(fn).catch(() => {});
  },

  onboardingSay(text, { delayMs, thinkingText } = {}) {
    const t = String(text || '').trim();
    if (!t) return;
    const d = Number.isFinite(Number(delayMs)) ? Number(delayMs) : 520;
    this.enqueueOnboardingMessage(async () => {
      const now = Date.now();
      const userId = this.getNumericUserId();
      const sessionId = this.data.sessionId || '';
      const timeText = this.getCurrentTime();
      const pendingVmId = `local_chat_ai_${now.toString(36)}_${Math.random().toString(36).slice(2, 7)}`;
      this.pushTimelineItem({
        id: pendingVmId,
        source: 'chat',
        role: 'assistant',
        systemMessageType: SystemMessageType.CHAT,
        text: '',
        ts: now,
        sessionId,
        userId,
        pending: true,
        pendingKind: 'chat',
        timeText,
        avatar: AI_CHAT_AVATAR_URL
      });
      await this.waitMs(d);
      this.updateTimelineItem(pendingVmId, { text: t, pending: false });
    });
  },

  sendHiddenToAI(userMessage) {
    const raw = String(userMessage || '').trim();
    if (!raw) return;
    const userId = this.getNumericUserId();
    const sessionId = this.data.sessionId || '';
    const now = Date.now();
    const timeText = this.getCurrentTime();
    const aiVM = {
      id: `local_chat_ai_${now.toString(36)}_${Math.random().toString(36).slice(2, 7)}`,
      source: 'chat',
      role: 'assistant',
      systemMessageType: SystemMessageType.CHAT,
      text: '',
      ts: now,
      sessionId,
      userId,
      pending: true,
      pendingKind: 'chat',
      timeText,
      avatar: AI_CHAT_AVATAR_URL
    };
    this.pushTimelineItem(aiVM);
    this.processAIResponse(raw, aiVM.id, 'chat');
  },

  matchOptionIndex(rawText, options) {
    const raw = String(rawText || '').trim();
    const list = Array.isArray(options) ? options : [];
    if (!raw || !list.length) return -1;
    const m = raw.match(/^\s*(\d{1,2})\s*$/);
    if (m) {
      const n = Number(m[1]);
      if (Number.isFinite(n) && n >= 1 && n <= list.length) return n - 1;
    }
    const idx = list.findIndex((it) => String(it) === raw || raw.indexOf(String(it)) >= 0);
    return idx;
  },

  parseFirstNumber(text) {
    const s = String(text || '');
    const m = s.match(/(\d{1,3})/);
    if (!m) return null;
    const n = Number(m[1]);
    return Number.isFinite(n) ? n : null;
  },

  normalizeSkipText(text) {
    const s = String(text || '').trim();
    if (!s) return '';
    return s;
  },

  isSkipAnswer(text) {
    const s = this.normalizeSkipText(text);
    if (!s) return true;
    return /(跳过|不想|不方便|略过|不知道|保密|算了|随便|不告诉你)/.test(s);
  },

  computeOnboardingInitialState({ force } = {}) {
    const forceStart = !!force;
    const draft = this.getProfileSetupDraft();
    const base = {
      active: true,
      step: 'ASK_GENDER',
      collected: {
        genderIndex: draft.genderIndex != null ? Number(draft.genderIndex) : 0,
        age: draft.age != null ? String(draft.age) : '',
        height: draft.height != null ? String(draft.height) : '',
        weight: draft.weight != null ? String(draft.weight) : '',
        goalIndex: draft.goalIndex != null ? Number(draft.goalIndex) : -1,
        activityIndex: draft.activityIndex != null ? Number(draft.activityIndex) : -1,
        dietIndex: draft.dietIndex != null ? Number(draft.dietIndex) : -1,
        allergies: draft.allergies != null ? String(draft.allergies) : ''
      },
      startedAt: Date.now(),
      updatedAt: Date.now()
    };

    if (forceStart) return base;

    const c = base.collected;
    if (!(c.genderIndex > 0)) { base.step = 'ASK_GENDER'; return base; }
    if (!String(c.age || '').trim()) { base.step = 'ASK_AGE'; return base; }
    if (!String(c.height || '').trim()) { base.step = 'ASK_HEIGHT'; return base; }
    if (!String(c.weight || '').trim()) { base.step = 'ASK_WEIGHT'; return base; }
    if (!(c.goalIndex >= 0)) { base.step = 'ASK_GOAL'; return base; }
    if (!(c.activityIndex >= 0)) { base.step = 'ASK_ACTIVITY'; return base; }
    if (!(c.dietIndex >= 0)) { base.step = 'ASK_DIET'; return base; }
    if (!String(c.allergies || '').trim()) { base.step = 'ASK_ALLERGIES'; return base; }
    base.step = 'CONFIRM';
    return base;
  },

  startOnboarding({ force } = {}) {
    this._sessionMode = 'onboarding';
    this.ensureSessionId();
    const state = {
      active: true,
      step: 'AI',
      startedAt: Date.now(),
      updatedAt: Date.now(),
      kickoffSent: false
    };
    this._onboardingState = state;
    this.writeOnboardingState(state);
    if (this.data.quickReplies && this.data.quickReplies.length) {
      this.setData({ quickReplies: [], quickReplyAnchorId: '' });
    }

    this.kickoffOnboardingAI();
  },

  kickoffOnboardingAI() {
    const s = this.getOnboardingState();
    if (!s || s.active !== true) return;
    if (s.kickoffSent === true) return;
    const next = { ...s, kickoffSent: true, updatedAt: Date.now() };
    this.setOnboardingState(next);
    this.sendHiddenToAI(ONBOARDING_KICKOFF_TOKEN);
  },

  startVoiceOnboarding() {
    if (this.data.isCalling) return;
    if (this.data.callStatus === 'connected') return;
    this._sessionMode = 'voice-onboarding';
    this.ensureSessionId();
    this._voiceOnboardingStepIndex = 0;
    this._voiceOnboardingAnswers = {};
    this._voiceOnboardingGreeted = false;
    this.setData({
      isVoiceOnboarding: true,
      voiceOnboardingStep: 'INTRO',
      voiceOnboardingProgress: 0,
      voiceOnboardingStepLabel: '准备开始'
    });
    this.onStartAIPhoneCall();
  },

  getVoiceOnboardingStepByIndex(idx) {
    return (VOICE_ONBOARDING_STEPS && VOICE_ONBOARDING_STEPS[idx]) || null;
  },

  getCurrentVoiceOnboardingStep() {
    const idx = typeof this._voiceOnboardingStepIndex === 'number' ? this._voiceOnboardingStepIndex : 0;
    return this.getVoiceOnboardingStepByIndex(idx) || VOICE_ONBOARDING_STEPS[0];
  },

  voiceOnboardingSay(questionText) {
    if (!questionText) {
      const stepDef = this.getCurrentVoiceOnboardingStep();
      questionText = stepDef ? stepDef.question : '';
    }
    if (!questionText) return;
    try { this.cancelTTS(); } catch (e) {}
    this.playTTS(questionText);
  },

  voiceOnboardingAdvanceStep() {
    const idx = typeof this._voiceOnboardingStepIndex === 'number' ? this._voiceOnboardingStepIndex : 0;
    const total = (VOICE_ONBOARDING_STEPS || []).length || 9;
    const nextIdx = Math.min(idx + 1, total - 1);
    this._voiceOnboardingStepIndex = nextIdx;
    const stepDef = this.getVoiceOnboardingStepByIndex(nextIdx) || {};
    this.setData({
      voiceOnboardingStep: stepDef.step || '',
      voiceOnboardingProgress: nextIdx,
      voiceOnboardingTotal: total,
      voiceOnboardingStepLabel: stepDef.label || ''
    });
    return nextIdx;
  },

  voiceOnboardingHandleAnswer(text) {
    const raw = String(text || '').trim();
    if (!raw) return false;

    const stepDef = this.getCurrentVoiceOnboardingStep();
    if (!stepDef) return false;
    const step = stepDef.step;
    if (step === 'INTRO') {
      if (raw.includes('好') || raw.includes('可以') || raw.includes('行') || raw.includes('开始') || raw.includes('是') || raw.includes('OK') || raw.includes('ok')) {
        return true;
      }
      return true;
    }

    const lower = raw.toLowerCase();

    if (step === 'ASK_GENDER') {
      if (lower.includes('男') || lower === 'boy' || lower === 'male' || lower === 'm' || lower.includes('man')) {
        this._voiceOnboardingAnswers.genderIndex = 1;
      } else {
        this._voiceOnboardingAnswers.genderIndex = 2;
      }
      this._voiceOnboardingAnswers.genderRaw = raw;
    } else if (step === 'ASK_AGE') {
      const nums = raw.match(/\d+/);
      if (nums) this._voiceOnboardingAnswers.age = nums[0];
      this._voiceOnboardingAnswers.ageRaw = raw;
    } else if (step === 'ASK_HEIGHT') {
      const nums = raw.match(/\d+/);
      if (nums) this._voiceOnboardingAnswers.height = nums[0];
      this._voiceOnboardingAnswers.heightRaw = raw;
    } else if (step === 'ASK_WEIGHT') {
      const nums = raw.match(/\d+/);
      if (nums) this._voiceOnboardingAnswers.weight = nums[0];
      this._voiceOnboardingAnswers.weightRaw = raw;
    } else if (step === 'ASK_GOAL') {
      let matched = -1;
      ONBOARDING_GOAL_OPTIONS.forEach((o, i) => {
        if (matched >= 0) return;
        if (lower.includes(o.toLowerCase()) || lower.includes(o.replace(/\/.*/, '').toLowerCase())) matched = i;
      });
      if (matched < 0 && lower.includes('减')) matched = 0;
      if (matched < 0 && lower.includes('增')) matched = 1;
      if (matched < 0 && lower.includes('保持')) matched = 2;
      if (matched < 0 && lower.includes('糖')) matched = 3;
      this._voiceOnboardingAnswers.goalIndex = matched >= 0 ? matched : 0;
      this._voiceOnboardingAnswers.goalRaw = raw;
    } else if (step === 'ASK_ACTIVITY') {
      let matched = -1;
      ONBOARDING_ACTIVITY_OPTIONS.forEach((o, i) => {
        if (matched >= 0) return;
        if (lower.includes(o.toLowerCase()) || (i === 0 && lower.includes('久坐'))) matched = i;
      });
      if (matched < 0 && lower.includes('不运动') || lower.includes('不动')) matched = 0;
      if (matched < 0 && lower.includes('一点点') || lower.includes('偶尔')) matched = 1;
      if (matched < 0 && lower.includes('一般') || lower.includes('中等') || lower.includes('适中')) matched = 2;
      if (matched < 0 && lower.includes('经常') || lower.includes('高强度') || lower.includes('很多')) matched = 3;
      this._voiceOnboardingAnswers.activityIndex = matched >= 0 ? matched : 1;
      this._voiceOnboardingAnswers.activityRaw = raw;
    } else if (step === 'ASK_DIET') {
      let matched = -1;
      ONBOARDING_DIET_OPTIONS.forEach((o, i) => {
        if (matched >= 0) return;
        if (lower.includes(o.toLowerCase())) matched = i;
      });
      if (matched < 0 && (lower.includes('没有') || lower.includes('无') || lower.includes('没什么') || lower.includes('都可以'))) matched = 0;
      this._voiceOnboardingAnswers.dietIndex = matched >= 0 ? matched : 0;
      this._voiceOnboardingAnswers.dietRaw = raw;
    } else if (step === 'ASK_ALLERGIES') {
      if (lower.includes('没有') || lower.includes('无') || lower.includes('没什么') || lower.includes('不过敏')) {
        this._voiceOnboardingAnswers.allergies = '';
      } else {
        this._voiceOnboardingAnswers.allergies = raw;
      }
      this._voiceOnboardingAnswers.allergiesRaw = raw;
    }

    return true;
  },

  voiceOnboardingStoreAnswersToProfile() {
    const a = this._voiceOnboardingAnswers || {};
    const state = {
      active: true,
      step: 'CONFIRM',
      startedAt: Date.now(),
      updatedAt: Date.now(),
      kickoffSent: true,
      collected: {
        genderIndex: a.genderIndex != null ? Number(a.genderIndex) : 0,
        age: a.age != null ? String(a.age) : '',
        height: a.height != null ? String(a.height) : '',
        weight: a.weight != null ? String(a.weight) : '',
        goalIndex: a.goalIndex != null ? Number(a.goalIndex) : -1,
        activityIndex: a.activityIndex != null ? Number(a.activityIndex) : -1,
        dietIndex: a.dietIndex != null ? Number(a.dietIndex) : -1,
        allergies: a.allergies != null ? String(a.allergies) : ''
      }
    };
    this._onboardingState = state;
    this.writeOnboardingState(state);
  },

  voiceOnboardingBuildSummaryText() {
    const a = this._voiceOnboardingAnswers || {};
    const gender = a.genderIndex === 1 ? '男' : '女';
    const age = a.age || '未知';
    const height = a.height || '未知';
    const weight = a.weight || '未知';
    const goal = (a.goalIndex >= 0 && ONBOARDING_GOAL_OPTIONS[a.goalIndex]) ? ONBOARDING_GOAL_OPTIONS[a.goalIndex] : '保持健康';
    const activity = (a.activityIndex >= 0 && ONBOARDING_ACTIVITY_OPTIONS[a.activityIndex]) ? ONBOARDING_ACTIVITY_OPTIONS[a.activityIndex] : '轻度活动';
    const diet = (a.dietIndex >= 0 && ONBOARDING_DIET_OPTIONS[a.dietIndex]) ? ONBOARDING_DIET_OPTIONS[a.dietIndex] : '无';
    const allergies = a.allergies || '无';

    return '好的，我帮你整理一下：性别' + gender + '，年龄' + age + '岁，身高' + height + '厘米，体重' + weight + '公斤，目标是' + goal + '，活动量' + activity + '，饮食偏好' + diet + '，忌口' + allergies + '。这些信息准确吗？如果准确，我就帮你生成专属的营养方案啦。';
  },

  _handleVoiceOnboardingInput(text) {
    const stepDef = this.getCurrentVoiceOnboardingStep();
    if (!stepDef) return;
    const step = stepDef.step;

    this.voiceOnboardingHandleAnswer(text);

    if (step === 'ASK_ALLERGIES') {
      this.voiceOnboardingStoreAnswersToProfile();
      const summary = this.voiceOnboardingBuildSummaryText();
      this.setData({
        voiceOnboardingStep: 'CONFIRM',
        voiceOnboardingProgress: this.data.voiceOnboardingTotal,
        voiceOnboardingStepLabel: '确认'
      });
      this.playTTS(summary);
      return;
    }

    if (step === 'CONFIRM') {
      const lower = String(text || '').toLowerCase();
      if (lower.includes('对') || lower.includes('是') || lower.includes('可以') || lower.includes('好') || lower.includes('准确') || lower.includes('没错') || lower.includes('ok')) {
        this._completeVoiceOnboarding();
      } else {
        this._voiceOnboardingStepIndex = 1;
        const firstQ = VOICE_ONBOARDING_STEPS[1];
        this.setData({
          voiceOnboardingStep: firstQ.step,
          voiceOnboardingProgress: 1,
          voiceOnboardingStepLabel: firstQ.label
        });
        this.voiceOnboardingSay(firstQ.question);
      }
      return;
    }

    this.voiceOnboardingAdvanceStep();
    const nextStepDef = this.getCurrentVoiceOnboardingStep();
    if (nextStepDef) {
      this.voiceOnboardingSay(nextStepDef.question);
    }
  },

  _completeVoiceOnboarding() {
    try { wx.vibrateShort({ type: 'heavy' }); } catch (e) {}
    const a = this._voiceOnboardingAnswers || {};
    const goal = (a.goalIndex >= 0 && ONBOARDING_GOAL_OPTIONS[a.goalIndex]) ? ONBOARDING_GOAL_OPTIONS[a.goalIndex] : '保持健康';
    const completeText = '太好了！你的健康档案已经建立好了。根据你的情况，我为你制定了' + goal + '的营养方案。从今天开始，你可以随时找我聊饮食、运动和健康，我会一直陪伴你。现在，我们挂掉电话，开始你的健康之旅吧！';

    this.setData({
      voiceOnboardingStep: 'DONE',
      voiceOnboardingProgress: this.data.voiceOnboardingTotal,
      voiceOnboardingStepLabel: '完成'
    });

    this.playTTS(completeText);

    setTimeout(() => {
      this.endAIPhoneCall();
      this.setData({
        isVoiceOnboarding: false,
        isOnboardingComplete: true
      });
      try {
        const app = getApp();
        if (app && typeof app.setUserProfile === 'function') {
          app.setUserProfile({
            genderIndex: a.genderIndex || 0,
            age: a.age || '',
            height: a.height || '',
            weight: a.weight || '',
            goalIndex: a.goalIndex != null ? a.goalIndex : -1,
            activityIndex: a.activityIndex != null ? a.activityIndex : -1,
            dietIndex: a.dietIndex != null ? a.dietIndex : -1,
            allergies: a.allergies || ''
          });
        }
      } catch (e) {}
      try { this.saveChatCache(); } catch (e) {}
    }, 5000);
  },

  getOnboardingState() {
    if (this._onboardingState && typeof this._onboardingState === 'object') return this._onboardingState;
    const cached = this.readOnboardingState();
    if (cached && typeof cached === 'object') {
      this._onboardingState = cached;
      return cached;
    }
    return null;
  },

  setOnboardingState(next) {
    this._onboardingState = next;
    this.writeOnboardingState(next);
  },

  endOnboarding({ markDone } = {}) {
    const done = !!markDone;
    const s = this.getOnboardingState() || {};
    const next = { ...(s || {}), active: false, updatedAt: Date.now() };
    this.setOnboardingState(next);
    if (done) {
      try { wx.setStorageSync(this.getOnboardingDoneKey(), true); } catch (e) {}
    }
  },

  summarizeOnboardingCollected(collected) {
    const c = collected && typeof collected === 'object' ? collected : {};
    const genderText = c.genderIndex === 1 ? '男' : c.genderIndex === 2 ? '女' : '';
    const goalText = (typeof c.goalIndex === 'number' && c.goalIndex >= 0 && c.goalIndex < ONBOARDING_GOAL_OPTIONS.length) ? ONBOARDING_GOAL_OPTIONS[c.goalIndex] : '';
    const activityText = (typeof c.activityIndex === 'number' && c.activityIndex >= 0 && c.activityIndex < ONBOARDING_ACTIVITY_OPTIONS.length) ? ONBOARDING_ACTIVITY_OPTIONS[c.activityIndex] : '';
    const dietText = (typeof c.dietIndex === 'number' && c.dietIndex >= 0 && c.dietIndex < ONBOARDING_DIET_OPTIONS.length) ? ONBOARDING_DIET_OPTIONS[c.dietIndex] : '';
    const lines = [];
    if (genderText) lines.push(`性别：${genderText}`);
    if (String(c.age || '').trim()) lines.push(`年龄：${String(c.age).trim()} 岁`);
    if (String(c.height || '').trim()) lines.push(`身高：${String(c.height).trim()} cm`);
    if (String(c.weight || '').trim()) lines.push(`体重：${String(c.weight).trim()} kg`);
    if (goalText) lines.push(`目标：${goalText}`);
    if (activityText) lines.push(`活动水平：${activityText}`);
    if (dietText) lines.push(`饮食偏好：${dietText}`);
    if (String(c.allergies || '').trim()) lines.push(`忌口/过敏：${String(c.allergies).trim()}`);
    return lines.length ? lines.join('\n') : '（尚未收集到信息）';
  },

  advanceOnboarding() {
    const s = this.getOnboardingState();
    if (!s || s.active !== true) return;

    const step = String(s.step || '').trim();
    const c = s.collected || {};

    if (step === 'ASK_GENDER') {
      this.onboardingSay('先从一个简单的开始：你的性别是？（男/女；不方便就回“跳过”）');
      return;
    }

    if (step === 'ASK_AGE') {
      this.onboardingSay('你的年龄是？（例如 28；不方便就回“跳过”）');
      return;
    }

    if (step === 'ASK_HEIGHT') {
      this.onboardingSay('身高多少 cm？（例如 172；不方便就回“跳过”）');
      return;
    }

    if (step === 'ASK_WEIGHT') {
      this.onboardingSay('体重多少 kg？（例如 65；不方便就回“跳过”）');
      return;
    }

    if (step === 'ASK_GOAL') {
      const opt = ONBOARDING_GOAL_OPTIONS.map((it, i) => `${i + 1}. ${it}`).join('\n');
      this.onboardingSay(`你的主要目标是？\n${opt}\n你可以回数字（如 1）或直接回文字；不方便就回“跳过”。`);
      return;
    }

    if (step === 'ASK_ACTIVITY') {
      const opt = ONBOARDING_ACTIVITY_OPTIONS.map((it, i) => `${i + 1}. ${it}`).join('\n');
      this.onboardingSay(`你的日常活动水平更接近哪种？\n${opt}\n回数字或文字即可；不方便就回“跳过”。`);
      return;
    }

    if (step === 'ASK_DIET') {
      const opt = ONBOARDING_DIET_OPTIONS.map((it, i) => `${i + 1}. ${it}`).join('\n');
      this.onboardingSay(`你的饮食偏好是？\n${opt}\n回数字或文字即可；不方便就回“跳过”。`);
      return;
    }

    if (step === 'ASK_ALLERGIES') {
      this.onboardingSay('有忌口或过敏吗？（例如 牛奶、花生；没有就回“无”；不方便就回“跳过”）');
      return;
    }

    if (step === 'CONFIRM') {
      this.onboardingSay(`我把信息整理一下：\n${this.summarizeOnboardingCollected(c)}\n\n要把这些写入“完善资料”的草稿吗？\n你可以回：\n- 保存\n- 保存并去完善资料\n- 先不保存`);
    }
  },

  upsertOnboardingGateCard({ enabled } = {}) {
    const now = Date.now();
    const timeText = this.getCurrentTime();
    const userId = this.getNumericUserId();
    if (!userId) return;
    const decisionKey = `onboarding_gate:${userId}`;
    const id = `sys_${decisionKey}`;
    const list = Array.isArray(this.data.timeline) ? this.data.timeline : [];
    const exists = list.some((m) => m && String(m.id || '') === id);
    if (!exists) return;
    const text = '为了让「提醒 / 日终总结」能自动履约，需要先开启服务器侧能力开关。\n这不是消息通知开关；你随时也可以去【我的-设置】修改。';
    this.upsertSystemTimelineItem({
      id,
      kind: 'system',
      source: 'chat',
      role: 'system',
      systemMessageType: SystemMessageType.ONBOARDING_GATE,
      decisionKey,
      statusPhase: enabled ? 'ENABLED' : 'PENDING',
      statusText: enabled ? '已开启' : '待开启',
      text,
      ts: now,
      timeText
    });
  },

  upsertFeatureGuideCard() {
    const now = Date.now();
    const timeText = this.getCurrentTime();
    const userId = this.getNumericUserId();
    if (!userId) return;
    const decisionKey = `feature_guide:${userId}`;
    const id = `sys_${decisionKey}`;
    const list = Array.isArray(this.data.timeline) ? this.data.timeline : [];
    const exists = list.some((m) => m && String(m.id || '') === id);
    if (!exists) return;
    const features = [
      { title: '拍照记录饮食', desc: '拍一张餐食照片，我来识别并帮你记录', example: '点击「去拍照」', action: 'photo' },
      { title: '系统提醒', desc: '到点自动提醒，不用你记', example: '每天 8 点提醒我喝水', action: 'fill' },
      { title: '日终总结', desc: '复盘今天吃得怎么样、给明天建议', example: '给我做个今天的总结', action: 'fill' }
    ];
    this.upsertSystemTimelineItem({
      id,
      kind: 'system',
      source: 'chat',
      role: 'system',
      systemMessageType: SystemMessageType.FEATURE_GUIDE,
      decisionKey,
      features,
      ts: now,
      timeText
    });
  },

  async enableOnboardingGate() {
    const caps = ['PROACTIVE_REMINDER', 'PROACTIVE_SUMMARY'];
    const now = Date.now();
    const userId = this.getNumericUserId();
    const sessionId = this.data.sessionId || '';
    const timeText = this.getCurrentTime();
    const pendingVmId = `local_chat_ai_${now.toString(36)}_${Math.random().toString(36).slice(2, 7)}`;
    this.pushTimelineItem({
      id: pendingVmId,
      source: 'chat',
      role: 'assistant',
      systemMessageType: SystemMessageType.CHAT,
      text: '',
      ts: now,
      sessionId,
      userId,
      pending: true,
      pendingKind: 'chat',
      timeText,
      avatar: AI_CHAT_AVATAR_URL
    });
    try {
      for (let i = 0; i < caps.length; i += 1) {
        const key = caps[i];
        await request.post('/user/capability/set', { capabilityKey: key, state: 'ENABLED', reason: 'onboarding' }, { loading: false, showError: false });
      }
      await this.refreshRuntimeState({ silent: true });
      this.upsertOnboardingGateCard({ enabled: true });
      this.updateTimelineItem(pendingVmId, { text: '已开启。之后你说“每天 8 点提醒我喝水”这类话，我就能按时帮你提醒。', pending: false });
      return true;
    } catch (e) {
      try { wx.showToast({ title: '开启失败，可稍后在设置里开启', icon: 'none' }); } catch (err) {}
      this.upsertOnboardingGateCard({ enabled: false });
      this.updateTimelineItem(pendingVmId, { text: '我这边没成功开起来，你可以稍后去【我的-设置】里开启。先继续也没问题。', pending: false, error: true });
      return false;
    } finally {
      try { this.scheduleSaveChatCache(); } catch (e) {}
    }
  },

  handleOnboardingInput(text) {
    const s = this.getOnboardingState();
    if (!s || s.active !== true) return false;

    const raw = String(text || '').trim();
    if (this.data.quickReplies && this.data.quickReplies.length) {
      this.setData({ quickReplies: [], quickReplyAnchorId: '' });
    }
    this.pushUserText(raw);

    const step = String(s.step || '').trim();
    const c = { ...(s.collected || {}) };

    const reject = (msg) => {
      this.onboardingSay(String(msg || '').trim() || '我没太听懂，能换个说法吗？', { thinkingText: '想一下...' });
    };

    if (step === 'ASK_GENDER') {
      if (this.isSkipAnswer(raw)) {
        c.genderIndex = 0;
      } else if (/^男$/.test(raw) || (raw.indexOf('男') >= 0 && raw.indexOf('女') < 0)) {
        c.genderIndex = 1;
      } else if (/^女$/.test(raw) || raw.indexOf('女') >= 0) {
        c.genderIndex = 2;
      } else {
        reject('我没看懂你的性别。回“男/女”，或者回“跳过”。');
        return true;
      }
      const next = { ...s, collected: c, step: 'ASK_AGE', updatedAt: Date.now() };
      this.setOnboardingState(next);
      this.advanceOnboarding();
      return true;
    }

    if (step === 'ASK_AGE') {
      if (this.isSkipAnswer(raw)) {
        c.age = '';
      } else {
        const n = this.parseFirstNumber(raw);
        if (!n || n < 1 || n > 120) {
          reject('年龄我需要一个 1~120 的数字（例如 28），或者回“跳过”。');
          return true;
        }
        c.age = String(n);
      }
      const next = { ...s, collected: c, step: 'ASK_HEIGHT', updatedAt: Date.now() };
      this.setOnboardingState(next);
      this.advanceOnboarding();
      return true;
    }

    if (step === 'ASK_HEIGHT') {
      if (this.isSkipAnswer(raw)) {
        c.height = '';
      } else {
        const n = this.parseFirstNumber(raw);
        if (!n || n < 80 || n > 250) {
          reject('身高我需要一个 80~250 的数字（单位 cm，例如 172），或者回“跳过”。');
          return true;
        }
        c.height = String(n);
      }
      const next = { ...s, collected: c, step: 'ASK_WEIGHT', updatedAt: Date.now() };
      this.setOnboardingState(next);
      this.advanceOnboarding();
      return true;
    }

    if (step === 'ASK_WEIGHT') {
      if (this.isSkipAnswer(raw)) {
        c.weight = '';
      } else {
        const n = this.parseFirstNumber(raw);
        if (!n || n < 20 || n > 300) {
          reject('体重我需要一个 20~300 的数字（单位 kg，例如 65），或者回“跳过”。');
          return true;
        }
        c.weight = String(n);
      }
      const next = { ...s, collected: c, step: 'ASK_GOAL', updatedAt: Date.now() };
      this.setOnboardingState(next);
      this.advanceOnboarding();
      return true;
    }

    if (step === 'ASK_GOAL') {
      if (this.isSkipAnswer(raw)) {
        c.goalIndex = -1;
        const next = { ...s, collected: c, step: 'ASK_ACTIVITY', updatedAt: Date.now() };
        this.setOnboardingState(next);
        this.advanceOnboarding();
        return true;
      }
      const idx = this.matchOptionIndex(raw, ONBOARDING_GOAL_OPTIONS);
      if (idx < 0) {
        reject('我没读懂你的目标。你可以回数字（如 1）或直接回“减脂/增肌/控糖…”，不方便就回“跳过”。');
        return true;
      }
      c.goalIndex = idx;
      const next = { ...s, collected: c, step: 'ASK_ACTIVITY', updatedAt: Date.now() };
      this.setOnboardingState(next);
      this.advanceOnboarding();
      return true;
    }

    if (step === 'ASK_ACTIVITY') {
      if (this.isSkipAnswer(raw)) {
        c.activityIndex = -1;
        const next = { ...s, collected: c, step: 'ASK_DIET', updatedAt: Date.now() };
        this.setOnboardingState(next);
        this.advanceOnboarding();
        return true;
      }
      const idx = this.matchOptionIndex(raw, ONBOARDING_ACTIVITY_OPTIONS);
      if (idx < 0) {
        reject('我没读懂你的活动水平。你可以回数字（如 2）或直接回“久坐/轻度活动…”，不方便就回“跳过”。');
        return true;
      }
      c.activityIndex = idx;
      const next = { ...s, collected: c, step: 'ASK_DIET', updatedAt: Date.now() };
      this.setOnboardingState(next);
      this.advanceOnboarding();
      return true;
    }

    if (step === 'ASK_DIET') {
      if (this.isSkipAnswer(raw)) {
        c.dietIndex = -1;
        const next = { ...s, collected: c, step: 'ASK_ALLERGIES', updatedAt: Date.now() };
        this.setOnboardingState(next);
        this.advanceOnboarding();
        return true;
      }
      const idx = this.matchOptionIndex(raw, ONBOARDING_DIET_OPTIONS);
      if (idx < 0) {
        reject('我没读懂你的饮食偏好。你可以回数字或直接回“低碳/素食/无…”，不方便就回“跳过”。');
        return true;
      }
      c.dietIndex = idx;
      const next = { ...s, collected: c, step: 'ASK_ALLERGIES', updatedAt: Date.now() };
      this.setOnboardingState(next);
      this.advanceOnboarding();
      return true;
    }

    if (step === 'ASK_ALLERGIES') {
      if (this.isSkipAnswer(raw) || raw === '无') {
        c.allergies = raw === '无' ? '无' : '';
      } else {
        c.allergies = String(raw).slice(0, 80);
      }
      const next = { ...s, collected: c, step: 'CONFIRM', updatedAt: Date.now() };
      this.setOnboardingState(next);
      this.advanceOnboarding();
      return true;
    }

    if (step === 'CONFIRM') {
      if (raw.indexOf('保存') >= 0) {
        const patch = {
          genderIndex: c.genderIndex != null ? Number(c.genderIndex) : 0,
          age: c.age != null ? String(c.age) : '',
          height: c.height != null ? String(c.height) : '',
          weight: c.weight != null ? String(c.weight) : '',
          goalIndex: c.goalIndex != null ? Number(c.goalIndex) : -1,
          activityIndex: c.activityIndex != null ? Number(c.activityIndex) : -1,
          dietIndex: c.dietIndex != null ? Number(c.dietIndex) : -1,
          allergies: c.allergies != null ? String(c.allergies) : ''
        };
        this.persistProfileSetupDraft(patch);
        this.onboardingSay('已写入“完善资料”草稿。你随时可以在【我的】-【完善资料】里继续。', { thinkingText: '整理中...' });
        if (raw.indexOf('去完善资料') >= 0) {
          try { wx.navigateTo({ url: '/pages/profile/profile-setup/profile-setup?from=onboarding' }); } catch (e) {}
        }
        const next = { ...s, collected: c, step: 'GATE', updatedAt: Date.now() };
        this.setOnboardingState(next);
        return true;
      }
      this.onboardingSay('好的，先不保存。你之后也可以随时在【我的】-【完善资料】里填写。', { thinkingText: '收到...' });
      const next = { ...s, collected: c, step: 'GATE', updatedAt: Date.now() };
      this.setOnboardingState(next);
      return true;
    }

    if (step === 'GATE') {
      if (/(开|开启|好|可以|同意|授权)/.test(raw)) {
        this.enableOnboardingGate().then(() => {
          const next = { ...(this.getOnboardingState() || s), step: 'TEACH', updatedAt: Date.now() };
          this.setOnboardingState(next);
          this.endOnboarding({ markDone: true });
        });
        return true;
      }
      const next = { ...s, step: 'TEACH', updatedAt: Date.now() };
      this.setOnboardingState(next);
      this.endOnboarding({ markDone: true });
      return true;
    }

    return true;
  },

  onEnableOnboardingGateTap() {
    const s = this.getOnboardingState();
    if (!s || s.active !== true) return;
    this.enableOnboardingGate().then(() => {
      const next = { ...(this.getOnboardingState() || s), step: 'TEACH', updatedAt: Date.now() };
      this.setOnboardingState(next);
      this.endOnboarding({ markDone: true });
    });
  },

  onSkipOnboardingGateTap() {
    const s = this.getOnboardingState();
    if (!s || s.active !== true) return;
    const next = { ...s, step: 'TEACH', updatedAt: Date.now() };
    this.setOnboardingState(next);
    this.endOnboarding({ markDone: true });
  },

  onFillFeatureExample(e) {
    const text = e && e.currentTarget && e.currentTarget.dataset ? String(e.currentTarget.dataset.text || '').trim() : '';
    if (!text) return;
    this.setData({ inputText: text });
  },

  dispatchUserText(text) {
    const raw = String(text || '').trim();
    if (!raw) return;
    const s = this.getOnboardingState();
    if (s && s.active === true) {
      if (/^我今天吃了[。\.…]*$/.test(raw)) {
        this.pushUserText(raw);
        this.onboardingSay('好，直接拍一张照片（或从相册选一张），我来识别并记录。');
        try { this.onImageUpload(); } catch (e) {}
        return;
      }
      this.sendTextMessage(raw);
      return;
    }
    this.sendTextMessage(raw);
  },

  onHeaderLongPress() {
    const inOnboarding = this.isOnboardingMode();
    let forcedAlways = false;
    try { forcedAlways = !!wx.getStorageSync(this.getOnboardingForceKey()); } catch (e) { forcedAlways = false; }

    const items = [];
    const actions = [];
    if (inOnboarding) {
      items.push('退出引导测试（恢复正常会话）');
      actions.push('exit');
    } else {
      items.push('进入引导测试（隔离会话）');
      actions.push('enter');
    }
    items.push(forcedAlways ? '关闭强制引导' : '开启强制引导');
    actions.push('toggle_force');
    items.push('重置引导完成标记');
    actions.push('reset_done');
    items.push('清空引导会话缓存');
    actions.push('clear_onboarding_cache');
    items.push('清空资料草稿（完善资料）');
    actions.push('clear_profile_draft');

    wx.showActionSheet({
      itemList: items,
      success: (res) => {
        const act = actions[res.tapIndex];
        if (!act) return;

        if (act === 'enter') {
          this.clearSessionCacheByMode('onboarding', { clearSessionId: true });
          this._sessionMode = 'onboarding';
          this.reloadSessionView({ silent: true });
          try { wx.showToast({ title: '已进入引导测试', icon: 'none' }); } catch (e) {}
          return;
        }

        if (act === 'exit') {
          this._sessionMode = 'default';
          this.reloadSessionView({ silent: true });
          try { wx.showToast({ title: '已恢复正常会话', icon: 'none' }); } catch (e) {}
          return;
        }

        if (act === 'toggle_force') {
          const k = this.getOnboardingForceKey();
          if (forcedAlways) {
            try { wx.removeStorageSync(k); } catch (e) {}
            try { wx.showToast({ title: '已关闭强制引导', icon: 'none' }); } catch (e) {}
          } else {
            try { wx.setStorageSync(k, true); } catch (e) {}
            try { wx.showToast({ title: '已开启强制引导', icon: 'none' }); } catch (e) {}
          }
          return;
        }

        if (act === 'reset_done') {
          try { wx.removeStorageSync(this.getOnboardingDoneKey()); } catch (e) {}
          try { wx.showToast({ title: '已重置引导标记', icon: 'none' }); } catch (e) {}
          return;
        }

        if (act === 'clear_onboarding_cache') {
          this.clearSessionCacheByMode('onboarding', { clearSessionId: true });
          if (this.isOnboardingMode()) this.reloadSessionView({ silent: true });
          try { wx.showToast({ title: '已清空引导会话缓存', icon: 'none' }); } catch (e) {}
          return;
        }

        if (act === 'clear_profile_draft') {
          try { wx.removeStorageSync(PROFILE_SETUP_DRAFT_KEY); } catch (e) {}
          try { wx.showToast({ title: '已清空资料草稿', icon: 'none' }); } catch (e) {}
        }
      }
    });
  },

  onLoad(options) {
    try {
      const systemInfo = wx.getSystemInfoSync();
      const safeAreaBottomPx = systemInfo.safeArea ? systemInfo.windowHeight - systemInfo.safeArea.bottom : 0;
      const rpx2px = (Number(systemInfo.windowWidth || 375) / 750);
      const tabbarBaseHeightPx = Math.round(180 * rpx2px);
      const inputBarGapPx = 0;
      this._tabbarBaseHeightPx = tabbarBaseHeightPx;
      const inputBarBottomPx = safeAreaBottomPx + tabbarBaseHeightPx + inputBarGapPx - 30;
      const contentBottomPaddingPx = inputBarBottomPx + 0;
      this.setData({
        statusBarHeight: systemInfo.statusBarHeight,
        safeAreaBottom: safeAreaBottomPx,
        inputBarBottomPx,
        contentBottomPaddingPx
      });
      setTimeout(() => this.measureBottomOverlays(), 80);

      this.initPage();
      this.updateDhGreeting();
      this.updateSuggestionState();
      this.initCallAudio();
      this.initTTSAudio();
      this.initSpectrumColors(24);
      // 初始化一个默认工作流示例，便于演示
      this.setData({ workflowItems: this.getDefaultWorkflow() });
      this.setData({
        managerActions: [
          {
            id: 'daily_summary_archive',
            title: '每日总结',
            subtitle: '查看档案',
            icon: '/static/aiAgentPage/clock.svg',
            action: 'navigate',
            url: '/pages/dailySummary/dailySummary'
          }
        ]
      });
    } catch (error) {
      console.error('页面加载失败:', error);
    }
    try {
      const app = getApp();
      const apiBase = (app && app.getApiBase) ? app.getApiBase() : (wx.getStorageSync('API_BASE_URL') || '');
      this.setData({ apiBase });
      tts.setBaseUrl(apiBase);
      stt.setBaseUrl(apiBase);
    } catch (e) {}
    try {
      this._ttsReady = Promise.resolve(tts.cleanup && tts.cleanup()).catch(() => {});
    } catch (e) {
      this._ttsReady = Promise.resolve();
    }
    this.initSessionMode(options);
    this.ensureSessionId();
    this.restoreChatFromCache();
    this.restoreFactFromCache();
    this.ensureChatCursorFromCache();
    this.syncChatHistoryFromServer({ silent: true }).finally(() => {
      try {
        const app = getApp();
        app.fetchAiInboxOnce && app.fetchAiInboxOnce({ silent: true });
      } catch (e) {}
      setTimeout(() => {
        this.drainInboxStorageToTimeline();
        this.ensureWelcomeMessage();
        this.scrollToBottomLater();
      }, 180);
    });
    setTimeout(() => {
      this.setData({ scrollWithAnimation: true });
    }, 800);

    try {
      const app = getApp();
      this._onAiInboxUpdate = () => this.drainInboxStorageToTimeline();
      app.onEvent && app.onEvent('aiInboxUpdate', this._onAiInboxUpdate);
      app.hideNotification && app.hideNotification();
    } catch (e) {}

    this.drainInboxStorageToTimeline();
  },

  onShow() {
    this.updateDhGreeting();
    this.updateSuggestionState();
    this.drainInboxStorageToTimeline();
    this.syncChatHistoryFromServer({ silent: true }).finally(() => this.scrollToBottomLater());
    setTimeout(() => this.measureBottomOverlays(), 80);
    try { wx.hideTabBar({ animation: false }); } catch (e) {}
    try {
      const app = getApp();
      app.hideNotification && app.hideNotification();
    } catch (e) {}
  },

  getEnvVersion() {
    try {
      const info = wx.getAccountInfoSync();
      return info && info.miniProgram && info.miniProgram.envVersion ? String(info.miniProgram.envVersion) : '';
    } catch (e) {
      return '';
    }
  },

  showTtsPlayError(stage, err, extra = {}) {
    const env = this.getEnvVersion();
    const msg = err && (err.errMsg || err.message) ? String(err.errMsg || err.message) : '';
    const audioPath = String((extra && extra.audioPath) ? extra.audioPath : (this._ttsCurrentAudioPath || '')).trim();
    const sys = (() => {
      try {
        const s = wx.getSystemInfoSync();
        const model = s && s.model ? String(s.model) : '';
        const system = s && s.system ? String(s.system) : '';
        const platform = s && s.platform ? String(s.platform) : '';
        return [platform, system, model].filter(Boolean).join(' | ');
      } catch (e) {
        return '';
      }
    })();
    if (env === 'release') {
      wx.showToast({ title: '语音播放失败', icon: 'none' });
      return;
    }
    const lines = [];
    lines.push(stage ? `阶段：${stage}` : '阶段：');
    lines.push(env ? `环境：${env}` : '环境：');
    if (sys) lines.push(`设备：${sys}`);
    lines.push(msg ? `原因：${msg}` : '原因：');
    lines.push(audioPath ? `音频：${audioPath}` : '音频：');
    wx.showModal({ title: '语音播放失败', content: lines.join('\n'), showCancel: false, confirmText: '知道了' });
  },

  onHide() {
    try {
      const app = getApp();
      app.startAiInboxPolling && app.startAiInboxPolling();
    } catch (e) {}
    try { this._pauseCallVideo(); } catch (e) {}
    try { this.cancelTTS && this.cancelTTS(); } catch (e) {}
    try { this.saveChatCache(); } catch (e) {}
  },

  onUnload() {
    try {
      const app = getApp();
      app.offEvent && app.offEvent('aiInboxUpdate', this._onAiInboxUpdate);
      app.startAiInboxPolling && app.startAiInboxPolling();
    } catch (e) {}
    try { this.endAIPhoneCall(); } catch (e) {}
    try { this.cancelTTS && this.cancelTTS(); } catch (e) {}
    try { this.saveChatCache(); } catch (e) {}
    try {
      if (this._measureBottomOverlaysTimer) clearTimeout(this._measureBottomOverlaysTimer);
      this._measureBottomOverlaysTimer = null;
    } catch (e) {}
    try { this._clearVoiceRecordTimer(); } catch (e) {}
    try { this.stopSpectrumAnimLoop(); } catch (e) {}
    try { this.stopAISpectrum(); } catch (e) {}
  },

  initPage() {
    try {
      this.refreshUserInfo();
    } catch (error) {
      console.error('初始化页面失败:', error);
    }
  },

  refreshUserInfo() {
    let raw = {};
    try {
      raw = wx.getStorageSync('user_cache') || wx.getStorageSync('userProfile') || wx.getStorageSync('userInfo') || {};
    } catch (e) {
      raw = {};
    }
    const userInfo = this.normalizeUserInfo(raw);
    this.setData({ userInfo });
  },

  normalizeUserInfo(raw = {}) {
    const avatarUrl = raw.avatarUrl || raw.avatar || raw.avatar_url || raw.headImg || raw.headimgurl || '';
    const nickname = raw.nickname || raw.username || raw.name || raw.mobile || '';
    const id = raw.id || raw.userId || raw.uid || '';
    return { ...raw, avatarUrl, nickname, id };
  },

  drainInboxStorageToTimeline() {
    let list = [];
    try {
      const v = wx.getStorageSync(AI_INBOX_KEY);
      list = Array.isArray(v) ? v : [];
    } catch (e) {}
    if (!list.length) return;

    const grouped = new Map();
    list.forEach((it) => {
      const t = this.normalizeSystemMessageType(it && it.systemMessageType);
      const decisionKey = String(it && it.decisionKey ? it.decisionKey : '').trim();
      const serverId = String(it && (it.serverId || it.serverMessageId || it.id) ? (it.serverId || it.serverMessageId || it.id) : '').trim();
      const fallback = serverId || String(it && it.dedupeKey ? it.dedupeKey : '').trim();
      const key = decisionKey || fallback;
      if (!key) return;
      const ts = Number(it && it.ts ? it.ts : Date.now());
      const prev = grouped.get(key);
      if (!prev || ts >= prev.ts) grouped.set(key, { it, ts, t, key, decisionKey });
    });

    const entries = Array.from(grouped.values()).sort((a, b) => Number(a.ts || 0) - Number(b.ts || 0));
    entries.forEach(({ it, ts, t, key, decisionKey }) => {
      const createdAt = it && it.createdAt ? String(it.createdAt) : '';
      const subject = String(it && it.subject ? it.subject : '').trim();
      const timeSlot = String(it && it.timeSlot ? it.timeSlot : '').trim();
      const systemMessageType = t;
      const isChatLike = systemMessageType === SystemMessageType.CHAT || systemMessageType === SystemMessageType.PROACTIVE_INQUIRY;
      const dk = decisionKey || `inbox:${key}`;
      const sysId = `sys_${dk}`;
      const phase = systemMessageType === SystemMessageType.REMINDER ? 'TRIGGERED' : 'DELIVERED';
      const statusText = systemMessageType === SystemMessageType.REMINDER
        ? `到点提醒${subject ? `：${subject}` : ''}`
        : '系统消息已送达';
      const primaryActionLabel = systemMessageType === SystemMessageType.REMINDER ? this.getReminderPrimaryActionLabel(dk, subject) : '';
      this.upsertSystemTimelineItem({
        id: sysId,
        kind: 'system',
        source: 'inbox',
        role: isChatLike ? 'assistant' : 'system',
        systemMessageType,
        decisionKey: dk,
        timeSlot,
        subject,
        statusPhase: phase,
        statusText,
        primaryActionLabel,
        canComplete: systemMessageType === SystemMessageType.REMINDER && String(dk).indexOf('reminder:') === 0,
        text: String(it && (it.text || it.message) ? (it.text || it.message) : ''),
        ts: Number.isFinite(ts) ? ts : Date.now(),
        createdAt,
        timeText: createdAt && createdAt.length >= 16 ? createdAt.slice(11, 16) : this.getCurrentTime(),
        avatar: isChatLike ? AI_CHAT_AVATAR_URL : '',
        explanationCode: String(it && it.explanationCode ? it.explanationCode : '').trim(),
        explanationArgs: it ? it.explanationArgs : null
      });
    });

    try { wx.setStorageSync(AI_INBOX_KEY, []); } catch (e) {}
  },

  getChatCacheKey() {
    const sid = this.getSessionId();
    return `${CHAT_CACHE_PREFIX}${sid}`;
  },

  getChatCursorKey() {
    const sid = this.getSessionId();
    return `${CHAT_CURSOR_PREFIX}${sid}`;
  },

  getSessionId() {
    const local = String(this.data.sessionId || '').trim();
    if (local) return local;
    try {
      return String(wx.getStorageSync(this.getSessionStorageKey()) || '').trim();
    } catch (e) {
      return '';
    }
  },

  migrateCachedMessages(list) {
    const arr = Array.isArray(list) ? list : [];
    if (!arr.length) return [];
    const looksLikeOld = arr.some(m => m && (m.type === 'ai' || m.type === 'user') && typeof m.content === 'string');
    if (!looksLikeOld) return arr.filter(Boolean);
    const out = [];
    for (const m of arr) {
      const role = m && m.type === 'user' ? 'user' : 'assistant';
      const text = String(m && m.content ? m.content : '').trim();
      if (!text) continue;
      out.push({
        id: String(m.id || `legacy_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 7)}`),
        source: 'chat',
        role,
        systemMessageType: SystemMessageType.CHAT,
        text,
        ts: Date.now()
      });
    }
    return out;
  },

  trimTimelineTail(list, maxItems) {
    const arr = Array.isArray(list) ? list.filter(Boolean) : [];
    const n = Math.max(1, Number(maxItems) || 0);
    if (!n || arr.length <= n) return arr;
    return arr.slice(arr.length - n);
  },

  mergeTimeline(prev, incoming) {
    const base = Array.isArray(prev) ? prev : [];
    const add = Array.isArray(incoming) ? incoming : [];

    const normText = (s) => String(s || '').replace(/\s+/g, ' ').trim();
    const isLocalChat = (m) => {
      if (!m || !m.id) return false;
      if (String(m.source || '') !== 'chat') return false;
      if (String(m.systemMessageType || '') !== SystemMessageType.CHAT) return false;
      if (m.serverMessageId) return false;
      if (String(m.role || '') !== 'user' && String(m.role || '') !== 'assistant') return false;
      return !!normText(m.text);
    };
    const isServerChat = (m) => {
      if (!m || !m.id) return false;
      if (String(m.source || '') !== 'chat') return false;
      if (String(m.systemMessageType || '') !== SystemMessageType.CHAT) return false;
      if (!m.serverMessageId) return false;
      if (String(m.role || '') !== 'user' && String(m.role || '') !== 'assistant') return false;
      return !!normText(m.text);
    };

    const baseIndex = new Map();
    for (const m of base) {
      if (!isLocalChat(m)) continue;
      const key = `${String(m.role)}|${normText(m.text)}`;
      const list = baseIndex.get(key) || [];
      list.push({ id: String(m.id), ts: Number(m.ts || 0) });
      baseIndex.set(key, list);
    }
    for (const [k, list] of baseIndex.entries()) {
      list.sort((a, b) => a.ts - b.ts);
      baseIndex.set(k, list);
    }

    const dropIds = new Set();
    const pickClosest = (list, targetTs, maxDeltaMs) => {
      if (!Array.isArray(list) || !list.length) return null;
      let best = null;
      let bestDelta = Infinity;
      for (const it of list) {
        const d = Math.abs(Number(it.ts || 0) - Number(targetTs || 0));
        if (d < bestDelta) {
          bestDelta = d;
          best = it;
        }
      }
      if (!best) return null;
      if (!(bestDelta <= maxDeltaMs)) return null;
      return best;
    };

    for (const m of add) {
      if (!isServerChat(m)) continue;
      const key = `${String(m.role)}|${normText(m.text)}`;
      const list = baseIndex.get(key) || [];
      const matched = pickClosest(list, Number(m.ts || 0), 20000);
      if (!matched) continue;
      dropIds.add(matched.id);
      const idx = list.findIndex((x) => x && String(x.id) === String(matched.id));
      if (idx >= 0) list.splice(idx, 1);
      baseIndex.set(key, list);
    }

    const filteredBase = dropIds.size ? base.filter((m) => !(m && m.id && dropIds.has(String(m.id)))) : base;
    const map = new Map();
    for (const it of filteredBase) {
      if (!it || !it.id) continue;
      map.set(String(it.id), it);
    }
    for (const it of add) {
      if (!it || !it.id) continue;
      const id = String(it.id);
      if (!map.has(id)) {
        map.set(id, it);
      } else {
        map.set(id, { ...map.get(id), ...it });
      }
    }
    return Array.from(map.values()).sort((a, b) => {
      const at = Number(a.ts || 0);
      const bt = Number(b.ts || 0);
      if (at !== bt) return at - bt;
      return String(a.id || '').localeCompare(String(b.id || ''));
    });
  },

  normalizeSystemMessageType(v) {
    const t = String(v || '').trim();
    return t ? t : SystemMessageType.CHAT;
  },

  deriveReminderUi(subject, decisionKey, timeSlot) {
    const sub = String(subject || '').trim();
    const dk = String(decisionKey || '').trim();
    const slot = String(timeSlot || '').trim();
    const isBreakfast = sub === '早餐' || sub.indexOf('早餐') >= 0;
    const isLunch = sub === '午餐' || sub.indexOf('午餐') >= 0;
    const isDinner = sub === '晚餐' || sub.indexOf('晚餐') >= 0;
    const isMedication = dk.indexOf('medication_') >= 0 || sub.indexOf('药') >= 0 || sub === '用药';
    const isSupplement = dk.indexOf('supplement_') >= 0 || sub.indexOf('补') >= 0 || sub.indexOf('保健') >= 0;

    let reminderTone = 'default';
    let reminderIcon = '/static/aiAgentPage/clock.svg';
    let reminderTitle = '系统提醒';

    if (isBreakfast) {
      reminderTone = 'morning';
      reminderIcon = '/static/aiAgentPage/早.png';
      reminderTitle = '早餐提醒';
    } else if (isLunch) {
      reminderTone = 'noon';
      reminderIcon = '/static/aiAgentPage/中午.png';
      reminderTitle = '午餐提醒';
    } else if (isDinner) {
      reminderTone = 'night';
      reminderIcon = '/static/aiAgentPage/晚上.png';
      reminderTitle = '晚餐提醒';
    } else if (isMedication || isSupplement) {
      reminderTone = 'medicine';
      reminderIcon = '/static/aiAgentPage/药瓶.png';
      reminderTitle = isMedication ? '用药提醒' : '补剂提醒';
    } else if (sub) {
      reminderTitle = `${sub}提醒`;
    }

    const reminderSub = [sub, slot].filter(Boolean).join(' · ');
    return { reminderTone, reminderIcon, reminderTitle, reminderSub };
  },

  deriveReminderStatusPill(statusPhase, statusText) {
    const phase = String(statusPhase || '').trim().toUpperCase();
    if (phase === 'QUEUED') return '已安排';
    if (phase === 'TRIGGERED') return '到点';
    if (phase === 'COMPLETED') return '已完成';
    if (phase === 'DISABLED') return '已关闭';
    if (phase === 'SKIPPED') return '已跳过';
    const t = String(statusText || '').trim();
    if (t.indexOf('到点') >= 0) return '到点';
    return '提醒';
  },

  decorateSystemItem(item) {
    const it = item || null;
    if (!it) return it;
    const t = this.normalizeSystemMessageType(it.systemMessageType);
    if (t !== SystemMessageType.REMINDER) return it;
    const ui = this.deriveReminderUi(it.subject, it.decisionKey, it.timeSlot);
    const reminderStatusPill = this.deriveReminderStatusPill(it.statusPhase, it.statusText);
    return { ...it, ...ui, reminderStatusPill };
  },

  buildRenderTimeline(list) {
    const arr = Array.isArray(list) ? list : [];
    return arr.map((m) => this.decorateSystemItem(m));
  },

  refreshRenderTimeline(nextTimeline) {
    const base = Array.isArray(nextTimeline) ? nextTimeline : (this.data.timeline || []);
    const render = this.buildRenderTimeline(base);
    this.setData({ renderTimeline: render });
  },

  clearJustLoadedFlags(justLoadedIds) {
    const ids = justLoadedIds instanceof Set ? justLoadedIds : new Set();
    if (!ids.size) return;
    const next = (this.data.timeline || []).map((m) => {
      if (!m || !m.id) return m;
      const mid = String(m.id);
      if (!ids.has(mid)) return m;
      if (!m._justLoaded) return m;
      return { ...m, _justLoaded: false };
    });
    this.setData({ timeline: next });
    this.refreshRenderTimeline(next);
  },

  scheduleMeasureBottomOverlays(delay = 32) {
    try {
      if (this._measureBottomOverlaysTimer) clearTimeout(this._measureBottomOverlaysTimer);
      this._measureBottomOverlaysTimer = setTimeout(() => {
        this._measureBottomOverlaysTimer = null;
        this.measureBottomOverlays();
      }, delay);
    } catch (e) {}
  },

  measureBottomOverlays() {
    try {
      const inputBarGapPx = 10;
      const extraPaddingPx = 5;

      let inputHeightPx = null;
      const safeAreaBottomPx = Number(this.data.safeAreaBottom || 0);
      const tabbarBaseHeightPx = Number.isFinite(this._tabbarBaseHeightPx) ? this._tabbarBaseHeightPx : 0;

      const maybeApply = () => {
        if (inputHeightPx === null) return;

        const safeInputHeightPx = Number.isFinite(inputHeightPx) ? inputHeightPx : 0;

        const nextInputBarBottomPx = Math.max(
          0,
          Math.round(safeAreaBottomPx + tabbarBaseHeightPx + inputBarGapPx - 30)
        );
        const nextContentBottomPaddingPx = Math.max(
          0,
          Math.round(nextInputBarBottomPx + safeInputHeightPx + extraPaddingPx)
        );

        const patch = {};
        if (Math.abs(Number(this.data.inputBarBottomPx || 0) - nextInputBarBottomPx) >= 2) {
          patch.inputBarBottomPx = nextInputBarBottomPx;
        }
        if (Math.abs(Number(this.data.contentBottomPaddingPx || 0) - nextContentBottomPaddingPx) >= 2) {
          patch.contentBottomPaddingPx = nextContentBottomPaddingPx;
        }
        if (Object.keys(patch).length) this.setData(patch);
      };

      wx.createSelectorQuery()
        .in(this)
        .select('.input-bar')
        .boundingClientRect()
        .exec((res) => {
          const rect = res && res[0] ? res[0] : null;
          inputHeightPx = rect && typeof rect.height === 'number' ? rect.height : 0;
          maybeApply();
        });
    } catch (e) {}
  },

  scrollToBottomLater(delay = 60) {
    setTimeout(() => this.scrollToBottom(), delay);
  },

  scrollToBottom() {
    const list = Array.isArray(this.data.renderTimeline) ? this.data.renderTimeline : [];
    if (!list.length) return;
    const last = list[list.length - 1];
    const id = last && last.id ? String(last.id) : '';
    if (!id) return;
    this.setData({ scrollIntoView: '' });
    setTimeout(() => {
      this.setData({ scrollIntoView: `msg-${id}` });
    }, 16);
  },

  pushTimelineItem(item) {
    if (!item || !item.id) return;
    const merged = this.mergeTimeline(this.data.timeline || [], [item]);
    this.setData({ timeline: merged, scrollIntoView: `msg-${item.id}` });
    this.refreshRenderTimeline(merged);
    this.scheduleSaveChatCache();
    this.scrollToBottomLater();
  },

  updateTimelineItem(id, patch) {
    const key = String(id || '').trim();
    if (!key) return;
    const next = (this.data.timeline || []).map((m) => {
      if (!m || String(m.id) !== key) return m;
      return { ...m, ...(patch || {}) };
    });
    this.setData({ timeline: next, scrollIntoView: `msg-${key}` });
    this.refreshRenderTimeline(next);
    this.scheduleSaveChatCache();
    this.scrollToBottomLater();
  },

  updateTimelineItemTransient(id, patch) {
    const key = String(id || '').trim();
    if (!key) return;
    const next = (this.data.timeline || []).map((m) => {
      if (!m || String(m.id) !== key) return m;
      return { ...m, ...(patch || {}) };
    });
    this.setData({ timeline: next });
    this.refreshRenderTimeline(next);
  },

  removeTimelineItem(id) {
    const key = String(id || '').trim();
    if (!key) return;
    const next = (this.data.timeline || []).filter((m) => !m || String(m.id) !== key);
    this.setData({ timeline: next });
    this.refreshRenderTimeline(next);
    this.scheduleSaveChatCache();
  },

  upsertSystemTimelineItem(item) {
    if (!item) return;
    const decisionKey = String(item.decisionKey || '').trim();
    const next = Array.isArray(this.data.timeline) ? [...this.data.timeline] : [];
    if (decisionKey) {
      const idx = next.findIndex((m) => m && String(m.decisionKey || '').trim() === decisionKey);
      if (idx >= 0) {
        const prev = next[idx] || {};
        next[idx] = this.decorateSystemItem({
          ...prev,
          ...item,
          id: prev.id,
          ts: prev.ts,
          createdAt: prev.createdAt || item.createdAt,
          timeText: prev.timeText || item.timeText
        });
      } else {
        next.push(this.decorateSystemItem(item));
      }
    } else if (item.id) {
      const idx = next.findIndex((m) => m && String(m.id) === String(item.id));
      if (idx >= 0) next[idx] = this.decorateSystemItem({ ...(next[idx] || {}), ...item });
      else next.push(this.decorateSystemItem(item));
    }
    const merged = next.sort((a, b) => (Number(a && a.ts ? a.ts : 0) - Number(b && b.ts ? b.ts : 0)));
    const seen = new Set();
    const deduped = [];
    for (const m of merged) {
      if (!m) continue;
      const dk = String(m.decisionKey || '').trim();
      if (dk) {
        if (seen.has(dk)) continue;
        seen.add(dk);
      }
      deduped.push(m);
    }
    try { this.persistDailySummaryFromItem(item); } catch (e) {}
    this.setData({ timeline: deduped, scrollIntoView: item.id ? `msg-${item.id}` : '' });
    this.refreshRenderTimeline(deduped);
    this.scheduleSaveChatCache();
    this.scrollToBottomLater();
  },

  persistDailySummaryFromItem(item) {
    if (!item) return;
    const t = this.normalizeSystemMessageType(item.systemMessageType);
    if (t !== SystemMessageType.SUMMARY) return;
    const text = String(item.text || '').trim();
    if (!text) return;

    const decisionKey = String(item.decisionKey || '').trim();
    const parsed = this.parseSummaryDecisionKey(decisionKey);
    const userId = parsed.userId || this.getNumericUserId();
    const date = parsed.date || this.parseDateFromCreatedAt(item.createdAt) || this.getTodayDate();
    if (!userId || !date) return;

    let store = {};
    try { store = wx.getStorageSync(DAILY_SUMMARY_ARCHIVE_KEY) || {}; } catch (e) { store = {}; }
    if (!store || typeof store !== 'object' || Array.isArray(store)) store = {};
    const userStore = (store[userId] && typeof store[userId] === 'object' && !Array.isArray(store[userId])) ? store[userId] : {};

    const ts = Number(item.ts || 0);
    const createdAt = String(item.createdAt || '').trim();
    const entry = {
      userId,
      date,
      text,
      decisionKey,
      createdAt,
      ts: Number.isFinite(ts) ? ts : Date.now(),
      updatedAt: Date.now()
    };

    const prev = userStore[date] || null;
    const prevTs = prev ? Number(prev.ts || 0) : 0;
    const nextTs = Number(entry.ts || 0);
    const shouldReplace = !prev || !String(prev.text || '').trim() || (!Number.isFinite(prevTs) || !Number.isFinite(nextTs) ? true : nextTs >= prevTs);
    if (shouldReplace) userStore[date] = entry;
    store[userId] = userStore;
    try { wx.setStorageSync(DAILY_SUMMARY_ARCHIVE_KEY, store); } catch (e) {}
  },

  persistDailySummaryFromTimeline(list) {
    const arr = Array.isArray(list) ? list : [];
    if (!arr.length) return;
    arr.forEach((it) => {
      try { this.persistDailySummaryFromItem(it); } catch (e) {}
    });
  },

  parseSummaryDecisionKey(decisionKey) {
    const dk = String(decisionKey || '').trim();
    if (!dk) return { userId: '', date: '' };
    const m = dk.match(/^summary:([^:]+):(\d{4}-\d{2}-\d{2})(?::|$)/);
    if (!m) return { userId: '', date: '' };
    const userId = String(m[1] || '').trim();
    const date = String(m[2] || '').trim();
    return { userId, date };
  },

  parseDateFromCreatedAt(createdAt) {
    const s = String(createdAt || '').trim();
    if (!s) return '';
    const m = s.match(/^(\d{4}-\d{2}-\d{2})/);
    return m ? String(m[1] || '').trim() : '';
  },

  pushLocalChatUser(text) {
    const t = String(text || '').trim();
    if (!t) return;
    const now = Date.now();
    const u = this.data.userInfo || {};
    const userAvatar = u.avatarUrl || u.avatar || '/images/default_avatar.svg';
    this.pushTimelineItem({
      id: `local_chat_user_${now.toString(36)}_${Math.random().toString(36).slice(2, 7)}`,
      source: 'chat',
      role: 'user',
      systemMessageType: SystemMessageType.CHAT,
      text: t,
      ts: now,
      timeText: this.getCurrentTime(),
      avatar: userAvatar
    });
  },

  mapChatHistoryItemToVM(it) {
    if (!it) return null;
    const id = it.id == null ? '' : String(it.id);
    if (!id) return null;
    const role = String(it.role || '').trim() || (String(it.messageType || '').toUpperCase() === 'USER' ? 'user' : 'assistant');
    const systemMessageType = String(it.systemMessageType || '').trim() || SystemMessageType.CHAT;
    const text = String(it.content || '').trim();
    if (!text && (systemMessageType === SystemMessageType.CHAT || systemMessageType === SystemMessageType.PROACTIVE_INQUIRY)) return null;
    const tsStr = String(it.timestamp || '').trim();
    const ts = tsStr ? this.parseTimestampMillis(tsStr) : Date.now();
    const timeText = tsStr && tsStr.length >= 16 ? tsStr.slice(11, 16) : this.getCurrentTime();
    const u = this.data.userInfo || {};
    const userAvatar = u.avatarUrl || u.avatar || '/images/default_avatar.svg';
    const explanationCode = String(it.explanationCode || '').trim();
    const explanationArgs = it.explanationArgs || null;
    const baseVm = {
      serverMessageId: id,
      source: 'chat',
      role,
      systemMessageType,
      text,
      ts: Number.isFinite(ts) ? ts : Date.now(),
      timeText,
      avatar: role === 'assistant' ? AI_CHAT_AVATAR_URL : userAvatar,
      explanationCode,
      explanationArgs
    };
    if (systemMessageType === SystemMessageType.CHAT || systemMessageType === SystemMessageType.PROACTIVE_INQUIRY) {
      return { ...baseVm, id: `srv_chat_${id}` };
    }

    const metaDecisionKey = String(it.decisionKey || '').trim();
    const metaTimeSlot = String(it.timeSlot || '').trim();
    const metaSubject = String(it.subject || '').trim();
    const parsedArgs = this.safeParseJson(explanationArgs);

    let decisionKey = metaDecisionKey;
    let timeSlot = metaTimeSlot;
    let subject = metaSubject;
    if (parsedArgs && typeof parsedArgs === 'object') {
      if (!decisionKey && parsedArgs.decisionKey) decisionKey = String(parsedArgs.decisionKey || '').trim();
      if (!timeSlot && parsedArgs.timeSlot) timeSlot = String(parsedArgs.timeSlot || '').trim();
      if (!subject && parsedArgs.subject) subject = String(parsedArgs.subject || '').trim();
      if (!subject && parsedArgs.itemKey) subject = String(parsedArgs.itemKey || '').trim();
    }

    const sysVm = {
      ...baseVm,
      id: `srv_sys_${id}`,
      role: 'system',
      decisionKey,
      timeSlot,
      subject
    };
    if (systemMessageType === SystemMessageType.REMINDER) {
      const statusText = text || (subject ? `到点提醒：${subject}` : '到点提醒');
      return {
        ...sysVm,
        statusPhase: 'TRIGGERED',
        statusText,
        primaryActionLabel: this.getReminderPrimaryActionLabel(decisionKey, subject),
        canComplete: String(decisionKey).indexOf('reminder:') === 0
      };
    }
    if (systemMessageType === SystemMessageType.PLAN_FEEDBACK) {
      const statusText = text || '已记录你的安排';
      return { ...sysVm, statusText };
    }
    if (systemMessageType === SystemMessageType.IMAGE_ANALYSIS) {
      const items = parsedArgs && Array.isArray(parsedArgs.items) ? parsedArgs.items : [];
      const recorded = parsedArgs && (parsedArgs.recorded === true || parsedArgs.recorded === 'true');
      const brochureRecorded = parsedArgs && (parsedArgs.brochureRecorded === true || parsedArgs.brochureRecorded === 'true');
      const brochureMealType = parsedArgs && parsedArgs.brochureMealType ? String(parsedArgs.brochureMealType || '') : '';
      return { ...sysVm, analysisItems: items, payload: parsedArgs || {}, recorded, brochureRecorded, brochureMealType };
    }
    return sysVm;
  },

  safeParseJson(input) {
    if (!input) return null;
    if (typeof input === 'object') return input;
    const s = String(input || '').trim();
    if (!s) return null;
    try { return JSON.parse(s); } catch (e) { return null; }
  },

  getNumericUserId() {
    const u = this.data.userInfo || {};
    const raw = (u.id || u.userId || '').toString().trim();
    if (/^\d+$/.test(raw)) return raw;
    let cached = {};
    try {
      cached = wx.getStorageSync('user_cache') || wx.getStorageSync('userProfile') || wx.getStorageSync('userInfo') || {};
    } catch (e) {
      cached = {};
    }
    const id = cached && (cached.id || cached.userId || cached.uid) != null ? String(cached.id || cached.userId || cached.uid).trim() : '';
    return /^\d+$/.test(id) ? id : '';
  },

  refreshRuntimeState({ silent } = {}) {
    return request.get('/user/runtime-state', {}, { loading: false, showError: !silent })
      .then((body) => {
        const snap = (body && body.data) ? body.data : null;
        this.setData({ runtimeState: snap, runtimeStateUpdatedAt: Date.now() });
        try { wx.setStorageSync(RUNTIME_STATE_KEY, snap); } catch (e) {}
        this.applyRuntimeFlags(snap);
        return snap;
      })
      .catch(() => {
        try {
          const cached = wx.getStorageSync(RUNTIME_STATE_KEY);
          if (cached) {
            this.setData({ runtimeState: cached });
            this.applyRuntimeFlags(cached);
          }
        } catch (e) {}
        return null;
      });
  },

  applyRuntimeFlags(snap) {
    const lastImageId = snap && snap.lastImageEvent ? snap.lastImageEvent.eventId : null;
    const hasLastImage = !!lastImageId;
    const next = (this.data.timeline || []).map((m) => {
      if (!m) return m;
      if (String(m.systemMessageType) === SystemMessageType.IMAGE_ANALYSIS) {
        return { ...m, recorded: hasLastImage };
      }
      return m;
    });
    this.setData({ timeline: next });
    this.refreshRenderTimeline(next);
    this.updateCapabilityGateState(snap);
  },

  updateCapabilityGateState(snap) {
    const states = (snap && snap.capability && snap.capability.states) ? snap.capability.states : {};
    if (!this.data.capabilityBlocked) return;
    const required = String(this.data.capabilityGuideKey || '').trim();
    const proactiveOk = String(states.PROACTIVE_REMINDER || '').trim() === 'ENABLED';
    const reqOk = !required || String(states[required] || '').trim() === 'ENABLED';
    if (proactiveOk && reqOk) {
      const next = (this.data.timeline || []).filter((m) => !m || String(m.systemMessageType) !== SystemMessageType.CAPABILITY_GUIDE);
      this.setData({ capabilityBlocked: false, capabilityGuideKey: '', timeline: next });
      this.refreshRenderTimeline(next);
      this.scheduleSaveChatCache();
    }
  },

  restoreChatFromCache() {
    const key = this.getChatCacheKey();
    if (!key || key === CHAT_CACHE_PREFIX) return;
    let list = [];
    try {
      const v = wx.getStorageSync(key);
      list = Array.isArray(v) ? v : [];
    } catch (e) {
      list = [];
    }
    if (Array.isArray(list) && list.length) {
      const migrated = this.migrateCachedMessages(list);
      const sorted = migrated.slice().sort((a, b) => {
        const at = Number(a && a.ts ? a.ts : 0);
        const bt = Number(b && b.ts ? b.ts : 0);
        if (at !== bt) return at - bt;
        return String(a && a.id ? a.id : '').localeCompare(String(b && b.id ? b.id : ''));
      });
      const trimmed = this.trimTimelineTail(sorted, CHAT_CACHE_MAX_ITEMS);
      const patch = { timeline: trimmed };
      if (trimmed.length) {
        patch.scrollIntoView = `msg-${trimmed[trimmed.length - 1].id}`;
      }
      const ids = trimmed
        .filter(m => m && m.serverMessageId && String(m.serverMessageId).trim())
        .map(m => Number(m.serverMessageId))
        .filter(n => Number.isFinite(n));
      if (ids.length) {
        patch.historyMinServerId = String(Math.min.apply(null, ids));
      }
      this.setData(patch);
      this.refreshRenderTimeline(trimmed);
      try { this.persistDailySummaryFromTimeline(trimmed); } catch (e) {}
      try { wx.setStorageSync(key, trimmed); } catch (e) {}
      this.scrollToBottomLater();
    }
  },

  saveChatCache() {
    try {
      const key = this.getChatCacheKey();
      if (!key || key === CHAT_CACHE_PREFIX) return;
      const list = Array.isArray(this.data.timeline) ? this.data.timeline : [];
      wx.setStorageSync(key, this.trimTimelineTail(list, CHAT_CACHE_MAX_ITEMS));
      if (this._chatCacheTimer) {
        try { clearTimeout(this._chatCacheTimer); } catch (e) {}
        this._chatCacheTimer = null;
      }
    } catch (e) {}
  },

  ensureChatCursorFromCache() {
    try {
      const cursorKey = this.getChatCursorKey();
      if (!cursorKey || cursorKey === CHAT_CURSOR_PREFIX) return;
      let cursor = '';
      try { cursor = wx.getStorageSync(cursorKey) || ''; } catch (e) { cursor = ''; }
      if (cursor) return;

      const list = Array.isArray(this.data.timeline) ? this.data.timeline : [];
      const ids = list
        .filter(m => m && m.serverMessageId && String(m.serverMessageId).trim())
        .map(m => Number(m.serverMessageId))
        .filter(n => Number.isFinite(n));
      if (!ids.length) return;

      const maxId = Math.max.apply(null, ids);
      const minId = Math.min.apply(null, ids);
      if (Number.isFinite(maxId)) {
        try { wx.setStorageSync(cursorKey, String(maxId)); } catch (e) {}
      }
      if (Number.isFinite(minId)) {
        this.setData({ historyMinServerId: String(minId) });
      }
    } catch (e) {}
  },

  restoreFactFromCache() {
    const key = `${FACT_CACHE_PREFIX}${this.data.sessionId || ''}`;
    if (!key || key === FACT_CACHE_PREFIX) return;
    let list = [];
    try {
      const v = wx.getStorageSync(key);
      list = Array.isArray(v) ? v : [];
    } catch (e) {
      list = [];
    }
    if (Array.isArray(list) && list.length) {
      const normalized = this.normalizeFactRecords(list);
      this.setData({ factRecords: normalized });
    }
  },

  normalizeFactRecords(list) {
    const arr = Array.isArray(list) ? list : [];
    return arr.map((rec) => {
      const items = Array.isArray(rec && rec.items) ? rec.items : [];
      return {
        ...rec,
        items: items.map((it) => this.normalizeFoodCandidate(it))
      };
    });
  },

  // 已弃用：不再本地伪造 IMAGE_ANALYSIS 卡片

  normalizeFoodCandidate(it) {
    const name = String(it && it.name ? it.name : '').trim();
    const calorie = it && typeof it === 'object' ? it.calorie : undefined;
    const probability = it && typeof it === 'object' ? it.probability : undefined;

    const calNum = typeof calorie === 'number' && isFinite(calorie) ? calorie : null;
    const probNum = typeof probability === 'number' && isFinite(probability) ? probability : null;

    const calorieText = calNum === null ? '--' : `${Math.round(calNum)} kcal`;
    const probabilityText = probNum === null ? '--' : `${Math.round(probNum * 100)}%`;

    return {
      ...it,
      name,
      calorie: calNum,
      probability: probNum,
      calorieText,
      probabilityText
    };
  },

  getTodayDate() {
    const d = new Date();
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${dd}`;
  },

  inferMealTypeForBrochure() {
    const list = Array.isArray(this.data.timeline) ? this.data.timeline : [];
    for (let i = list.length - 1; i >= 0; i--) {
      const m = list[i];
      if (!m) continue;
      if (String(m.systemMessageType) !== SystemMessageType.FACT_RECORD) continue;
      const subject = String(m.subject || '').trim();
      if (subject.includes('早') || subject.includes('早餐')) return 'BREAKFAST';
      if (subject.includes('午') || subject.includes('午餐')) return 'LUNCH';
      if (subject.includes('晚') || subject.includes('晚餐')) return 'DINNER';
      break;
    }

    const h = new Date().getHours();
    if (h < 10) return 'BREAKFAST';
    if (h < 16) return 'LUNCH';
    return 'DINNER';
  },

  async ensureNutritionMemberId() {
    try {
      const cached = wx.getStorageSync('nutrition_selected_member_id');
      const id = Number(cached);
      if (Number.isFinite(id) && id > 0) return id;
    } catch (e) {}

    try {
      const body = await request.get('/nutrition/members', {}, { loading: false, showError: false });
      const list = body && Array.isArray(body.data) ? body.data : [];
      if (!list.length) return null;

      const preferredKey = (() => {
        try { return String(wx.getStorageSync('nutrition_selected_member') || '').trim(); } catch (e) { return ''; }
      })();
      let picked = preferredKey ? list.find((m) => String(m && m.memberKey ? m.memberKey : '') === preferredKey) : null;
      if (!picked) picked = list.find((m) => String(m && m.memberKey ? m.memberKey : '') === 'me') || list[0];
      const id = picked && picked.id != null ? Number(picked.id) : null;
      if (!Number.isFinite(id) || id <= 0) return null;
      try { wx.setStorageSync('nutrition_selected_member_id', id); } catch (e) {}
      if (picked && picked.memberKey) {
        try { wx.setStorageSync('nutrition_selected_member', String(picked.memberKey)); } catch (e) {}
      }
      return id;
    } catch (e) {
      return null;
    }
  },

  patchLocalNutritionBrochureCache({ mealType, date, foods }) {
    const type = String(mealType || '').trim();
    if (!type) return false;
    const dt = String(date || this.getTodayDate());
    const list = Array.isArray(foods) ? foods : [];
    if (!list.length) return false;

    const memberKey = (() => {
      try { return String(wx.getStorageSync('nutrition_selected_member') || 'me'); } catch (e) { return 'me'; }
    })();
    let store = {};
    try { store = wx.getStorageSync(NUTRITION_MEAL_DATA_KEY) || {}; } catch (e) { store = {}; }
    const byMember = store && typeof store === 'object' ? store : {};
    const node = (byMember[memberKey] && typeof byMember[memberKey] === 'object') ? byMember[memberKey] : {};
    const day = (node[dt] && typeof node[dt] === 'object') ? node[dt] : {};

    const field = type === 'BREAKFAST' ? 'breakfastFoods' : (type === 'LUNCH' ? 'lunchFoods' : 'dinnerFoods');
    const existing = Array.isArray(day[field]) ? day[field] : [];

    const normalize = (f) => {
      const item = (f && typeof f === 'object') ? f : {};
      return {
        name: String(item.name || '').trim(),
        unit: String(item.unit || '').trim(),
        nutrient: String(item.nutrient || '').trim(),
        img: String(item.img || '').trim(),
        note: String(item.note || '').trim(),
        calorie: Math.max(0, Number(item.calorie) || 0),
        carb: Number(item.carb) || 0,
        protein: Number(item.protein) || 0,
        fat: Number(item.fat) || 0
      };
    };

    const append = list.map(normalize).filter((f) => f.name).slice(0, 8);
    if (!append.length) return false;

    const mergedFoods = [...append, ...existing];
    const nextDay = {
      ...day,
      breakfastFoods: Array.isArray(day.breakfastFoods) ? day.breakfastFoods : [],
      lunchFoods: Array.isArray(day.lunchFoods) ? day.lunchFoods : [],
      dinnerFoods: Array.isArray(day.dinnerFoods) ? day.dinnerFoods : [],
      snackCards: Array.isArray(day.snackCards) ? day.snackCards : []
    };
    nextDay[field] = mergedFoods;

    const nextNode = { ...node, [dt]: nextDay };
    const next = { ...byMember, [memberKey]: nextNode };
    try { wx.setStorageSync(NUTRITION_MEAL_DATA_KEY, next); } catch (e) {}
    return true;
  },

  saveFactCache() {
    try {
      const key = `${FACT_CACHE_PREFIX}${this.data.sessionId || ''}`;
      if (!key || key === FACT_CACHE_PREFIX) return;
      wx.setStorageSync(key, Array.isArray(this.data.factRecords) ? this.data.factRecords : []);
    } catch (e) {}
  },

  scheduleSaveChatCache() {
    if (this._chatCacheTimer) {
      try { clearTimeout(this._chatCacheTimer); } catch (e) {}
    }
    this._chatCacheTimer = setTimeout(() => {
      try {
        const key = this.getChatCacheKey();
        if (!key || key === CHAT_CACHE_PREFIX) return;
        const list = Array.isArray(this.data.timeline) ? this.data.timeline : [];
        wx.setStorageSync(key, this.trimTimelineTail(list, CHAT_CACHE_MAX_ITEMS));
      } catch (e) {}
    }, 400);
  },

  ensureWelcomeMessage() {
    const list = Array.isArray(this.data.timeline) ? this.data.timeline : [];
    if (list.length) return;

    const cached = this.readOnboardingState();
    if (cached && cached.active === true) {
      const next = { ...(cached || {}), kickoffSent: false, updatedAt: Date.now() };
      this._onboardingState = next;
      this.writeOnboardingState(next);
      this._sessionMode = 'onboarding';
      this.ensureSessionId();
      this.kickoffOnboardingAI();
      return;
    }

    let done = false;
    try { done = !!wx.getStorageSync(this.getOnboardingDoneKey()); } catch (e) { done = false; }

    let profileCompleted = false;
    try {
      const map = wx.getStorageSync('profile_completed_uids') || {};
      if (map && typeof map === 'object') profileCompleted = !!map[this.getUserKey()];
    } catch (e) { profileCompleted = false; }

    const force = this.isOnboardingMode();
    if (force) {
      this.startOnboarding({ force: true });
      return;
    }

    if (!done && !profileCompleted) {
      this.startVoiceOnboarding();
    }
  },

  syncChatHistoryFromServer({ silent } = {}) {
    return new Promise((resolve) => {
      try {
        const sessionId = this.data.sessionId;
        if (!sessionId) { resolve(false); return; }

        const cursorKey = this.getChatCursorKey();
        let cursor = '';
        try { cursor = wx.getStorageSync(cursorKey) || ''; } catch (e) { cursor = ''; }
        const params = { sessionId, limit: CHAT_INITIAL_PULL_LIMIT };
        if (cursor) params.cursor = cursor;
        request.get('/chat/history', params, { loading: false, showError: false })
          .then((body) => {
            const data = (body && (body.data || body)) || {};
            const items = Array.isArray(data.items) ? data.items : [];
            const nextCursor = data.nextCursor || '';
            if (nextCursor) {
              try { wx.setStorageSync(cursorKey, nextCursor); } catch (e) {}
            }

            if (!items.length) { resolve(true); return; }

            const mapped = items.map((it) => this.mapChatHistoryItemToVM(it)).filter(Boolean);
            const merged = this.mergeTimeline(this.data.timeline || [], mapped);
            if (merged.length !== (this.data.timeline || []).length) {
              const capped = this.trimTimelineTail(merged, 240);
              const last = merged[merged.length - 1];
              const patch = { timeline: capped };
              if (last && last.id) patch.scrollIntoView = `msg-${last.id}`;
              const ids = mapped
                .map((m) => Number(m && m.serverMessageId ? m.serverMessageId : NaN))
                .filter((n) => Number.isFinite(n));
              if (ids.length) {
                const minId = Math.min.apply(null, ids);
                const prevMin = Number(this.data.historyMinServerId || NaN);
                if (!Number.isFinite(prevMin) || minId < prevMin) patch.historyMinServerId = String(minId);
              }
              patch.historyHasMore = true;
              this.setData(patch);
              this.refreshRenderTimeline(capped);
              this.scheduleSaveChatCache();
            }
            resolve(true);
          })
          .catch(() => resolve(false));
      } catch (e) {
        if (!silent) {
          try { wx.showToast({ title: '历史记录拉取失败', icon: 'none' }); } catch (err) {}
        }
        resolve(false);
      }
    });
  },

  loadOlderChatHistoryFromServer({ silent } = {}) {
    return new Promise((resolve) => {
      try {
        const sessionId = this.data.sessionId;
        if (!sessionId) { resolve(false); return; }
        if (this._olderHistoryLoading) { resolve(false); return; }
        if (this.data.historyHasMore === false) { resolve(true); return; }

        let beforeId = String(this.data.historyMinServerId || '').trim();
        if (!beforeId) {
          const ids = (this.data.timeline || [])
            .filter((m) => m && m.serverMessageId && String(m.serverMessageId).trim())
            .map((m) => Number(m.serverMessageId))
            .filter((n) => Number.isFinite(n));
          if (ids.length) beforeId = String(Math.min.apply(null, ids));
        }
        if (!beforeId) { this.setData({ historyHasMore: false }); resolve(true); return; }

        const prevFirst = (() => {
          const list = Array.isArray(this.data.renderTimeline) ? this.data.renderTimeline : [];
          return list.length ? String(list[0].id || '') : '';
        })();

        this._olderHistoryLoading = true;
        this.setData({ loadingOlderHistory: true });
        request.get('/chat/history', { sessionId, limit: 50, before: beforeId }, { loading: false, showError: false })
          .then((body) => {
            const data = (body && (body.data || body)) || {};
            const items = Array.isArray(data.items) ? data.items : [];
            const nextCursor = String(data.nextCursor || '').trim();
            if (!items.length) {
              this.setData({ historyHasMore: false });
              resolve(true);
              return;
            }

            const mapped = items.map((it) => this.mapChatHistoryItemToVM(it)).filter(Boolean);
            const prevTimeline = Array.isArray(this.data.timeline) ? this.data.timeline : [];
            const merged = this.mergeTimeline(prevTimeline, mapped);
            if (!mapped.length || merged.length === prevTimeline.length) {
              this.setData({ historyHasMore: false });
              resolve(true);
              return;
            }

            const prevIds = new Set(prevTimeline.filter(Boolean).map((m) => String(m.id || '')));
            const justLoadedIds = new Set();
            merged.forEach((m) => {
              if (!m || !m.id) return;
              const mid = String(m.id);
              if (!prevIds.has(mid)) justLoadedIds.add(mid);
            });
            const mergedWithFlags = justLoadedIds.size
              ? merged.map((m) => (m && m.id && justLoadedIds.has(String(m.id)) ? { ...m, _justLoaded: true } : m))
              : merged;

            const patch = { timeline: mergedWithFlags, historyHasMore: true };
            if (nextCursor) patch.historyMinServerId = nextCursor;
            if (prevFirst) patch.scrollIntoView = `msg-${prevFirst}`;
            this.setData(patch);
            this.refreshRenderTimeline(mergedWithFlags);
            this.scheduleSaveChatCache();
            if (justLoadedIds.size) {
              setTimeout(() => {
                try { this.clearJustLoadedFlags(justLoadedIds); } catch (e) {}
              }, 260);
            }
            resolve(true);
          })
          .catch(() => {
            if (!silent) {
              try { wx.showToast({ title: '历史记录拉取失败', icon: 'none' }); } catch (err) {}
            }
            resolve(false);
          })
          .finally(() => {
            this._olderHistoryLoading = false;
            try { this.setData({ loadingOlderHistory: false }); } catch (e) {}
          });
      } catch (e) {
        this._olderHistoryLoading = false;
        try { this.setData({ loadingOlderHistory: false }); } catch (err) {}
        resolve(false);
      }
    });
  },

  onScrollToUpper() {
    const now = Date.now();
    if (this._olderHistoryLoading) return;
    if (this._upperArmed === false) return;
    if (this._lastUpperLoadAt && now - this._lastUpperLoadAt < 800) return;
    this._upperArmed = false;
    this._lastUpperLoadAt = now;
    this.loadOlderChatHistoryFromServer({ silent: true });
  },

  onScroll(e) {
    try {
      const top = e && e.detail && typeof e.detail.scrollTop === 'number' ? e.detail.scrollTop : NaN;
      if (Number.isFinite(top)) {
        this._lastScrollTop = top;
        if (top > 60) this._upperArmed = true;
      }
    } catch (err) {}
  },

  // 频谱彩色渐变初始化
  initSpectrumColors(n = 24) {
    const colors = [];
    for (let i = 0; i < n; i++) {
      const hue = Math.round(200 + (i / n) * 140); // 从蓝绿到紫粉的区间
      colors.push(`linear-gradient(180deg, hsl(${hue}, 85%, 65%), hsl(${hue}, 80%, 55%))`);
    }
    this.setData({ userColors: colors });
  },

  // ===== AI电话相关 =====
  initCallAudio() {
    try {
      this.recorderManager = wx.getRecorderManager();
      this.recorderManager.onStart(() => {
        this.setData({ isUserSpeaking: true });
      });
      this.recorderManager.onStop((res) => {
        this.setData({ isUserSpeaking: false });
        this._resumeCallVideo();
        if (this._voiceFallbackSpectrumTimer) {
          try { clearInterval(this._voiceFallbackSpectrumTimer); } catch (e) {}
          this._voiceFallbackSpectrumTimer = null;
        }
        if (this._ignoreNextVoiceStop) {
          this._ignoreNextVoiceStop = false;
          return;
        }
        if (this._voiceInputMode) {
          this._voiceInputMode = false;
          this.handleVoiceInputStop(res || {});
        } else if (this.data.callStatus !== 'connected') {
          this.simulateAISpeech();
        }
      });
      // 分片录音，使用帧数据驱动用户频谱
      this.recorderManager.onFrameRecorded((res) => {
        const buffer = res.frameBuffer;
        this.updateUserSpectrum(buffer);
      });
    } catch (e) {
      console.warn('初始化录音管理器失败', e);
    }
  },

  initTTSAudio() {
    try {
      this.ttsAudio = wx.createInnerAudioContext();
      this.ttsAudio.autoplay = false;
      this.ttsAudio.obeyMuteSwitch = false;
      if (!this._ttsQueue) this._ttsQueue = [];
      this._ttsProcessing = false;
      this.ttsAudio.onPlay(() => {
        this._isTtsAudiblyPlaying = true;
        this._lastTtsStartedAt = Date.now();
        if (this._voiceInputMode) {
          this._ignoreNextVoiceStop = true;
          this._voiceInputMode = false;
          this._clearVoiceRecordTimer();
          try { this.recorderManager && this.recorderManager.stop(); } catch (e) {}
          try { this.setData({ isHoldingVoice: false, voiceRecordDuration: 0 }); } catch (e) {}
          if (this._voiceFallbackSpectrumTimer) {
            try { clearInterval(this._voiceFallbackSpectrumTimer); } catch (e) {}
            this._voiceFallbackSpectrumTimer = null;
          }
        }
        this.setData({ isAISpeaking: true });
        try {
          const activeId = String(this.data.ttsActiveId || '');
          if (activeId && this._ttsPlayingTaskId && this._ttsPlayingTaskId === this._ttsActiveTaskId) {
            this.setData({ ttsActivePhase: 'playing' });
          }
        } catch (e) {}
        this.aiInterval = setInterval(() => {
          const target = this.randomBars(this.data.aiSpectrum.length);
          const bars = this.smoothBars(this.data.aiSpectrum, target, 0.6);
          this.setData({ aiSpectrum: bars });
        }, 80);
      });
      const stopSpeak = () => {
        const finishedTaskId = this._ttsPlayingTaskId || 0;
        const audioPath = this._ttsCurrentAudioPath || '';
        this._ttsPlayingTaskId = 0;
        this._ttsCurrentAudioPath = '';
        this._isTtsAudiblyPlaying = false;
        this._lastTtsEndedAt = Date.now();
        if (this.aiInterval) { clearInterval(this.aiInterval); this.aiInterval = null; }
        this.setData({ isAISpeaking: false, aiSpectrum: new Array(this.data.aiSpectrum.length).fill(10) });
        this._ttsProcessing = false;
        try { this._unlinkUserDataFileSafe && this._unlinkUserDataFileSafe(audioPath); } catch (e) {}
        if (finishedTaskId && finishedTaskId === this._ttsActiveTaskId) {
          this._playNextTTSFromQueue && this._playNextTTSFromQueue(finishedTaskId);
        }
        try {
          if (!this._ttsProcessing && (!this._ttsQueue || !this._ttsQueue.length)) {
            if (finishedTaskId && finishedTaskId === this._ttsActiveTaskId) {
              this._finishTtsUi && this._finishTtsUi(finishedTaskId);
            }
            this.maybeAutoStartCallVoiceInput && this.maybeAutoStartCallVoiceInput();
          }
        } catch (e) {}
      };
      this.ttsAudio.onEnded(stopSpeak);
      this.ttsAudio.onStop(stopSpeak);
      this.ttsAudio.onError((err) => {
        console.error('ttsAudio onError', err);
        stopSpeak();
        try {
          this.showTtsPlayError('innerAudio.onError', err, { audioPath: this._ttsCurrentAudioPath || '' });
        } catch (e) {}
      });
    } catch (e) {}
  },

  _unlinkUserDataFileSafe(filePath) {
    const fp = String(filePath || '').trim();
    if (!fp) return;
    if (!/^wxfile:\/\//.test(fp) && fp.indexOf(wx.env.USER_DATA_PATH) !== 0) return;
    try {
      const fs = wx.getFileSystemManager();
      fs.unlink({ filePath: fp, fail: () => {} });
    } catch (e) {}
  },

  _finishTtsUi(taskId) {
    if (taskId && this._ttsActiveTaskId && taskId !== this._ttsActiveTaskId) return;
    this.setData({ ttsActiveId: '', ttsActivePhase: '' });
  },

  safeStopTtsAudio() {
    try {
      if (!this.ttsAudio) return false;
      const src = this.ttsAudio.src;
      const hasSrc = !!(src && String(src).trim());
      if (!hasSrc && !this._isTtsAudiblyPlaying) return false;
      this.ttsAudio.stop();
      return true;
    } catch (e) {
      return false;
    }
  },

  cancelTTS() {
    const nextTaskId = (this._ttsActiveTaskId || 0) + 1;
    this._ttsActiveTaskId = nextTaskId;
    const prefetched = this._ttsPrefetchedByIdx || {};
    const inflight = this._ttsInflightByIdx || {};
    this._ttsQueue = [];
    this._ttsProcessing = false;
    this._ttsPlayingTaskId = 0;
    const audioPath = this._ttsCurrentAudioPath || '';
    this._ttsCurrentAudioPath = '';
    this._ttsCurrentSegIdx = -1;
    this._ttsPrefetchedByIdx = {};
    this._ttsInflightByIdx = {};
    this._finishTtsUi(nextTaskId);
    this.safeStopTtsAudio();
    try { this._unlinkUserDataFileSafe && this._unlinkUserDataFileSafe(audioPath); } catch (e) {}
    try {
      const paths = Object.values(prefetched || {});
      for (const p of paths) {
        try { this._unlinkUserDataFileSafe && this._unlinkUserDataFileSafe(p); } catch (e) {}
      }
    } catch (e) {}
    try {
      const inflightValues = Object.values(inflight || {});
      for (const v of inflightValues) {
        if (v && typeof v.cancel === 'function') {
          try { v.cancel(); } catch (e) {}
        }
      }
    } catch (e) {}
  },

  normalizeTextForTTS(text) {
    let raw = String(text || '');
    raw = raw.replace(/\r\n/g, '\n').replace(/\r/g, '\n');
    raw = raw.replace(/\*\*(.*?)\*\*/g, '$1');
    raw = raw.replace(/__(.*?)__/g, '$1');
    raw = raw.replace(/~~(.*?)~~/g, '$1');
    raw = raw.replace(/`+/g, '');
    raw = raw.replace(/\[(.*?)\]\([^)]+\)/g, '$1');
    raw = raw.replace(/#+\s*/g, '');
    raw = raw.replace(/>\s*/g, '');
    raw = raw.replace(/\n{2,}/g, '\n');
    raw = raw.replace(/\n/g, '。');
    raw = raw.replace(/\s+/g, ' ').trim();
    return raw;
  },

  maybeAutoStartCallVoiceInput() {
    if (!this._callAutoListenEnabled) return false;
    if (!this.data.isCalling || this.data.callStatus !== 'connected') return false;
    if (this.data.isMicMuted) return false;
    if (this._voiceInputMode) return false;
    if (this._ttsProcessing) return false;
    if (this._isTtsAudiblyPlaying) return false;
    const suppressUntil = this._suppressAutoListenUntil || 0;
    if (Date.now() < suppressUntil) return false;

    if (this._autoListenTimer) {
      try { clearTimeout(this._autoListenTimer); } catch (e) {}
      this._autoListenTimer = null;
    }
    const now = Date.now();
    const lastTtsEndedAt = Number(this._lastTtsEndedAt || 0);
    const minGapMs = 900;
    const elapsed = lastTtsEndedAt ? (now - lastTtsEndedAt) : minGapMs;
    const extraDelay = elapsed < minGapMs ? (minGapMs - elapsed) : 0;
    const delayMs = Math.max(650, extraDelay);
    this._autoListenTimer = setTimeout(() => {
      this._autoListenTimer = null;
      try { this.startCallVoiceInput && this.startCallVoiceInput(); } catch (e) {}
    }, delayMs);
    return true;
  },

  onStartAIPhoneCall() {
    if (this.data.isCalling) return;
    try { wx.hideTabBar({ animation: false }); } catch (e) {}
    this.setData({ isCalling: true, callStatus: 'connecting', callDuration: 0, callDurationStr: '00:00' });
    this._callGreeted = false;
    this.setData({ callLastUserText: '', callLastAiText: '' });
    this._callAutoListenEnabled = true;
    try { wx.vibrateShort({ type: 'light' }); } catch (e) {}
    try { wx.setKeepScreenOn({ keepScreenOn: true }); } catch (e) {}
    // 自动启动录音与频谱动画循环
    this.startSpectrumAnimLoop();
    // 接通模拟
      setTimeout(() => {
        if (!this.data.isCalling) return;
        this.setData({ callStatus: 'connected' });
        try { wx.vibrateShort({ type: 'medium' }); } catch (e) {}
        this.callTimer = setInterval(() => this.tickCallTimer(), 1000);
        if (!this._callGreeted) {
          this._callGreeted = true;
          if (this.data.isVoiceOnboarding) {
            const stepDef = this.getCurrentVoiceOnboardingStep();
            const greet = stepDef ? stepDef.question : '你好呀，我是你的专属AI营养师小云，我们开始吧。';
            this.setData({ callLastAiText: greet });
            this.playTTS(greet);
          } else {
            const greet = '您好呀，我是小云，今天有什么想聊的吗？';
            this.setData({ callLastAiText: greet });
            this.playTTS(greet);
          }
        }
      }, 800);
  },

  endAIPhoneCall() {
    try { this.recorderManager && this.recorderManager.stop(); } catch (e) {}
    this.stopAISpectrum();
    this.stopSpectrumAnimLoop();
    if (this.callTimer) { clearInterval(this.callTimer); this.callTimer = null; }
    this._callAutoListenEnabled = false;
    if (this._autoListenTimer) {
      try { clearTimeout(this._autoListenTimer); } catch (e) {}
      this._autoListenTimer = null;
    }
    this._clearVoiceRecordTimer();
    try { this._resumeCallVideo(); } catch (e) {}
    try { wx.setKeepScreenOn({ keepScreenOn: false }); } catch (e) {}
    try { wx.vibrateShort({ type: 'light' }); } catch (e) {}
    this.setData({ callStatus: 'ended', isUserSpeaking: false, voiceRecordDuration: 0 });
    try { this.cancelTTS && this.cancelTTS(); } catch (e) {}
    this._callGreeted = false;
    setTimeout(() => {
      this.setData({ isCalling: false });
      this.scheduleMeasureBottomOverlays(80);
      try { wx.hideTabBar({ animation: false }); } catch (e) {}
    }, 200);
  },

  _pauseCallVideo() {
    try {
      if (!this._callAvatarVideoContext && typeof wx.createVideoContext === 'function') {
        this._callAvatarVideoContext = wx.createVideoContext('callAvatarVideo', this);
      }
      const ctx = this._callAvatarVideoContext;
      if (ctx && typeof ctx.pause === 'function') ctx.pause();
    } catch (e) {}
  },

  _resumeCallVideo() {
    try {
      const ctx = this._callAvatarVideoContext;
      if (!ctx) return;
      if (typeof ctx.play === 'function') ctx.play();
    } catch (e) {}
  },

  toggleMic() {
    if (!this.data.isUserSpeaking) {
      this.startRecorder();
    } else {
      try { this.recorderManager && this.recorderManager.stop(); } catch (e) {}
    }
  },

  toggleMicMute() {
    const next = !this.data.isMicMuted;
    this.setData({ isMicMuted: next });
    if (next) {
      wx.showToast({ title: '麦克风已关闭', icon: 'none' });
    } else {
      wx.showToast({ title: '麦克风已开启', icon: 'none' });
      try {
        if (this.data.isCalling && this.data.callStatus === 'connected') {
          this.maybeAutoStartCallVoiceInput && this.maybeAutoStartCallVoiceInput();
        }
      } catch (e) {}
    }
  },

  toggleSpeaker() {
    const next = !this.data.isSpeakerOn;
    this.setData({ isSpeakerOn: next });
    wx.showToast({ title: next ? '扬声器已开启' : '扬声器已关闭', icon: 'none' });
    if (!next) { try { this.cancelTTS && this.cancelTTS(); } catch (e) {} }
  },

  splitTextForTTS(text, maxLen = 120) {
    const raw = this.normalizeTextForTTS(text);
    if (!raw) return [];
    const limit = Math.max(40, Number(maxLen) || 90);
    if (raw.length <= limit) return [raw];
    const boundaries = new Set(['。', '！', '？', '!', '?', '；', ';', '…']);
    const minLen = Math.max(24, Math.floor(limit * 0.55));
    const parts = [];
    let buf = '';
    let lastBoundaryPos = -1;
    for (let i = 0; i < raw.length; i++) {
      const ch = raw[i];
      buf += ch;
      if (boundaries.has(ch)) lastBoundaryPos = buf.length;

      if (buf.length >= limit) {
        const cutPos = (lastBoundaryPos >= minLen) ? lastBoundaryPos : limit;
        const seg = buf.slice(0, cutPos).trim();
        if (seg) parts.push(seg);
        buf = buf.slice(cutPos).trimStart();
        lastBoundaryPos = -1;
        for (let j = buf.length - 1; j >= 0; j--) {
          if (boundaries.has(buf[j])) { lastBoundaryPos = j + 1; break; }
        }
        continue;
      }

      if (lastBoundaryPos === buf.length && buf.length >= minLen) {
        const seg = buf.trim();
        if (seg) parts.push(seg);
        buf = '';
        lastBoundaryPos = -1;
      }
    }
    const tail = buf.trim();
    if (tail) parts.push(tail);
    return parts.filter(Boolean);
  },

  _prefetchTtsAhead(taskId) {
    const tid = Number(taskId || 0);
    if (!tid || tid !== this._ttsActiveTaskId) return;
    if (!this._ttsQueue || !this._ttsQueue.length) return;
    if (!this.data.isSpeakerOn) return;
    if (this.data.isCalling && this.data.callStatus !== 'connected') return;

    const maxPrefetch = Math.max(0, Number(this._ttsPrefetchMax || 2));
    if (!maxPrefetch) return;

    if (!this._ttsPrefetchedByIdx) this._ttsPrefetchedByIdx = {};
    if (!this._ttsInflightByIdx) this._ttsInflightByIdx = {};
    const prefetched = this._ttsPrefetchedByIdx;
    const inflight = this._ttsInflightByIdx;

    const inflightCount = Object.keys(inflight).length;
    const prefetchedCount = Object.keys(prefetched).length;
    let budget = maxPrefetch - inflightCount - prefetchedCount;
    if (budget <= 0) return;

    for (let i = 0; i < this._ttsQueue.length && budget > 0; i++) {
      const seg = this._ttsQueue[i];
      if (!seg) continue;
      const idx = Number(seg.idx);
      if (!(idx >= 0)) continue;
      if (idx === this._ttsCurrentSegIdx) continue;
      if (prefetched[idx]) continue;
      if (inflight[idx]) continue;

      const key = `${tid.toString(36)}_${idx.toString(36)}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
      const entry = { promise: null, claimed: false };
      entry.promise = Promise.resolve()
        .then(() => tts.synthesize(seg.text, { key }))
        .then(({ audioPath }) => {
          if (tid !== this._ttsActiveTaskId) {
            try { this._unlinkUserDataFileSafe && this._unlinkUserDataFileSafe(audioPath); } catch (e) {}
            return '';
          }
          if (!audioPath) return '';
          if (!entry.claimed) prefetched[idx] = audioPath;
          return audioPath;
        })
        .catch((e) => {
          return '';
        })
        .finally(() => {
          try { delete inflight[idx]; } catch (e) {}
        });
      inflight[idx] = entry;

      budget -= 1;
    }
  },

  _playNextTTSFromQueue(taskId) {
    const tid = Number(taskId || 0);
    if (!tid || tid !== this._ttsActiveTaskId) return;
    if (this._ttsProcessing) return;
    if (this.data.isCalling && this.data.callStatus !== 'connected') {
      this._ttsQueue = [];
      this._finishTtsUi(tid);
      return;
    }
    if (!this.data.isSpeakerOn) {
      this._ttsQueue = [];
      this._finishTtsUi(tid);
      return;
    }
    if (!this._ttsQueue || !this._ttsQueue.length) {
      this._finishTtsUi(tid);
      return;
    }
    const seg = this._ttsQueue.shift();
    if (!seg) {
      this._finishTtsUi(tid);
      return;
    }
    const idx = Number(seg.idx);
    if (idx >= 0) this._ttsCurrentSegIdx = idx;

    if (String(this.data.ttsActiveId || '') && tid === this._ttsActiveTaskId) {
      this.setData({ ttsActivePhase: 'loading' });
    }
    this._ttsProcessing = true;

    if (!this._ttsPrefetchedByIdx) this._ttsPrefetchedByIdx = {};
    if (!this._ttsInflightByIdx) this._ttsInflightByIdx = {};
    const prefetched = this._ttsPrefetchedByIdx;
    const inflight = this._ttsInflightByIdx;

    const startPlayWithAudioPath = (audioPath) => {
      if (tid !== this._ttsActiveTaskId) {
        this._ttsProcessing = false;
        try { this._unlinkUserDataFileSafe && this._unlinkUserDataFileSafe(audioPath); } catch (e) {}
        return;
      }
      if (this.data.isCalling && this.data.callStatus !== 'connected') { this._ttsProcessing = false; return; }
      if (!this.data.isSpeakerOn) { this._ttsProcessing = false; return; }
      this._ttsPlayingTaskId = tid;
      this._ttsCurrentAudioPath = audioPath;
      this.ttsAudio.src = audioPath;
      try {
        this.ttsAudio.play();
      } catch (err) {
        this._ttsProcessing = false;
        this._ttsPlayingTaskId = 0;
        this._ttsCurrentAudioPath = '';
        try { this._unlinkUserDataFileSafe && this._unlinkUserDataFileSafe(audioPath); } catch (e) {}
        this._finishTtsUi(tid);
        throw err;
      }
      try { this._prefetchTtsAhead && this._prefetchTtsAhead(tid); } catch (e) {}
    };

    const existingPrefetchPath = (idx >= 0) ? prefetched[idx] : '';
    if (existingPrefetchPath) {
      try { delete prefetched[idx]; } catch (e) {}
      Promise.resolve()
        .then(() => startPlayWithAudioPath(existingPrefetchPath))
        .catch((e) => {
          if (tid !== this._ttsActiveTaskId) return;
          this._ttsProcessing = false;
          this._ttsQueue = [];
          this._finishTtsUi(tid);
          console.error('playTTS segment failed', e);
          try { this.showTtsPlayError('prefetch/play', e, { audioPath: this._ttsCurrentAudioPath || '' }); } catch (err) {}
        });
      try { this._prefetchTtsAhead && this._prefetchTtsAhead(tid); } catch (e) {}
      return;
    }

    const existingInflight = (idx >= 0) ? inflight[idx] : null;
    if (existingInflight) {
      try { existingInflight.claimed = true; } catch (e) {}
    }
    const promise = existingInflight ? Promise.resolve(existingInflight.promise).then((p) => p) : Promise.resolve().then(() => {
      const key = `${tid.toString(36)}_${(idx >= 0 ? idx : 0).toString(36)}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
      return tts.synthesize(seg.text, { key }).then((r) => (r && r.audioPath) ? r.audioPath : '');
    });

    Promise.resolve()
      .then(() => promise)
      .then((audioPath) => {
        if (!audioPath) {
          throw new Error('TTS_EMPTY_AUDIO');
        }
        startPlayWithAudioPath(audioPath);
      })
      .catch((e) => {
        if (tid !== this._ttsActiveTaskId) return;
        this._ttsProcessing = false;
        this._ttsQueue = [];
        this._finishTtsUi(tid);
        console.error('playTTS segment failed', e);
        try { this.showTtsPlayError('synthesize/play', e, { audioPath: this._ttsCurrentAudioPath || '' }); } catch (err) {}
      });
  },

  async playTTS(text, options) {
    if (this.data.isCalling && this.data.callStatus !== 'connected') return;
    if (!this.data.isSpeakerOn) return;
    try {
      if (this._voiceInputMode) {
        this._ignoreNextVoiceStop = true;
        this._voiceInputMode = false;
        try { this.recorderManager && this.recorderManager.stop(); } catch (e) {}
        try { this.setData({ isHoldingVoice: false }); } catch (e) {}
        if (this._voiceFallbackSpectrumTimer) {
          try { clearInterval(this._voiceFallbackSpectrumTimer); } catch (e) {}
          this._voiceFallbackSpectrumTimer = null;
        }
      }
      if (this._ttsReady) await this._ttsReady;
      const maxLen = this.data.isCalling ? 100 : 120;
      const segments = this.splitTextForTTS(text, maxLen);
      if (!segments.length) return;
      const tid = (this._ttsActiveTaskId || 0) + 1;
      this._ttsActiveTaskId = tid;
      this._ttsPlayingTaskId = 0;
      this._ttsCurrentAudioPath = '';
      this._ttsCurrentSegIdx = -1;
      this._ttsPrefetchMax = 2;
      this._ttsPrefetchedByIdx = {};
      this._ttsInflightByIdx = {};
      this._ttsQueue = segments.map((s, i) => ({ text: s, idx: i }));
      this._ttsProcessing = false;
      this.safeStopTtsAudio();
      const msgId = options && options.messageId ? String(options.messageId) : '';
      if (msgId) {
        this.setData({ ttsActiveId: msgId, ttsActivePhase: 'loading' });
      } else {
        this.setData({ ttsActiveId: '', ttsActivePhase: '' });
      }
      try { this._prefetchTtsAhead && this._prefetchTtsAhead(tid); } catch (e) {}
      this._playNextTTSFromQueue(tid);
    } catch (e) {
      console.error('playTTS failed', e);
      try { this.showTtsPlayError('playTTS', e, { audioPath: this._ttsCurrentAudioPath || '' }); } catch (err) {}
    }
  },

  findMessageTextById(id) {
    const key = String(id || '').trim();
    if (!key) return '';
    const list = Array.isArray(this.data.renderTimeline) ? this.data.renderTimeline : [];
    const hit = list.find((m) => m && String(m.id) === key);
    if (hit && typeof hit.text === 'string') return hit.text;
    const full = Array.isArray(this.data.timeline) ? this.data.timeline : [];
    const hit2 = full.find((m) => m && String(m.id) === key);
    if (hit2 && typeof hit2.text === 'string') return hit2.text;
    return '';
  },

  onBubbleCopy(e) {
    const id = e && e.currentTarget && e.currentTarget.dataset ? e.currentTarget.dataset.id : '';
    const text = String(this.findMessageTextById(id) || '').trim();
    if (!text) {
      wx.showToast({ title: '没有可复制的内容', icon: 'none' });
      return;
    }
    wx.setClipboardData({
      data: text,
      success: () => {
        wx.showToast({ title: '已复制', icon: 'none' });
      },
      fail: () => {
        wx.showToast({ title: '复制失败', icon: 'none' });
      }
    });
  },

  onBubbleSpeak(e) {
    const id = e && e.currentTarget && e.currentTarget.dataset ? e.currentTarget.dataset.id : '';
    if (String(this.data.ttsActiveId || '') === String(id || '') && String(this.data.ttsActivePhase || '')) {
      this.cancelTTS();
      return;
    }
    const text = String(this.findMessageTextById(id) || '').trim();
    if (!text) {
      wx.showToast({ title: '没有可朗读的内容', icon: 'none' });
      return;
    }
    this.playTTS(text, { messageId: id });
  },

  startRecorder() {
    try {
      if (this.data.isMicMuted) return; // 静音时不启动录音
      this.recorderManager.start({
        sampleRate: 16000,
        numberOfChannels: 1,
        format: 'pcm',
        frameSize: 4
      });
    } catch (e) {
      wx.showToast({ title: '麦克风启动失败', icon: 'none' });
    }
  },

  updateUserSpectrum(buffer) {
    try {
      const view = new DataView(buffer);
      const len = view.byteLength;
      let sum = 0;
      for (let i = 0; i < len; i += 2) {
        const s = view.getInt16(i, true);
        sum += Math.abs(s);
      }
      const amp = Math.min(sum / (len / 2), 32767);
      const target = this.genBars(amp, this.data.userSpectrum.length);
      // 仅更新目标值，由动画循环平滑逼近
      this.setData({ _userTargetBars: target });
    } catch (e) {
      // 退化为随机动画
      const target = this.randomBars(this.data.userSpectrum.length);
      this.setData({ _userTargetBars: target });
    }
  },

  simulateAISpeech() {
    this.setData({ isAISpeaking: true });
    this.aiInterval = setInterval(() => {
      const target = this.randomBars(this.data.aiSpectrum.length);
      const bars = this.smoothBars(this.data.aiSpectrum, target, 0.6);
      this.setData({ aiSpectrum: bars });
    }, 80);
    // 模拟持续时长
    setTimeout(() => this.stopAISpectrum(), 1800);
  },

  stopAISpectrum() {
    if (this.aiInterval) { clearInterval(this.aiInterval); this.aiInterval = null; }
    this.setData({ isAISpeaking: false, aiSpectrum: new Array(this.data.aiSpectrum.length).fill(10) });
  },

  genBars(amp, n = 24) {
    const level = Math.max(6, Math.min(amp / 600, 60));
    const arr = [];
    for (let i = 0; i < n; i++) {
      const variance = (Math.sin(Date.now() / 90 + i) + 1) * 0.5;
      arr.push(Math.round(level * (0.5 + variance)));
    }
    return arr;
  },

  smoothBars(prev, target, alpha = 0.6) {
    if (!Array.isArray(prev) || prev.length !== target.length) return target;
    const out = new Array(target.length);
    for (let i = 0; i < target.length; i++) {
      out[i] = Math.round((prev[i] || 10) * alpha + target[i] * (1 - alpha));
    }
    return out;
  },

  randomBars(n = 24) {
    const arr = [];
    for (let i = 0; i < n; i++) {
      arr.push(Math.round(8 + Math.random() * 60));
    }
    return arr;
  },

  // ===== 频谱动画循环（约60fps）=====
  startSpectrumAnimLoop() {
    if (this._spectrumAnimTimer) return;
    const fpsInterval = 33; // ~30fps
    this._spectrumAnimTimer = setInterval(() => {
      const current = this.data.userSpectrum;
      const target = this.data._userTargetBars || current;
      const next = new Array(current.length);
      const alpha = 0.18; // 平滑系数，越小越平滑
      for (let i = 0; i < current.length; i++) {
        next[i] = Math.round(current[i] * (1 - alpha) + target[i] * alpha);
      }
      this.setData({ userSpectrum: next });
    }, fpsInterval);
  },

  stopSpectrumAnimLoop() {
    if (this._spectrumAnimTimer) {
      clearInterval(this._spectrumAnimTimer);
      this._spectrumAnimTimer = null;
    }
  },

  tickCallTimer() {
    const t = (this.data.callDuration || 0) + 1;
    const m = Math.floor(t / 60), s = t % 60;
    const str = `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    this.setData({ callDuration: t, callDurationStr: str });
  },

  onInputChange(e) {
    this.setData({
      inputText: e.detail.value
    });
    this.scheduleMeasureBottomOverlays(32);
  },

  onTextareaLineChange() {
    this.scheduleMeasureBottomOverlays(16);
  },

  onToggleVoiceMode() {
    const next = !this.data.isVoiceMode;
    if (this._voiceInputMode) {
      try { this.recorderManager && this.recorderManager.stop(); } catch (e) {}
      this._voiceInputMode = false;
      this._clearVoiceRecordTimer();
    }
    this.setData({ isVoiceMode: next, isHoldingVoice: false, voiceRecordDuration: 0 });
    this.scheduleMeasureBottomOverlays(80);
  },

  onHoldVoiceStart() {
    if (!this.data.isVoiceMode) return;
    if (this.data.isLoading) return;
    if (this._voiceInputMode) return;
    if (this.data.isMicMuted) {
      wx.showToast({ title: '麦克风已关闭', icon: 'none' });
      return;
    }
    try { this.cancelTTS && this.cancelTTS(); } catch (e) {}
    this._voiceInputMode = true;
    this.setData({ isHoldingVoice: true, voiceRecordDuration: 0 });
    try { wx.vibrateShort({ type: 'light' }); } catch (e) {}
    this._voiceRecordTimer = setInterval(() => {
      this.setData({ voiceRecordDuration: (this.data.voiceRecordDuration || 0) + 1 });
    }, 1000);
    try {
      this._voiceInputStartedAt = Date.now();
      this.recorderManager.start({ duration: 10000, sampleRate: 16000, numberOfChannels: 1, format: 'pcm', frameSize: 4 });
    } catch (e) {
      this._voiceInputStartedAt = 0;
      this._voiceInputMode = false;
      this._clearVoiceRecordTimer();
      this.setData({ isHoldingVoice: false, voiceRecordDuration: 0 });
      wx.showToast({ title: '录音启动失败', icon: 'none' });
    }
  },

  onHoldVoiceEnd() {
    if (!this.data.isVoiceMode) return;
    if (!this._voiceInputMode) {
      this.setData({ isHoldingVoice: false });
      return;
    }
    this._clearVoiceRecordTimer();
    this.setData({ isHoldingVoice: false, voiceRecordDuration: 0 });
    try { this.recorderManager && this.recorderManager.stop(); } catch (e) {}
  },

  _clearVoiceRecordTimer() {
    if (this._voiceRecordTimer) {
      try { clearInterval(this._voiceRecordTimer); } catch (e) {}
      this._voiceRecordTimer = null;
    }
  },

  onHoldVoiceCancel() {
    this.onHoldVoiceEnd();
  },

  onSendMessage() {
    const content = this.data.inputText.trim();
    if (!content || this.data.isLoading) return;
    
    this.setData({
      inputText: ''
    });
    
    this.dispatchUserText(content);
  },

  onQuickReplyTap(e) {
    const text = e && e.currentTarget && e.currentTarget.dataset ? String(e.currentTarget.dataset.text || '').trim() : '';
    if (!text || this.data.isLoading) return;
    this.dispatchUserText(text);
  },

  onOpenCapabilitySettings(e) {
    const capKey = e && e.currentTarget && e.currentTarget.dataset ? String(e.currentTarget.dataset.capkey || '') : '';
    const capLabel = this.getCapabilityLabel(capKey);
    wx.showModal({
      title: '开启能力',
      content: capLabel ? `该能力当前未开启：${capLabel}。\n这是服务器侧能力开关，不等同于「消息通知」。\n开启后系统才能履约提醒与自动触发。` : '该能力当前未开启，请前往设置开启。',
      confirmText: '去设置',
      cancelText: '稍后',
      success: (res) => {
        if (res && res.confirm) {
          wx.navigateTo({ url: '/pages/profile/settings/settings' });
        }
      }
    });
  },

  onShowSnapshot(e) {
    const id = e && e.currentTarget && e.currentTarget.dataset ? String(e.currentTarget.dataset.id || '') : '';
    const item = (this.data.timeline || []).find(m => String(m.id) === id);
    const snap = item && item.snapshot ? item.snapshot : null;
    const intent = snap && snap.recognizedIntent ? snap.recognizedIntent : null;
    const plan = snap && snap.dailyPlan ? snap.dailyPlan : null;
    const cap = snap && snap.capability ? snap.capability : null;
    const lines = [];
    if (intent) {
      lines.push(`识别意图：${intent.intentType || ''}`);
      lines.push(`时间：${intent.scheduleTime || ''}`);
      lines.push(`事项：${intent.subject || ''}`);
      lines.push(`是否拦截：${intent.blocked ? '是' : '否'}`);
      lines.push(`需要能力：${intent.requiredCapability || ''} (${intent.requiredCapabilityState || ''})`);
    }
    if (plan) {
      lines.push(`今日计划：${plan.exists ? '存在' : '不存在'}${plan.exists ? (plan.passiveOnly ? '（仅分析）' : '（可提醒）') : ''}`);
    }
    if (cap && cap.states) {
      lines.push('能力状态：');
      Object.keys(cap.states).forEach((k) => {
        lines.push(`- ${k}: ${cap.states[k]}`);
      });
    }
    wx.showModal({
      title: '系统状态',
      content: lines.length ? lines.join('\n') : '暂无快照信息',
      showCancel: false,
      confirmText: '知道了'
    });
  },

  onShowExplanation(e) {
    const id = e && e.currentTarget && e.currentTarget.dataset ? String(e.currentTarget.dataset.id || '') : '';
    const item = (this.data.timeline || []).find(m => String(m.id) === id);
    const code = item ? String(item.explanationCode || '') : '';
    let args = item ? item.explanationArgs : null;
    if (typeof args === 'string') {
      try { args = JSON.parse(args); } catch (err) { args = null; }
    }
    const lines = [];
    if (item) {
      lines.push(`类型：${String(item.systemMessageType || '')}`);
      if (item.decisionKey) lines.push(`decisionKey：${String(item.decisionKey)}`);
      if (item.timeSlot) lines.push(`timeSlot：${String(item.timeSlot)}`);
      if (item.subject) lines.push(`subject：${String(item.subject)}`);
      if (item.statusPhase) lines.push(`statusPhase：${String(item.statusPhase)}`);
      if (item.statusText) lines.push(`statusText：${String(item.statusText)}`);
    }
    lines.push(code ? `explanationCode：${code}` : 'explanationCode：');
    lines.push(args ? `explanationArgs：${JSON.stringify(args)}` : 'explanationArgs：');
    const snap = item && item.snapshot ? item.snapshot : null;
    const intent = snap && snap.recognizedIntent ? snap.recognizedIntent : null;
    if (intent) {
      lines.push(`intentType：${intent.intentType || ''}`);
      lines.push(`scheduleTime：${intent.scheduleTime || ''}`);
      lines.push(`intentSubject：${intent.subject || ''}`);
      lines.push(`blocked：${intent.blocked ? 'true' : 'false'}`);
      lines.push(`requiredCapability：${intent.requiredCapability || ''}`);
      lines.push(`requiredCapabilityState：${intent.requiredCapabilityState || ''}`);
    }
    wx.showModal({ title: '系统元信息', content: lines.length ? lines.join('\n') : '暂无信息', showCancel: false, confirmText: '知道了' });
  },

  async onReminderComplete(e) {
    const decisionKey = e && e.currentTarget && e.currentTarget.dataset ? String(e.currentTarget.dataset.decisionkey || '') : '';
    if (!decisionKey) return;
    try {
      await request.post('/reminder/complete', { decisionKey }, { loading: true, showError: true });
      this.upsertSystemTimelineItem({
        decisionKey,
        statusPhase: 'COMPLETED',
        statusText: '已完成',
        canComplete: false,
        primaryActionLabel: ''
      });
      try { wx.showToast({ title: '已记录', icon: 'success' }); } catch (e2) {}
      this.refreshRuntimeState({ silent: true });
    } catch (err) {
      try { wx.showToast({ title: '提交失败', icon: 'none' }); } catch (e2) {}
    }
  },

  async onReminderSkipToday(e) {
    const decisionKey = e && e.currentTarget && e.currentTarget.dataset ? String(e.currentTarget.dataset.decisionkey || '') : '';
    if (!decisionKey) return;
    try {
      await request.post('/reminder/skip-today', { decisionKey }, { loading: true, showError: true });
      this.upsertSystemTimelineItem({
        decisionKey,
        statusPhase: 'SKIPPED',
        statusText: '今天已跳过',
        canComplete: false,
        primaryActionLabel: ''
      });
      try { wx.showToast({ title: '已跳过', icon: 'success' }); } catch (e2) {}
      this.refreshRuntimeState({ silent: true });
    } catch (err) {
      try { wx.showToast({ title: '提交失败', icon: 'none' }); } catch (e2) {}
    }
  },

  async onReminderDisable(e) {
    const decisionKey = e && e.currentTarget && e.currentTarget.dataset ? String(e.currentTarget.dataset.decisionkey || '') : '';
    if (!decisionKey) return;
    wx.showModal({
      title: '关闭提醒',
      content: '确认关闭这条后续每天提醒？',
      confirmText: '关闭',
      confirmColor: '#ef4444',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          await request.post('/reminder/schedule/disable', { decisionKey }, { loading: true, showError: true });
          this.upsertSystemTimelineItem({
            decisionKey,
            statusPhase: 'DISABLED',
            statusText: '已关闭后续提醒',
            canComplete: false,
            primaryActionLabel: ''
          });
          try { wx.showToast({ title: '已关闭', icon: 'success' }); } catch (e2) {}
          this.refreshRuntimeState({ silent: true });
        } catch (err) {
          try { wx.showToast({ title: '提交失败', icon: 'none' }); } catch (e2) {}
        }
      }
    });
  },

  onAskImageAnalysis(e) {
    if (this.data.isLoading) return;
    const id = e && e.currentTarget && e.currentTarget.dataset ? String(e.currentTarget.dataset.id || '') : '';
    const item = (this.data.timeline || []).find(m => String(m.id) === id);
    const foods = (() => {
      const payload = item && item.payload && typeof item.payload === 'object' ? item.payload : null;
      const rec = payload && Array.isArray(payload.recordFoods) ? payload.recordFoods : null;
      if (rec && rec.length) return rec;
      const a = item && Array.isArray(item.analysisItems) ? item.analysisItems : null;
      if (a && a.length) return a;
      const items = payload && Array.isArray(payload.items) ? payload.items : [];
      return items;
    })();

    const list = (Array.isArray(foods) ? foods : []).slice(0, 8).map((f) => {
      const name = String(f && f.name ? f.name : '').trim();
      const cal = f && f.calorie != null ? Number(f.calorie) : NaN;
      const calText = Number.isFinite(cal) ? `${Math.max(0, Math.round(cal))}kcal` : '';
      return name ? (calText ? `${name}(${calText})` : name) : '';
    }).filter(Boolean);

    const prompt = list.length
      ? `请基于我刚才拍照识别的这餐做一个简要营养分析与改进建议。\n识别结果：${list.join('、')}\n要求：先给结论（高/中/低负担），再给3条可执行建议（份量/搭配/下一餐补救）。`
      : '请基于我刚才拍照识别的这餐做一个简要营养分析与改进建议（当前没有拿到识别条目时，先按常见搭配给泛化建议）。';

    this.sendTextMessage(prompt);
  },

  getCapabilityLabel(key) {
    const k = String(key || '').trim();
    if (k === 'MEDICATION_REMINDER') return '用药提醒';
    if (k === 'SUPPLEMENT_REMINDER') return '补剂/保健品提醒';
    if (k === 'PROACTIVE_REMINDER') return '系统主动提醒';
    if (k === 'PROACTIVE_SUMMARY') return '日终总结';
    if (k === 'EXERCISE_SUGGESTION') return '运动建议';
    return '';
  },

  getReminderPrimaryActionLabel(decisionKey, subject) {
    const dk = String(decisionKey || '').trim();
    const sub = String(subject || '').trim();
    if (dk.indexOf('medication_') >= 0) return '已用药';
    if (dk.indexOf('supplement_') >= 0) return '已服用';
    if (sub.indexOf('药') >= 0) return '已用药';
    if (sub.indexOf('鱼油') >= 0 || sub.indexOf('维生素') >= 0 || sub.indexOf('补剂') >= 0) return '已服用';
    return '已完成';
  },

  isReminderRequestSnapshot(snap) {
    const intent = snap && snap.recognizedIntent ? snap.recognizedIntent : null;
    const t = String(intent && intent.intentType ? intent.intentType : '').trim();
    return !!t && t.indexOf('REMINDER_REQUEST') >= 0;
  },

  computeReminderDecisionKey(snap) {
    const intent = snap && snap.recognizedIntent ? snap.recognizedIntent : null;
    const userId = snap && snap.userId != null ? String(snap.userId) : this.getNumericUserId();
    const scheduleTime = String(intent && intent.scheduleTime ? intent.scheduleTime : '').trim();
    const subject = String(intent && intent.subject ? intent.subject : '').trim();
    const required = String(intent && intent.requiredCapability ? intent.requiredCapability : '').trim();
    if (!userId || !scheduleTime) return '';
    const prefix = required === 'SUPPLEMENT_REMINDER' ? 'supplement_' : 'medication_';
    const itemKey = prefix + (subject || '事项');
    return `reminder:${userId}:${itemKey}:${scheduleTime}`;
  },

  buildSystemItemFromChatResponse(systemMessageType, snapshot, replyText) {
    const now = Date.now();
    const snap = snapshot || null;
    const intent = snap && snap.recognizedIntent ? snap.recognizedIntent : null;
    const userId = snap && snap.userId != null ? String(snap.userId) : this.getNumericUserId();
    const scheduleTime = String(intent && intent.scheduleTime ? intent.scheduleTime : '').trim();
    const subject = String(intent && intent.subject ? intent.subject : '').trim();
    const requiredCapability = String(intent && intent.requiredCapability ? intent.requiredCapability : '').trim();
    const requiredCapabilityState = String(intent && intent.requiredCapabilityState ? intent.requiredCapabilityState : '').trim();
    const blocked = !!(intent && intent.blocked);
    const timeText = this.getCurrentTime();

    if (systemMessageType === SystemMessageType.CAPABILITY_GUIDE) {
      const decisionKey = `capability:${userId}:${requiredCapability || 'UNKNOWN'}`;
      return {
        id: `sys_${decisionKey}`,
        kind: 'system',
        source: 'chat',
        role: 'system',
        systemMessageType,
        decisionKey,
        timeSlot: scheduleTime,
        subject: subject || this.getCapabilityLabel(requiredCapability) || '',
        statusPhase: 'BLOCKED',
        statusText: requiredCapabilityState === 'ENABLED' ? '系统能力可用' : '能力未开启，系统未执行该操作',
        text: String(replyText || '').trim(),
        ts: now,
        timeText,
        snapshot: snap
      };
    }

    if (systemMessageType === SystemMessageType.PLAN_FEEDBACK && this.isReminderRequestSnapshot(snap) && !blocked) {
      const decisionKey = this.computeReminderDecisionKey(snap);
      const dk = decisionKey || `reminder:${userId}:${now}`;
      return {
        id: `sys_${dk}`,
        kind: 'system',
        source: 'chat',
        role: 'system',
        systemMessageType: SystemMessageType.REMINDER,
        decisionKey: dk,
        timeSlot: scheduleTime,
        subject,
        statusPhase: 'QUEUED',
        statusText: '已加入系统提醒队列，将自动触发',
        text: '',
        ts: now,
        timeText,
        snapshot: snap
      };
    }

    if (systemMessageType === SystemMessageType.PLAN_FEEDBACK) {
      const decisionKey = `plan:${userId}:${scheduleTime}:${subject}`;
      return {
        id: `sys_${decisionKey}`,
        kind: 'system',
        source: 'chat',
        role: 'system',
        systemMessageType,
        decisionKey,
        timeSlot: scheduleTime,
        subject,
        statusPhase: 'RECORDED',
        statusText: '已写入系统记录',
        text: '',
        ts: now,
        timeText,
        snapshot: snap
      };
    }

    if (systemMessageType === SystemMessageType.SUMMARY) {
      const day = (() => {
        const d = new Date();
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, '0');
        const dd = String(d.getDate()).padStart(2, '0');
        return `${y}-${m}-${dd}`;
      })();
      const decisionKey = `summary:${userId}:${day}:${now}`;
      return {
        id: `sys_${decisionKey}`,
        kind: 'system',
        source: 'chat',
        role: 'system',
        systemMessageType,
        decisionKey,
        timeSlot: scheduleTime,
        subject,
        statusPhase: 'GENERATED',
        statusText: '系统汇总已生成',
        text: String(replyText || '').trim(),
        ts: now,
        timeText,
        snapshot: snap
      };
    }

    const decisionKey = `system:${systemMessageType}:${userId}:${now}`;
    return {
      id: `sys_${decisionKey}`,
      kind: 'system',
      source: 'chat',
      role: 'system',
      systemMessageType,
      decisionKey,
      timeSlot: scheduleTime,
      subject,
      statusPhase: 'DELIVERED',
      statusText: '系统消息已确认',
      text: String(replyText || '').trim(),
      ts: now,
      timeText,
      snapshot: snap
    };
  },

  applyChatMeta(meta) {
    const m = (meta && typeof meta === 'object') ? meta : null;
    if (!m) return;
    const hideQuickReplies = !!m.hideQuickReplies;
    if (hideQuickReplies) {
      try { this.setData({ quickReplies: [], quickReplyAnchorId: '' }); } catch (e) {}
    }
    const items = Array.isArray(m.systemItems) ? m.systemItems : [];
    if (!items.length) return;
    const now = Date.now();
    for (const raw of items) {
      const it = (raw && typeof raw === 'object') ? raw : null;
      if (!it) continue;
      const t = this.normalizeSystemMessageType(it.systemMessageType);
      if (!t) continue;
      const ts = Number(it.ts);
      const out = {
        ...it,
        kind: it.kind || 'system',
        source: it.source || 'chat',
        role: it.role || 'system',
        systemMessageType: t,
        ts: Number.isFinite(ts) ? ts : now,
        timeText: String(it.timeText || '').trim() || this.getCurrentTime()
      };
      if (!out.id) {
        const dk = String(out.decisionKey || '').trim();
        out.id = dk ? `sys_${dk}` : `sys_${t}_${String(out.ts)}`;
      }
      try { this.upsertSystemTimelineItem(out); } catch (e) {}
    }
  },

  inferPendingKind(text) {
    const raw = String(text || '').trim();
    if (!raw) return 'chat';
    if (/(日终|总结|汇总|复盘|小结|回顾)/.test(raw)) return 'summary';
    return 'chat';
  },

  sendTextMessage(text) {
    const raw = String(text || '').trim();
    if (!raw) return;
    this.setData({ userHasMessaged: true, showSmartSuggestions: false });
    if (this.data.quickReplies && this.data.quickReplies.length) {
      this.setData({ quickReplies: [], quickReplyAnchorId: '' });
    }
    const userId = this.getNumericUserId();
    const sessionId = this.data.sessionId || '';
    const now = Date.now();
    const u = this.data.userInfo || {};
    const userAvatar = u.avatarUrl || u.avatar || '/images/default_avatar.svg';
    const timeText = this.getCurrentTime();
    const pendingKind = this.inferPendingKind(raw);
    const userVM = {
      id: `local_chat_user_${now.toString(36)}_${Math.random().toString(36).slice(2, 7)}`,
      source: 'chat',
      role: 'user',
      systemMessageType: SystemMessageType.CHAT,
      text: raw,
      ts: now,
      sessionId,
      userId,
      timeText,
      avatar: userAvatar
    };
    const aiVM = {
      id: `local_chat_ai_${now.toString(36)}_${Math.random().toString(36).slice(2, 7)}`,
      source: 'chat',
      role: 'assistant',
      systemMessageType: SystemMessageType.CHAT,
      text: '',
      ts: now + 1,
      sessionId,
      userId,
      pending: true,
      pendingKind,
      timeText,
      avatar: AI_CHAT_AVATAR_URL
    };
    this.pushTimelineItem(userVM);
    this.pushTimelineItem(aiVM);
    this.processAIResponse(raw, aiVM.id, pendingKind);
  },

  async processAIResponse(userMessage, pendingVmId, pendingKind) {
    this.setData({ isLoading: true });
    try {
      const payload = {
        sessionId: this.data.sessionId,
        userId: (() => {
          const u = this.data.userInfo || {};
          const raw = (u.id || u.userId || '').toString().trim();
          return /^\d+$/.test(raw) ? raw : '';
        })(),
        message: userMessage
      };
      const kind = String(pendingKind || '').trim() || 'chat';
      let usedStream = false;
      let streamedText = '';
      let streamFinal = null;
      let streamedSnapshot = null;
      let streamedSuggestions = null;

      if (kind === 'chat' && typeof request.requestStream === 'function') {
        const startAt = Date.now();
        let lastPaintAt = 0;
        const paint = (force) => {
          const now = Date.now();
          if (!force && now - lastPaintAt < 60) return;
          lastPaintAt = now;
          try {
            const finalObj = (streamFinal && typeof streamFinal === 'object') ? streamFinal : {};
            const finalReply = String(finalObj.reply || finalObj.text || finalObj.message || '').trim();
            const textToShow = String(streamedText || finalReply || '');
            this.updateTimelineItemTransient(pendingVmId, { text: textToShow, pending: !textToShow, typing: false });
          } catch (e) {}
          if (now - startAt > 120) {
            try { this.scrollToBottomLater(0); } catch (e) {}
          }
        };

        try {
          streamFinal = await request.requestStream({
            url: '/chat/stream',
            method: 'POST',
            data: payload,
            timeout: 120000,
            loading: false,
            showError: false,
            onMessage: (msg) => {
              const m = (msg && typeof msg === 'object') ? msg : {};
              const type = String(m.type || '').trim();
              if (type === 'delta') {
                const d = String(m.text || '').toString();
                if (d) {
                  streamedText += d;
                  paint(false);
                }
                return;
              }
              if (type === 'snapshot') {
                const snap = m && m.snapshot ? m.snapshot : null;
                streamedSnapshot = snap || streamedSnapshot;
                if (snap) {
                  try { this.setData({ runtimeState: snap, runtimeStateUpdatedAt: Date.now() }); } catch (e) {}
                  try { wx.setStorageSync(RUNTIME_STATE_KEY, snap); } catch (e) {}
                  try { this.updateCapabilityGateState(snap); } catch (e) {}
                }
                return;
              }
              if (type === 'suggestions') {
                const suggestions = Array.isArray(m.suggestions)
                  ? m.suggestions.map(s => String(s || '').trim()).filter(Boolean).slice(0, 3)
                  : [];
                streamedSuggestions = suggestions;
                if (suggestions.length) {
                  try { this.setData({ quickReplies: suggestions, quickReplyAnchorId: pendingVmId }); } catch (e) {}
                } else {
                  try { this.setData({ quickReplies: [], quickReplyAnchorId: '' }); } catch (e) {}
                }
                return;
              }
              if (type === 'final') {
                streamFinal = m;
                return;
              }
            }
          });
          const finalObj = (streamFinal && typeof streamFinal === 'object') ? streamFinal : {};
          const finalReply = String(finalObj.reply || '').trim();
          if (!finalReply && !String(streamedText || '').trim() && !streamedSnapshot) {
            usedStream = false;
            streamFinal = null;
            streamedText = '';
          } else {
            usedStream = true;
          }
        } catch (e) {
          usedStream = false;
          streamFinal = null;
          streamedText = '';
        } finally {
          try { paint(true); } catch (e) {}
        }
      }

      const body = usedStream ? (streamFinal || {}) : await request.post('/chat', payload, { loading: false, showError: false, acceptAnyBody: true, timeout: 120000 });
      const resp = (body && typeof body === 'object') ? body : {};
      const systemMessageType = this.normalizeSystemMessageType(resp.systemMessageType);
      const snapshot = resp.snapshot || streamedSnapshot || null;
      const meta = (resp && typeof resp === 'object') ? resp.meta : null;
      const cleaned = String(resp.reply || resp.message || resp.text || '').trim() || String(streamedText || '').trim();
      if (!cleaned && systemMessageType === SystemMessageType.CHAT) throw new Error('AI_REPLY_UNAVAILABLE');

      if (snapshot) {
        this.setData({ runtimeState: snapshot, runtimeStateUpdatedAt: Date.now() });
        try { wx.setStorageSync(RUNTIME_STATE_KEY, snapshot); } catch (e) {}
        this.updateCapabilityGateState(snapshot);
      }
      if (meta) {
        try { this.applyChatMeta(meta); } catch (e) {}
      }

      if (systemMessageType === SystemMessageType.CHAT) {
        const suggestions = Array.isArray(resp.suggestions)
          ? resp.suggestions.map(s => String(s || '').trim()).filter(Boolean).slice(0, 3)
          : (usedStream ? streamedSuggestions : []);
        const fullText = String(cleaned || '');
        const len = fullText.length;
        const enableTypewriter = !usedStream && len > 0 && len <= 420;
        this.cancelTyping(pendingVmId);
        if (!enableTypewriter) {
          this.updateTimelineItem(pendingVmId, { systemMessageType, snapshot, text: fullText, pending: false, typing: false });
        } else {
          this.updateTimelineItem(pendingVmId, { systemMessageType, snapshot, text: '', pending: false, typing: true });
          const opts = len <= 200 ? { delayMs: 10, maxSteps: 40 } : { delayMs: 10, maxSteps: 60 };
          this.typewriterEffect(fullText, pendingVmId, opts)
            .then(() => {
              try { this.updateTimelineItem(pendingVmId, { text: fullText, typing: false }); } catch (e) {}
            })
            .catch(() => {
              try { this.updateTimelineItem(pendingVmId, { text: fullText, typing: false }); } catch (e) {}
            });
        }
        const onboardingActive = (() => {
          const s = this.getOnboardingState();
          return !!(s && s.active === true);
        })();
        if (!onboardingActive && suggestions) {
          if (suggestions.length) {
            this.setData({ quickReplies: suggestions, quickReplyAnchorId: pendingVmId });
          } else {
            this.setData({ quickReplies: [], quickReplyAnchorId: '' });
          }
        } else if (onboardingActive) {
          this.setData({ quickReplies: [], quickReplyAnchorId: '' });
        }
        if (this.data.isCalling && this.data.callStatus === 'connected') {
          this.setData({ callLastAiText: fullText });
          this.playTTS(fullText);
        }
      } else {
        if (systemMessageType === SystemMessageType.CAPABILITY_GUIDE) {
          const intent = snapshot && snapshot.recognizedIntent ? snapshot.recognizedIntent : null;
          const req = String(intent && intent.requiredCapability ? intent.requiredCapability : '').trim();
          this.setData({ capabilityBlocked: true, capabilityGuideKey: req }, () => {
            try { this.refreshRenderTimeline(); } catch (e) {}
          });
        }
        this.cancelTyping(pendingVmId);
        this.removeTimelineItem(pendingVmId);
        const sysItem = this.buildSystemItemFromChatResponse(systemMessageType, snapshot, cleaned);
        if (sysItem) {
          this.upsertSystemTimelineItem(sysItem);
        }
      }
      this.drainInboxStorageToTimeline();
      this.updateSuggestionState();
    } catch (error) {
      console.error('AI回复失败:', error);
      const fallback = '我这边暂时连不上对话服务。你先把目标、身高体重和今天吃了什么发我，我按这个先给你一版可执行建议。'
      try {
        this.cancelTyping(pendingVmId);
        this.updateTimelineItem(pendingVmId, { text: fallback, pending: false, error: true });
      } catch (e) {}
      if (this.data.isCalling && this.data.callStatus === 'connected') {
        try { this.setData({ callLastAiText: fallback }) } catch (e) {}
        try { this.playTTS(fallback) } catch (e) {}
        try { this._callAutoListenEnabled = false } catch (e) {}
      }
      const errMsg = (error && (error.errMsg || error.message)) ? String(error.errMsg || error.message) : '';
      const isTimeout = /timeout/i.test(errMsg);
      try { wx.showToast({ title: isTimeout ? '网络超时，稍后再试' : '对话服务暂不可用', icon: 'none' }); } catch (e) {}
    } finally {
      this.setData({ isLoading: false });
      this.scheduleSaveChatCache();
    }
  },

  parseTimestampMillis(tsStr) {
    const raw = String(tsStr || '').trim();
    if (!raw) return Date.now();
    const s = raw.includes('T') ? raw : raw.replace(' ', 'T');
    const ms = Date.parse(s);
    if (Number.isFinite(ms)) return ms;
    const m = raw.match(/^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2})(?::(\d{2}))?$/);
    if (m) {
      const y = Number(m[1]);
      const mo = Number(m[2]) - 1;
      const d = Number(m[3]);
      const h = Number(m[4]);
      const mi = Number(m[5]);
      const se = Number(m[6] || 0);
      const dt = new Date(y, mo, d, h, mi, se);
      const out = dt.getTime();
      if (Number.isFinite(out)) return out;
    }
    return Date.now();
  },

  cancelTyping(messageId) {
    const id = String(messageId || '').trim();
    if (!id) return;
    if (!this._typingTokens) this._typingTokens = {};
    this._typingTokens[id] = '';
  },

  async typewriterEffect(text, messageId, opts = {}) {
    const full = String(text || '');
    const id = String(messageId || '').trim();
    if (!id) return false;
    if (!full) return true;
    if (!this._typingTokens) this._typingTokens = {};

    const token = `${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 7)}`;
    this._typingTokens[id] = token;

    const maxSteps = Math.max(20, Number(opts && opts.maxSteps) || 60);
    const delayMs = Math.max(6, Number(opts && opts.delayMs) || 10);
    const stepChars = Math.max(1, Math.ceil(full.length / maxSteps));
    let i = 0;

    while (i < full.length) {
      if (this._typingTokens[id] !== token) return false;
      i = Math.min(full.length, i + stepChars);
      this.updateTimelineItemTransient(id, { text: full.slice(0, i) });
      await this.delay(delayMs);
    }
    return this._typingTokens[id] === token;
  },

  ensureSessionId() {
    const key = this.getSessionStorageKey();
    let sid = '';
    try { sid = wx.getStorageSync(key); } catch (e) { sid = ''; }
    const onboarding = this.isOnboardingMode();
    if (sid && onboarding && !String(sid).startsWith(ONBOARDING_SESSION_ID_PREFIX)) {
      sid = `${ONBOARDING_SESSION_ID_PREFIX}${sid}`;
      try { wx.setStorageSync(key, sid); } catch (e) {}
    }
    if (!sid) {
      const base = `${Date.now().toString(36)}_${Math.random().toString(36).slice(2,8)}`;
      sid = onboarding ? `${ONBOARDING_SESSION_ID_PREFIX}${base}` : base;
      try { wx.setStorageSync(key, sid); } catch (e) {}
    }
    this.data.sessionId = sid;
    this.setData({ sessionId: sid });
  },

  onImageUpload() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: async (res) => {
        const filePath = res && res.tempFiles && res.tempFiles[0] ? res.tempFiles[0].tempFilePath : '';
        if (!filePath) return;
        const startedAt = Date.now();
        const pendingKey = `image:${this.data.sessionId || ''}:${startedAt}`;
        this.upsertSystemTimelineItem({
          id: `sys_${pendingKey}`,
          kind: 'system',
          source: 'local',
          role: 'system',
          systemMessageType: SystemMessageType.IMAGE_ANALYSIS,
          decisionKey: pendingKey,
          statusPhase: 'PENDING',
          statusText: '识别中…',
          analysisItems: [],
          payload: { items: [] },
          recorded: false,
          brochureRecorded: false,
          brochureMealType: '',
          ts: startedAt,
          timeText: this.getCurrentTime()
        });
        try {
          const u = this.data.userInfo || {};
          const raw = (u.id || u.userId || '').toString().trim();
          const userId = /^\d+$/.test(raw) ? raw : '';
          const compressed = await compressOrPassthrough(filePath, { maxWidth: 1280, quality: 0.75 })
          const uploadPath = compressed.tempFilePath || filePath

          const body = await networkMonitor.guard(() =>
            uploadFile('/food/recognize', uploadPath, {
              loadingText: '识别中...',
              name: 'file',
              formData: {
                sessionId: this.data.sessionId || '',
                userId: userId || ''
              },
              acceptAnyBody: true
            })
          , '无网络，暂无法识别食物')
          if (!body) return
          const data = (body && typeof body === 'object' && body.data) ? body.data : body;
          const out = (data && data.data) ? data.data : data;
          const items = (out && out.items) ? out.items : (data && data.items) ? data.items : [];
          const normalizedItems = Array.isArray(items) ? items.map((it) => this.normalizeFoodCandidate(it)) : [];
          const bestItem = (() => {
            const list = normalizedItems
              .filter((f) => f && String(f.name || '').trim())
              .sort((a, b) => {
                const ap = (a && typeof a.probability === 'number' && Number.isFinite(a.probability)) ? a.probability : -1;
                const bp = (b && typeof b.probability === 'number' && Number.isFinite(b.probability)) ? b.probability : -1;
                return bp - ap;
              });
            return list.length ? list[0] : null;
          })();
          const displayItems = bestItem ? [bestItem] : [];
          const recordFoods = displayItems.map((f) => {
              const calorie = f && f.calorie != null ? Number(f.calorie) : 0;
              return {
                name: String(f.name || '').trim(),
                unit: '份',
                nutrient: '',
                calorie: Math.max(0, Math.round(Number.isFinite(calorie) ? calorie : 0)),
                carb: Number(f && f.carb != null ? f.carb : 0) || 0,
                protein: Number(f && f.protein != null ? f.protein : 0) || 0,
                fat: Number(f && f.fat != null ? f.fat : 0) || 0,
                img: '',
                note: 'AI识别'
              };
            });
          this.upsertSystemTimelineItem({
            id: `sys_${pendingKey}`,
            kind: 'system',
            source: 'local',
            role: 'system',
            systemMessageType: SystemMessageType.IMAGE_ANALYSIS,
            decisionKey: pendingKey,
            statusPhase: 'DONE',
            statusText: '影像识别完成',
            analysisItems: displayItems,
            recorded: false,
            brochureRecorded: false,
            brochureMealType: '',
            payload: { items: displayItems, recordFoods },
            ts: startedAt,
            timeText: this.getCurrentTime()
          });

          try {
            const memberId = await this.ensureNutritionMemberId();
            const mealType = this.inferMealTypeForBrochure();
            if (memberId && recordFoods.length) {
              const resp = await request.post('/recognition/meal/add-to-brochure', {
                memberId,
                date: this.getTodayDate(),
                mealType,
                foods: recordFoods
              }, { loading: false, showError: false });
              const ok = !!(resp && (resp.code === 0 || resp.success));
              if (ok) {
                this.upsertSystemTimelineItem({
                  id: `sys_${pendingKey}`,
                  decisionKey: pendingKey,
                  systemMessageType: SystemMessageType.IMAGE_ANALYSIS,
                  brochureRecorded: true,
                  brochureMealType: mealType
                });
                this.patchLocalNutritionBrochureCache({ mealType, date: this.getTodayDate(), foods: recordFoods });
              }
            }
          } catch (e) {}

          this.refreshRuntimeState({ silent: true });
        } catch (e) {
          this.upsertSystemTimelineItem({
            id: `sys_${pendingKey}`,
            kind: 'system',
            source: 'local',
            role: 'system',
            systemMessageType: SystemMessageType.IMAGE_ANALYSIS,
            decisionKey: pendingKey,
            statusPhase: 'ERROR',
            statusText: '识别失败',
            recorded: false,
            brochureRecorded: false,
            brochureMealType: '',
            ts: startedAt,
            timeText: this.getCurrentTime()
          });
          try { wx.showToast({ title: '识别失败', icon: 'none' }); } catch (err) {}
        }
      },
      fail: () => {}
    });
  },

  formatTime(ts) {
    try {
      const d = new Date(Number(ts || Date.now()));
      const yyyy = d.getFullYear();
      const mm = String(d.getMonth() + 1).padStart(2, '0');
      const dd = String(d.getDate()).padStart(2, '0');
      const hh = String(d.getHours()).padStart(2, '0');
      const mi = String(d.getMinutes()).padStart(2, '0');
      return `${yyyy}-${mm}-${dd} ${hh}:${mi}`;
    } catch (e) {
      return '';
    }
  },

  startCallVoiceInput() {
    if (this.data.callStatus !== 'connected') return false;
    if (this.data.isMicMuted) {
      wx.showToast({ title: '麦克风已关闭', icon: 'none' });
      return false;
    }
    if (this._voiceInputMode) return false;
    this._voiceInputMode = true;
    this._suppressAutoListenUntil = Date.now() + 1200;
    try { wx.vibrateShort({ type: 'light' }); } catch (e) {}
    this.safeStopTtsAudio();
    this._pauseCallVideo();
    if (this._voiceFallbackSpectrumTimer) {
      try { clearInterval(this._voiceFallbackSpectrumTimer); } catch (e) {}
      this._voiceFallbackSpectrumTimer = null;
    }
    this._voiceFallbackSpectrumTimer = setInterval(() => {
      try { this.setData({ _userTargetBars: this.randomBars(this.data.userSpectrum.length) }); } catch (e) {}
    }, 90);
    try {
      this._voiceInputStartedAt = Date.now();
      this.recorderManager.start({ duration: 10000, sampleRate: 16000, numberOfChannels: 1, format: 'pcm', frameSize: 4 });
      wx.showToast({ title: '请说话…', icon: 'none' });
      return true;
    } catch (e) {
      this._voiceInputStartedAt = 0;
      this._voiceInputMode = false;
      this._resumeCallVideo();
      if (this._voiceFallbackSpectrumTimer) {
        try { clearInterval(this._voiceFallbackSpectrumTimer); } catch (err) {}
        this._voiceFallbackSpectrumTimer = null;
      }
      wx.showToast({ title: '录音启动失败', icon: 'none' });
      return false;
    }
  },

  async handleVoiceInputStop(res) {
    try {
      const filePath = res && res.tempFilePath;
      if (!filePath) { wx.showToast({ title: '无录音文件', icon: 'none' }); return; }
      try {
        const fs = wx.getFileSystemManager();
        const stat = await new Promise((resolve, reject) => {
          fs.stat({ path: filePath, success: resolve, fail: reject });
        });
        const size = (stat && stat.stats && typeof stat.stats.size === 'number') ? stat.stats.size : 0;
        if (size < 2048) {
          wx.showToast({ title: '说话时间太短', icon: 'none' });
          this._callNoInputCount = (this._callNoInputCount || 0) + 1;
          return;
        }
      } catch (e) {}
      const app = getApp ? getApp() : null;
      const apiBase = this.data.apiBase || (app && app.getApiBase ? app.getApiBase() : '') || '';
      stt.setBaseUrl(apiBase);
      const result = await stt.recognize(filePath);
      const text = (result && result.text) ? result.text.trim() : '';
      if (!text) {
        wx.showToast({ title: '未识别到内容', icon: 'none' });
        this._callNoInputCount = (this._callNoInputCount || 0) + 1;
        return;
      }
      this._callNoInputCount = 0;
      this.setData({ callLastUserText: text });

      if (this.data.isVoiceOnboarding) {
        this._handleVoiceOnboardingInput(text);
        return;
      }

      this.dispatchUserText(text);
      if (typeof this.resetProactiveQuestionTimer === 'function') this.resetProactiveQuestionTimer();
    } catch (err) {
      const msg = (err && (err.message || err.errMsg)) ? String(err.message || err.errMsg) : '';
      wx.showToast({ title: msg ? `语音识别失败：${msg}` : '语音识别失败', icon: 'none' });
      console.warn('handleVoiceInputStop error:', err);
    } finally {
      try {
        this._resumeCallVideo();
        if ((this._callNoInputCount || 0) >= 2) {
          this._callAutoListenEnabled = false;
          this._suppressAutoListenUntil = Date.now() + 15000;
          this._callNoInputCount = 0;
          try { wx.showToast({ title: '没听清，点麦克风再说', icon: 'none' }); } catch (e) {}
          return;
        }
        if (this.data.isCalling && this.data.callStatus === 'connected') {
          this.maybeAutoStartCallVoiceInput && this.maybeAutoStartCallVoiceInput();
        }
      } catch (e) {}
    }
  },

  

  getCurrentTime() {
    const now = new Date();
    const hours = now.getHours().toString().padStart(2, '0');
    const minutes = now.getMinutes().toString().padStart(2, '0');
    return `${hours}:${minutes}`;
  },

  delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  },

  // 下拉刷新
  onRefresh() {
    this.setData({ refresherTriggered: true });
    this.refreshRuntimeState({ silent: true });
    this.syncChatHistoryFromServer({ silent: true });
    try {
      const app = getApp();
      app.fetchAiInboxOnce && app.fetchAiInboxOnce({ silent: false });
    } catch (e) {}
    setTimeout(() => {
      this.setData({ refresherTriggered: false });
      this.drainInboxStorageToTimeline();
    }, 1000);
  },

  onShareAppMessage(res) {
    try {
      if (res && res.from === 'button') {
        const id = res.target && res.target.dataset ? res.target.dataset.id : '';
        const text = String(this.findMessageTextById(id) || '').trim();
        const oneLine = text.replace(/\s+/g, ' ').trim();
        if (oneLine) {
          const title = oneLine.length > 30 ? `${oneLine.slice(0, 30)}…` : oneLine;
          return {
            title,
            path: '/pages/aiAgent/aiAgent',
            imageUrl: AI_CHAT_AVATAR_URL
          };
        }
      }
    } catch (e) {}
    return {
      title: 'AI营养助手 - 您的专属营养顾问',
      path: '/pages/aiAgent/aiAgent',
      imageUrl: AI_CHAT_AVATAR_URL
    };
  },

  // 生成默认工作流入口（示例）
  getDefaultWorkflow() {
    return [
      {
        title: '去商店补充维生素C',
        subtitle: '推荐 SKU：VC 30天',
        icon: '/static/tabBar/shop.png',
        url: '/pages/shop/shop?sku=vitamin_c'
      },
      {
        title: '查看减脂食谱',
        subtitle: '一周轻负担餐单',
        icon: '/images/icon_plan.svg',
        url: '/pages/nutritionbrochure/foodpage/foodpage?plan=lowfat'
      },
      {
        title: '记录今天饮食',
        subtitle: '快速添加一餐',
        icon: '/images/icon_analysis.svg',
        url: '/pages/nutritionbrochure/FoodRecord/FoodRecord?source=ai'
      }
    ];
  },

  // 导航到工作流入口目标页面
  navigateWorkflow(e) {
    const url = e.currentTarget.dataset.url;
    if (!url) return;
    try {
      wx.navigateTo({ url });
    } catch (err) {
      wx.showToast({ title: '无法打开目标页面', icon: 'none' });
      console.warn('navigateWorkflow failed:', err);
    }
  }
});

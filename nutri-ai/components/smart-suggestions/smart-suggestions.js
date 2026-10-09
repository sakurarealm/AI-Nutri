const MORNING_GREET_SUGGESTIONS = [
  '我最近的饮食不太规律…',
  '今天早餐应该吃什么？',
  '我的身体感觉不太舒服…',
  '我想咨询一下减肥计划',
  '帮我看看最近的营养摄入'
]

const MORNING_FOLLOWUP_SUGGESTIONS = [
  '昨晚睡得不太好',
  '早上没什么胃口',
  '今天想开始控制饮食',
  '有没有简单的早餐推荐',
  '帮我计划今天的三餐'
]

const NOON_GREET_SUGGESTIONS = [
  '中午不知道吃什么…',
  '下午容易犯困怎么办',
  '帮我看看上午吃了什么',
  '午餐后适合做什么运动',
  '今天的水喝够了吗'
]

const NOON_FOLLOWUP_SUGGESTIONS = [
  '下午想喝杯奶茶可以吗',
  '午餐吃太饱了怎么办',
  '下午有什么健康的零食推荐',
  '今天热量还差多少',
  '推荐一个下午加餐吧'
]

const EVENING_GREET_SUGGESTIONS = [
  '晚餐应该怎么吃比较健康',
  '今天运动量不够怎么办',
  '晚上饿了能吃什么',
  '帮我总结今天的饮食',
  '明天的三餐有什么建议'
]

const EVENING_FOLLOWUP_SUGGESTIONS = [
  '睡前喝牛奶有用吗',
  '明天早餐可以提前准备什么',
  '晚上总想吃宵夜怎么办',
  '帮我规划明天的运动',
  '最近体重没什么变化'
]

const BODY_CONCERN_SUGGESTIONS = [
  '最近肠胃不太舒服…',
  '最近腰有点酸…',
  '血糖最近不太稳定…',
  '皮肤最近不太好…',
  '最近总是感觉很累…'
]

const DIET_ASK_SUGGESTIONS = [
  '中午吃了外卖，帮我分析一下',
  '我不知道这餐该吃什么…',
  '帮我看看这餐营养够不够',
  '这个食物适合我吗',
  '有没有更健康的替代选择'
]

const MOOD_SUGGESTIONS = [
  '最近压力有点大…',
  '心情不太好，想吃点甜的',
  '工作太忙没时间好好吃饭',
  '感觉自己坚持不下去了',
  '有没有缓解焦虑的饮食建议'
]

function hourSlot() {
  const h = new Date().getHours()
  if (h < 10) return 'morning'
  if (h < 14) return 'noon'
  if (h < 19) return 'evening'
  return 'night'
}

function pickRandom(arr, count) {
  const pool = Array.isArray(arr) ? [...arr] : []
  const n = Math.min(count, pool.length)
  const result = []
  for (let i = 0; i < n; i++) {
    const idx = Math.floor(Math.random() * pool.length)
    result.push(pool.splice(idx, 1)[0])
  }
  return result
}

function buildGreetSuggestions() {
  const slot = hourSlot()
  if (slot === 'morning') return pickRandom(MORNING_GREET_SUGGESTIONS, 5)
  if (slot === 'noon') return pickRandom(NOON_GREET_SUGGESTIONS, 5)
  return pickRandom(EVENING_GREET_SUGGESTIONS, 5)
}

function buildFollowupSuggestions(lastAiText) {
  const slot = hourSlot()
  const text = String(lastAiText || '').toLowerCase()

  if (text.includes('身体') || text.includes('不舒服') || text.includes('感觉')) {
    return pickRandom(BODY_CONCERN_SUGGESTIONS, 5)
  }
  if (text.includes('吃') || text.includes('餐') || text.includes('食') || text.includes('外卖')) {
    return pickRandom(DIET_ASK_SUGGESTIONS, 5)
  }
  if (text.includes('心情') || text.includes('压力') || text.includes('焦虑') || text.includes('累')) {
    return pickRandom(MOOD_SUGGESTIONS, 5)
  }

  if (slot === 'morning') return pickRandom(MORNING_FOLLOWUP_SUGGESTIONS, 5)
  if (slot === 'noon') return pickRandom(NOON_FOLLOWUP_SUGGESTIONS, 5)
  return pickRandom(EVENING_FOLLOWUP_SUGGESTIONS, 5)
}

Component({
  properties: {
    visible: {
      type: Boolean,
      value: false
    },
    scenario: {
      type: String,
      value: 'greet'
    },
    lastAiText: {
      type: String,
      value: ''
    },
    userHasMessaged: {
      type: Boolean,
      value: false
    }
  },

  data: {
    suggestions: [],
    expanded: false,
    animating: false,
    sending: false
  },

  observers: {
    'visible, scenario, lastAiText'(visible, scenario, lastAiText) {
      if (!visible) {
        this.setData({ expanded: false, suggestions: [] })
        return
      }
      this.generateSuggestions(scenario, lastAiText)
    }
  },

  methods: {
    generateSuggestions(scenario, lastAiText) {
      let list = []
      if (scenario === 'greet') {
        list = buildGreetSuggestions()
      } else if (scenario === 'followup') {
        list = buildFollowupSuggestions(lastAiText)
      } else {
        list = buildGreetSuggestions()
      }
      this.setData({ suggestions: list.slice(0, 5) })
    },

    onOrbTap() {
      if (this.data.animating) return
      const next = !this.data.expanded
      this.setData({ animating: true, expanded: next })
      setTimeout(() => this.setData({ animating: false }), 360)
    },

    onSuggestionTap(e) {
      const text = e.currentTarget.dataset.text
      if (!text || this.data.sending) return
      this.setData({ expanded: false, sending: true })
      this.triggerEvent('select', { text })
      setTimeout(() => this.setData({ sending: false }), 1500)
    },

    onDismiss() {
      this.setData({ expanded: false })
      this.triggerEvent('dismiss')
    }
  }
})

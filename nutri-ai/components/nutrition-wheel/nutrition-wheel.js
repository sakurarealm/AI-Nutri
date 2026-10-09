Component({
  properties: {
    carbs: {
      type: Number,
      value: 0
    },
    protein: {
      type: Number,
      value: 0
    },
    fat: {
      type: Number,
      value: 0
    },
    fiber: {
      type: Number,
      value: 0
    },
    totalCalories: {
      type: Number,
      value: 0
    },
    carbTarget: {
      type: Number,
      value: 264
    },
    proteinTarget: {
      type: Number,
      value: 100
    },
    fatTarget: {
      type: Number,
      value: 67
    },
    fiberTarget: {
      type: Number,
      value: 25
    },
    size: {
      type: Number,
      value: 340
    }
  },

  data: {
    canvasWidth: 340,
    canvasHeight: 340
  },

  lifetimes: {
    attached() {
      const size = this.properties.size || 340
      this.setData({ canvasWidth: size, canvasHeight: size })
    },
    ready() {
      this.drawWheel()
    }
  },

  observers: {
    'carbs, protein, fat, fiber, totalCalories, carbTarget, proteinTarget, fatTarget, fiberTarget'() {
      this.drawWheel()
    }
  },

  methods: {
    drawWheel() {
      const query = this.createSelectorQuery()
      query.select('#wheelCanvas')
        .fields({ node: true, size: true })
        .exec((res) => {
          const canvas = res[0] && res[0].node
          if (!canvas) {
            this._fallbackDraw()
            return
          }
          this._drawOnCanvas(canvas)
        })
    },

    _drawOnCanvas(canvas) {
      const sys = wx.getSystemInfoSync()
      const dpr = sys.pixelRatio || 2
      const screenWidth = sys.windowWidth || 375
      const rpxSize = this.properties.size || 340
      const pxSize = rpxSize * screenWidth / 750
      canvas.width = pxSize * dpr
      canvas.height = pxSize * dpr
      const ctx = canvas.getContext('2d')
      ctx.scale(dpr, dpr)

      const cx = pxSize / 2
      const cy = pxSize / 2
      const outerR = pxSize / 2 - 16
      const innerR = outerR - 42

      ctx.clearRect(0, 0, pxSize, pxSize)

      const carbs = Math.max(0, this.properties.carbs || 0)
      const protein = Math.max(0, this.properties.protein || 0)
      const fat = Math.max(0, this.properties.fat || 0)
      const fiber = Math.max(0, this.properties.fiber || 0)
      const total = carbs + protein + fat + fiber

      if (total === 0) {
        this._drawEmptyWheel(ctx, cx, cy, outerR, innerR)
        this._drawCenterText(ctx, cx, cy, this.properties.totalCalories || 0)
        return
      }

      const segments = [
        { label: '碳水', value: carbs, color: '#f59e0b', target: this.properties.carbTarget || 264 },
        { label: '蛋白', value: protein, color: '#ef4444', target: this.properties.proteinTarget || 100 },
        { label: '脂肪', value: fat, color: '#8b5cf6', target: this.properties.fatTarget || 67 },
        { label: '纤维', value: fiber, color: '#22c55e', target: this.properties.fiberTarget || 25 }
      ].filter(s => s.value > 0)

      if (segments.length === 1) {
        segments[0].startAngle = -Math.PI / 2
        segments[0].endAngle = Math.PI * 1.5
      } else {
        let startAngle = -Math.PI / 2
        segments.forEach((seg) => {
          seg.startAngle = startAngle
          seg.endAngle = startAngle + (seg.value / total) * Math.PI * 2
          startAngle = seg.endAngle
        })
      }

      segments.forEach((seg) => {
        ctx.beginPath()
        ctx.arc(cx, cy, outerR, seg.startAngle, seg.endAngle)
        ctx.arc(cx, cy, innerR, seg.endAngle, seg.startAngle, true)
        ctx.closePath()

        const grad = ctx.createLinearGradient(
          cx + Math.cos(seg.startAngle) * innerR,
          cy + Math.sin(seg.startAngle) * innerR,
          cx + Math.cos(seg.endAngle) * outerR,
          cy + Math.sin(seg.endAngle) * outerR
        )
        grad.addColorStop(0, seg.color + 'cc')
        grad.addColorStop(1, seg.color)
        ctx.fillStyle = grad
        ctx.fill()

        ctx.strokeStyle = 'rgba(255,255,255,0.5)'
        ctx.lineWidth = 2
        ctx.stroke()

        const midAngle = (seg.startAngle + seg.endAngle) / 2
        const labelR = (outerR + innerR) / 2
        const lx = cx + Math.cos(midAngle) * labelR
        const ly = cy + Math.sin(midAngle) * labelR

        ctx.fillStyle = '#ffffff'
        ctx.font = 'bold 22px -apple-system, sans-serif'
        ctx.textAlign = 'center'
        ctx.textBaseline = 'middle'
        ctx.shadowColor = 'rgba(0,0,0,0.3)'
        ctx.shadowBlur = 3
        ctx.fillText(seg.label, lx, ly - 8)
        ctx.font = '18px -apple-system, sans-serif'
        ctx.fillText(Math.round(seg.value) + 'g', lx, ly + 16)
        ctx.shadowBlur = 0
      })

      this._drawCenterRing(ctx, cx, cy, innerR)
      this._drawCenterText(ctx, cx, cy, this.properties.totalCalories || 0)
      this._drawLegend(ctx, pxSize, segments)
    },

    _drawEmptyWheel(ctx, cx, cy, outerR, innerR) {
      ctx.beginPath()
      ctx.arc(cx, cy, outerR, 0, Math.PI * 2)
      ctx.arc(cx, cy, innerR, Math.PI * 2, 0, true)
      ctx.closePath()
      ctx.fillStyle = 'rgba(148, 163, 184, 0.15)'
      ctx.fill()
    },

    _drawCenterRing(ctx, cx, cy, innerR) {
      ctx.beginPath()
      ctx.arc(cx, cy, innerR + 3, 0, Math.PI * 2)
      ctx.strokeStyle = 'rgba(255,255,255,0.6)'
      ctx.lineWidth = 2.5
      ctx.stroke()
    },

    _drawCenterText(ctx, cx, cy, calories) {
      ctx.fillStyle = '#0f172a'
      ctx.font = 'bold 36px -apple-system, sans-serif'
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillText(Math.round(calories || 0), cx, cy - 10)

      ctx.fillStyle = '#64748b'
      ctx.font = '20px -apple-system, sans-serif'
      ctx.fillText('千卡', cx, cy + 24)
    },

    _drawLegend(ctx, size, segments) {
      const y = size - 8
      const itemW = size / segments.length
      segments.forEach((seg, i) => {
        const x = itemW * i + itemW / 2
        ctx.fillStyle = seg.color
        ctx.beginPath()
        ctx.arc(x - 26, y, 8, 0, Math.PI * 2)
        ctx.fill()

        ctx.fillStyle = '#475569'
        ctx.font = '18px -apple-system, sans-serif'
        ctx.textAlign = 'left'
        ctx.textBaseline = 'middle'
        const pct = seg.target > 0 ? Math.round((seg.value / seg.target) * 100) : 0
        ctx.fillText(seg.label + ' ' + pct + '%', x - 12, y)
      })
    },

    _fallbackDraw() {
      setTimeout(() => this.drawWheel(), 200)
    }
  }
})

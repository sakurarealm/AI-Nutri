Component({
  properties: {
    // 按钮类型：camera, voice, send
    type: {
      type: String,
      value: 'camera'
    },
    // 图标路径
    icon: {
      type: String,
      value: ''
    },
    // 是否禁用
    disabled: {
      type: Boolean,
      value: false
    },
    // 自定义样式类
    customClass: {
      type: String,
      value: ''
    }
  },
  data: {
    // 数据
  },
  methods: {
    // 点击事件
    onTap: function() {
      if (this.properties.disabled) return;
      
      // 触发父组件的点击事件
      this.triggerEvent('click', {
        type: this.properties.type
      });
    }
  },
  ready: function() {
    // 组件就绪
  }
})

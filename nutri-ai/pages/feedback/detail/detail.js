function safeSubject(s) {
  const raw = String(s || '').trim();
  if (!raw) return '其他问题';
  try {
    return decodeURIComponent(raw);
  } catch (e) {
    return raw;
  }
}

Page({
  data: {
    subject: '其他问题',
    rating: 5,
    stars: [1, 2, 3, 4, 5],
    content: '',
    orderIndex: 0,
    orderOptions: [{ id: '', label: '不选择订单' }],
    canSubmit: false
  },

  onLoad(query) {
    const subject = safeSubject(query && (query.subject || query.label));
    const orderOptions = this.buildOrderOptions();
    this.setData({ subject, orderOptions });
  },

  buildOrderOptions() {
    const base = [{ id: '', label: '不选择订单' }];
    const mock = [
      { id: 'AN202601120001', label: 'AN202601120001（未付款）' },
      { id: 'AN202601110045', label: 'AN202601110045（待发货）' },
      { id: 'AN202601100118', label: 'AN202601100118（已发货）' },
      { id: 'AN202601080032', label: 'AN202601080032（已送达）' }
    ];
    return base.concat(mock);
  },

  onStarTap(e) {
    const v = Number(e.currentTarget.dataset.value);
    if (!isNaN(v) && v >= 1 && v <= 5) {
      this.setData({ rating: v });
    }
  },

  onContentInput(e) {
    const content = String((e && e.detail && e.detail.value) || '');
    const canSubmit = content.trim().length > 0;
    this.setData({ content, canSubmit });
  },

  onOrderChange(e) {
    const idx = Number(e.detail.value);
    if (!isNaN(idx)) this.setData({ orderIndex: idx });
  },

  onSubmit() {
    if (!this.data.canSubmit) return;
    const request = require('../../../utils/request.js');
    const orderOptions = Array.isArray(this.data.orderOptions) ? this.data.orderOptions : [];
    const idx = Number(this.data.orderIndex);
    const orderNo = orderOptions[idx] && orderOptions[idx].id ? String(orderOptions[idx].id) : '';

    request
      .post('/feedback/submit', {
        subject: this.data.subject,
        rating: this.data.rating,
        content: this.data.content,
        orderNo
      }, { showError: false, loadingText: '提交中...' })
      .then((resp) => {
        const id = resp && resp.data && (resp.data.id || resp.data.feedbackId) ? String(resp.data.id || resp.data.feedbackId) : '';
        wx.showToast({ title: id ? `反馈已提交 #${id}` : '反馈已提交', icon: 'success' });
        this.setData({ content: '', canSubmit: false, orderIndex: 0, rating: 5 });
        setTimeout(() => {
          try {
            wx.navigateBack({ delta: 1 });
          } catch (e) {}
        }, 800);
      })
      .catch((e) => {
        const msg = e && (e.message || e.errMsg) ? String(e.message || e.errMsg) : '';
        wx.showToast({ title: msg ? `提交失败：${msg}` : '提交失败', icon: 'none' });
      });
  }
});

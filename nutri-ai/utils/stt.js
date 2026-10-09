let baseUrl = ''
function setBaseUrl(url){ baseUrl = url }
function recognize(filePath) {
  if (!baseUrl) {
    try {
      const app = getApp ? getApp() : null
      let raw = (app && app.getApiBase ? app.getApiBase() : '') || wx.getStorageSync('API_BASE_URL') || ''
      raw = String(raw || '').trim().replace(/\/+$/, '')
      if (raw.endsWith('/api')) raw = raw.slice(0, -4)
      baseUrl = raw
    } catch (e) {
      baseUrl = ''
    }
  }
  if (!baseUrl) return Promise.reject(new Error('API host not configured'))
  return new Promise((resolve, reject) => {
    try {
      const app = getApp ? getApp() : null
      const token = (app && app.getAuthToken) ? app.getAuthToken() : (wx.getStorageSync('token') || wx.getStorageSync('AUTH_TOKEN'))
      const header = {}
      if (token) header['Authorization'] = `Bearer ${token}`
      wx.uploadFile({
        url: baseUrl + '/api/stt',
        filePath,
        name: 'file',
        formData: {},
        header,
        success: (res) => {
          try {
            const status = res.statusCode || 0
            if (status !== 200) { reject(new Error('HTTP_' + status)); return }
            let data = res.data
            if (typeof data === 'string') {
              try { data = JSON.parse(data) } catch (e) {}
            }
            if (data && typeof data === 'object' && typeof data.code === 'number' && data.code !== 0) {
              const msg = data.message ? String(data.message) : '语音识别失败'
              reject(new Error(msg))
              return
            }
            const inner = (data && typeof data === 'object' && data.data) ? data.data : data
            const text = (inner && (inner.text || inner.result || inner.transcript)) || ''
            resolve({ text })
          } catch (e) { reject(e) }
        },
        fail: reject
      })
    } catch (e) { reject(e) }
  })
}
module.exports = { recognize, setBaseUrl }

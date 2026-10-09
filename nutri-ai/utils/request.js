// utils/request.js
function getAppSafe() {
  try {
    return typeof getApp === 'function' ? getApp() : null
  } catch (e) {
    return null
  }
}

function getLogManagerSafe() {
  try {
    if (typeof wx !== 'undefined' && wx && typeof wx.getLogManager === 'function') {
      return wx.getLogManager({ level: 0 })
    }
  } catch (e) {}
  return null
}

function logNet(level, payload) {
  const logger = getLogManagerSafe()
  if (!logger) return
  const fn = typeof logger[level] === 'function' ? logger[level].bind(logger) : logger.log.bind(logger)
  try {
    fn('net', payload || {})
  } catch (e) {}
}

const config = {
  timeout: 10000,
  acceptHeader: {
    Accept: 'application/json'
  },
  jsonHeader: {
    'Content-Type': 'application/json',
    Accept: 'application/json'
  }
}

let _refreshingPromise = null

function normalizeApiRoot(url) {
  let u = String(url || '').trim()
  if (!u) return ''
  u = u.replace(/\/+$/, '')
  if (u.endsWith('/api')) return u
  return u + '/api'
}

function isAbsoluteUrl(url) {
  return /^https?:\/\//i.test(String(url || '').trim())
}

function getApiRoot() {
  try {
    const app = getAppSafe()
    const base = (app && app.getApiBase && app.getApiBase()) || wx.getStorageSync('API_BASE_URL') || ''
    return normalizeApiRoot(base)
  } catch (e) {
    return ''
  }
}

function getCloudContainerConfig() {
  try {
    const app = getAppSafe()
    const useCallContainer = wx.getStorageSync('USE_CALLCONTAINER')
      || (app && app.globalData && app.globalData.useCallContainer)
      || ''
    const enabled = String(useCallContainer).trim() === '1' || String(useCallContainer).trim() === 'true'
    if (!enabled) return null
    const env = wx.getStorageSync('CLOUDBASE_ENV_ID') || (app && app.globalData && app.globalData.cloudEnvId) || ''
    const service = wx.getStorageSync('CLOUDBASE_SERVICE') || (app && app.globalData && app.globalData.cloudService) || ''
    if (!env || !service) return null
    if (!wx.cloud || typeof wx.cloud.callContainer !== 'function') return null
    return { env: String(env), service: String(service) }
  } catch (e) {
    return null
  }
}

function getAuthToken() {
  try {
    const app = getAppSafe()
    if (app && app.getAuthToken) return app.getAuthToken() || ''
  } catch (e) {}
  try {
    return (wx.getStorageSync('token') || wx.getStorageSync('AUTH_TOKEN') || '').trim()
  } catch (e) {
    return ''
  }
}

function getRefreshToken() {
  try {
    const app = getAppSafe()
    if (app && app.getRefreshToken) return app.getRefreshToken() || ''
  } catch (e) {}
  try {
    return (wx.getStorageSync('REFRESH_TOKEN') || wx.getStorageSync('refreshToken') || '').trim()
  } catch (e) {
    return ''
  }
}

function setTokenPair({ accessToken, refreshToken } = {}) {
  const app = getAppSafe()
  if (accessToken) {
    if (app && app.setAuthToken) app.setAuthToken(accessToken)
    else {
      try { wx.setStorageSync('token', String(accessToken)) } catch (e) {}
      try { wx.setStorageSync('AUTH_TOKEN', String(accessToken)) } catch (e) {}
    }
  }
  if (refreshToken) {
    if (app && app.setRefreshToken) app.setRefreshToken(refreshToken)
    else {
      try { wx.setStorageSync('REFRESH_TOKEN', String(refreshToken)) } catch (e) {}
      try { wx.setStorageSync('refreshToken', String(refreshToken)) } catch (e) {}
    }
  }
}

function isUnauthorizedResponse(res) {
  const status = res && res.statusCode
  if (status === 401 || status === 403) return true
  const code = res && res.data && typeof res.data === 'object' ? res.data.code : undefined
  return status === 200 && (code === 1002 || code === 401)
}

function buildUrl(inputUrl) {
  const u = String(inputUrl || '')
  if (isAbsoluteUrl(u)) return u
  const root = getApiRoot()
  return root ? (root + u) : u
}

function buildContainerPath(inputUrl) {
  let u = String(inputUrl || '').trim()
  if (!u) return '/api'
  if (!u.startsWith('/')) u = '/' + u
  if (u.startsWith('/api/')) return u
  if (u === '/api') return u
  return '/api' + u
}

function wxRequestRaw(options) {
  const cloudCfg = !isAbsoluteUrl(options.url) ? getCloudContainerConfig() : null
  if (cloudCfg) {
    return new Promise((resolve, reject) => {
      wx.cloud.callContainer({
        config: { env: cloudCfg.env },
        path: buildContainerPath(options.url),
        header: { ...(options.header || {}), 'X-WX-SERVICE': cloudCfg.service },
        method: options.method || 'GET',
        data: options.data || {},
        success: resolve,
        fail: reject
      })
    })
  }
  return new Promise((resolve, reject) => {
    const finalUrl = buildUrl(options.url)
    if (!isAbsoluteUrl(finalUrl)) {
      reject(new Error('API host not configured'))
      return
    }
    wx.request({
      url: finalUrl,
      method: options.method || 'GET',
      data: options.data || {},
      header: options.header || {},
      timeout: options.timeout || config.timeout,
      responseType: options.responseType,
      success: resolve,
      fail: reject
    })
  })
}

function createUtf8Decoder() {
  if (typeof TextDecoder === 'function') {
    const dec = new TextDecoder('utf-8')
    return {
      decode: (ab) => {
        try { return dec.decode(ab, { stream: true }) } catch (e) { return '' }
      },
      flush: () => {
        try { return dec.decode() } catch (e) { return '' }
      }
    }
  }
  const decodeFallback = (ab) => {
    const bytes = ab instanceof ArrayBuffer ? new Uint8Array(ab) : (ab && ab.buffer ? new Uint8Array(ab.buffer) : new Uint8Array(0))
    let out = ''
    let i = 0
    while (i < bytes.length) {
      const c = bytes[i++]
      if (c < 0x80) {
        out += String.fromCharCode(c)
        continue
      }
      if (c >= 0xc0 && c < 0xe0) {
        const c2 = bytes[i++] & 0x3f
        out += String.fromCharCode(((c & 0x1f) << 6) | c2)
        continue
      }
      if (c >= 0xe0 && c < 0xf0) {
        const c2 = bytes[i++] & 0x3f
        const c3 = bytes[i++] & 0x3f
        out += String.fromCharCode(((c & 0x0f) << 12) | (c2 << 6) | c3)
        continue
      }
      if (c >= 0xf0) {
        const c2 = bytes[i++] & 0x3f
        const c3 = bytes[i++] & 0x3f
        const c4 = bytes[i++] & 0x3f
        let cp = ((c & 0x07) << 18) | (c2 << 12) | (c3 << 6) | c4
        cp -= 0x10000
        out += String.fromCharCode(0xd800 + (cp >> 10), 0xdc00 + (cp & 0x3ff))
      }
    }
    return out
  }
  return { decode: decodeFallback, flush: () => '' }
}

function requestStream(options = {}) {
  return new Promise((resolve, reject) => {
    const cloudCfg = !isAbsoluteUrl(options.url) ? getCloudContainerConfig() : null
    if (cloudCfg) {
      reject(new Error('STREAM_UNSUPPORTED'))
      return
    }
    const finalUrl = buildUrl(options.url)
    if (!isAbsoluteUrl(finalUrl)) {
      reject(new Error('API host not configured'))
      return
    }

    const header = withDefaultHeaders(options)
    if (!header.Accept && !header.accept) {
      header.Accept = 'application/x-ndjson, application/json;q=0.9, */*;q=0.8'
    } else {
      header.Accept = 'application/x-ndjson, application/json;q=0.9, */*;q=0.8'
    }
    const decoder = createUtf8Decoder()
    let buffer = ''
    let finalFrame = null
    let seenAny = false
    let done = false

    const task = wx.request({
      url: finalUrl,
      method: options.method || 'GET',
      data: options.data || {},
      header,
      timeout: options.timeout || config.timeout,
      enableChunked: true,
      responseType: 'arraybuffer',
      success: (res) => {
        if (done) return
        done = true
        const statusCode = res && typeof res.statusCode === 'number' ? res.statusCode : 0
        if (statusCode < 200 || statusCode >= 300) {
          reject(new Error(`HTTP_${statusCode || 'ERROR'}`))
          return
        }
        try {
          const tail = decoder.flush()
          if (tail) buffer += tail
        } catch (e) {}
        try {
          const raw = buffer.trim()
          if (raw) {
            const lines = raw.split('\n')
            for (const ln of lines) {
              const line = String(ln || '').trim()
              if (!line) continue
              let obj = null
              try { obj = JSON.parse(line) } catch (e) { obj = null }
              if (obj) {
                seenAny = true
                if (typeof options.onMessage === 'function') {
                  try { options.onMessage(obj) } catch (e) {}
                }
                if (obj && obj.type === 'final') finalFrame = obj
              }
            }
          }
        } catch (e) {}

        if (!finalFrame && !seenAny) {
          reject(new Error('EMPTY_STREAM'))
          return
        }
        resolve(finalFrame || null)
      },
      fail: (err) => {
        if (done) return
        done = true
        reject(err)
      }
    })

    if (!task || typeof task.onChunkReceived !== 'function') {
      try { task && task.abort && task.abort() } catch (e) {}
      reject(new Error('CHUNK_UNSUPPORTED'))
      return
    }

    if (typeof task.onHeadersReceived === 'function' && typeof options.onHeaders === 'function') {
      try {
        task.onHeadersReceived((r) => {
          try { options.onHeaders((r && (r.header || r.headers)) || {}) } catch (e) {}
        })
      } catch (e) {}
    }

    task.onChunkReceived((evt) => {
      const ab = evt && evt.data ? evt.data : null
      if (!ab) return
      const chunkText = decoder.decode(ab)
      if (!chunkText) return
      buffer += chunkText

      while (true) {
        const idx = buffer.indexOf('\n')
        if (idx < 0) break
        const line = buffer.slice(0, idx).trim()
        buffer = buffer.slice(idx + 1)
        if (!line) continue
        let obj = null
        try { obj = JSON.parse(line) } catch (e) { obj = null }
        if (!obj) continue
        seenAny = true
        if (obj && obj.type === 'error') {
          try { task.abort && task.abort() } catch (e) {}
          if (!done) {
            done = true
            reject(new Error(String(obj.message || 'stream error')))
          }
          return
        }
        if (typeof options.onMessage === 'function') {
          try { options.onMessage(obj) } catch (e) {}
        }
        if (obj && obj.type === 'final') {
          finalFrame = obj
        }
      }
    })
  })
}

async function refreshTokensOnce() {
  if (_refreshingPromise) return _refreshingPromise
  const rt = getRefreshToken()
  if (!rt) return null

  _refreshingPromise = wxRequestRaw({
    url: '/auth/token/refresh',
    method: 'POST',
    data: { refreshToken: rt },
    header: { ...config.jsonHeader },
    timeout: 15000
  })
    .then((res) => {
      if (!res || res.statusCode !== 200) return null
      const body = res.data || {}
      const ok = body && (body.code === 0 || body.code === 200 || body.success === true)
      if (!ok) return null
      const data = body.data || body
      const accessToken = data && (data.accessToken || data.token)
      const refreshToken = data && (data.refreshToken || data.refresh_token)
      if (!accessToken) return null
      setTokenPair({ accessToken, refreshToken })
      return { accessToken, refreshToken }
    })
    .catch(() => null)
    .finally(() => {
      _refreshingPromise = null
    })

  return _refreshingPromise
}

function withDefaultHeaders(options) {
  const method = String(options.method || 'GET').toUpperCase()
  const base = method === 'GET' || method === 'HEAD' ? config.acceptHeader : config.jsonHeader
  const baseHeader = { ...base, ...(options.header || {}) }
  const token = options.skipAuth ? '' : getAuthToken()
  if (token && !baseHeader.Authorization && !baseHeader.authorization) {
    baseHeader.Authorization = `Bearer ${token}`
  }
  return baseHeader
}

async function requestRaw(options = {}) {
  const header = withDefaultHeaders(options)
  const res = await wxRequestRaw({
    ...options,
    header
  })

  if (options.skipRefresh) return res
  if (options._retried) return res
  if (!isUnauthorizedResponse(res)) return res

  const refreshed = await refreshTokensOnce()
  if (!refreshed || !refreshed.accessToken) return res

  const retryHeader = withDefaultHeaders({ ...options, header: options.header })
  return wxRequestRaw({
    ...options,
    header: retryHeader,
    _retried: true
  })
}

function request(options = {}) {
  return new Promise(async (resolve, reject) => {
    if (options.loading !== false) {
      wx.showLoading({
        title: options.loadingText || '加载中...',
        mask: true
      })
    }

    try {
      const res = await requestRaw({
        timeout: config.timeout,
        ...options
      })

      if (options.loading !== false) wx.hideLoading()

      if (options.rawResponse) {
        resolve(res)
        return
      }

      const status = res.statusCode || 0
      if (status !== 200) {
        const errorMsg = `请求失败 ${status}`
        if (options.showError !== false) {
          wx.showToast({ title: errorMsg, icon: 'none' })
        }
        logNet('error', {
          at: Date.now(),
          type: 'http_status',
          statusCode: status,
          method: String(options.method || 'GET').toUpperCase(),
          url: buildUrl(options.url),
          errMsg: errorMsg
        })
        const err = new Error(errorMsg)
        err.statusCode = status
        err.response = res
        reject(err)
        return
      }

      const body = res.data
      const acceptAnyBody = options.acceptAnyBody === true
      if (acceptAnyBody) {
        resolve(body)
        return
      }

      if (body && (body.code === 0 || body.success)) {
        resolve(body)
        return
      }

      const errorMsg = (body && (body.message || body.msg)) || '请求失败'
      if (options.showError !== false) {
        wx.showToast({ title: errorMsg, icon: 'none' })
      }
      logNet('warn', {
        at: Date.now(),
        type: 'biz_error',
        statusCode: status,
        method: String(options.method || 'GET').toUpperCase(),
        url: buildUrl(options.url),
        code: body && typeof body === 'object' ? body.code : undefined,
        msg: errorMsg
      })
      const err = new Error(errorMsg)
      err.statusCode = status
      err.response = res
      err.data = body
      reject(err)
    } catch (err) {
      if (options.loading !== false) wx.hideLoading()
      const errorMsg = (err && err.errMsg) ? err.errMsg : ((err && err.message) ? err.message : '网络请求失败')
      if (options.showError !== false) {
        wx.showToast({ title: errorMsg, icon: 'none' })
      }
      logNet('error', {
        at: Date.now(),
        type: 'network_fail',
        method: String(options.method || 'GET').toUpperCase(),
        url: buildUrl(options.url),
        errMsg: errorMsg
      })
      reject(err)
    }
  })
}

/**
 * GET请求
 */
function get(url, data = {}, options = {}) {
  return request({
    url,
    method: 'GET',
    data,
    ...options
  })
}

/**
 * POST请求
 */
function post(url, data = {}, options = {}) {
  return request({
    url,
    method: 'POST',
    data,
    ...options
  })
}

/**
 * PUT请求
 */
function put(url, data = {}, options = {}) {
  return request({
    url,
    method: 'PUT',
    data,
    ...options
  })
}

/**
 * DELETE请求
 */
function del(url, data = {}, options = {}) {
  return request({
    url,
    method: 'DELETE',
    data,
    ...options
  })
}

function getApiHost() {
  const root = String(getApiRoot() || '').trim()
  if (!root) return ''
  if (root.endsWith('/api')) return root.slice(0, -4)
  return root.replace(/\/+$/, '')
}

function ping(options = {}) {
  const host = getApiHost()
  if (!host) return Promise.reject(new Error('API host not configured'))
  return request({
    url: host + '/ping',
    method: 'GET',
    loading: false,
    showError: false,
    acceptAnyBody: true,
    ...options
  })
}

/**
 * 上传文件
 */
function uploadFile(url, filePath, options = {}) {
  return new Promise(async (resolve, reject) => {
    wx.showLoading({
      title: options.loadingText || '上传中...',
      mask: true
    })

    const doUpload = (header) => new Promise((res2, rej2) => {
      wx.uploadFile({
        url: buildUrl(url),
        filePath,
        name: options.name || 'file',
        formData: options.formData || {},
        header,
        timeout: options.timeout || 120000,
        success: res2,
        fail: rej2
      })
    })

    const parseBody = (res) => {
      try {
        const raw = res && res.data
        if (typeof raw === 'string') return JSON.parse(raw)
        return raw || {}
      } catch (e) {
        return null
      }
    }

    try {
      const firstHeader = withDefaultHeaders({ header: options.header || {}, skipAuth: options.skipAuth })
      let res = await doUpload(firstHeader)

      const status = res.statusCode || 0
      const body = parseBody(res)
      const unauthorized = status === 401 || status === 403 || (body && typeof body === 'object' && (body.code === 1002 || body.code === 401))
      if (unauthorized && !options.skipRefresh) {
        const refreshed = await refreshTokensOnce()
        if (refreshed && refreshed.accessToken) {
          const retryHeader = withDefaultHeaders({ header: options.header || {}, skipAuth: options.skipAuth })
          res = await doUpload(retryHeader)
        }
      }

      wx.hideLoading()

      const status2 = res.statusCode || 0
      if (options.rawResponse) { resolve(res); return }
      if (status2 !== 200) {
        logNet('error', {
          at: Date.now(),
          type: 'upload_http_status',
          statusCode: status2,
          url: buildUrl(url),
          errMsg: `请求失败 ${status2}`
        })
        const err = new Error(`请求失败 ${status2}`)
        err.statusCode = status2
        err.response = res
        reject(err)
        return
      }

      const data = parseBody(res)
      if (!data) {
        reject(new Error('上传失败'))
        return
      }
      if (options.acceptAnyBody) {
        resolve(data)
        return
      }
      if (data.code === 0 || data.success) {
        resolve(data)
        return
      }
      const errorMsg = data.message || data.msg || '上传失败'
      wx.showToast({ title: errorMsg, icon: 'none' })
      logNet('warn', {
        at: Date.now(),
        type: 'upload_biz_error',
        url: buildUrl(url),
        code: data && typeof data === 'object' ? data.code : undefined,
        msg: errorMsg
      })
      reject(new Error(errorMsg))
    } catch (err) {
      wx.hideLoading()
      wx.showToast({ title: '上传失败', icon: 'none' })
      logNet('error', {
        at: Date.now(),
        type: 'upload_fail',
        url: buildUrl(url),
        errMsg: (err && err.errMsg) ? err.errMsg : ((err && err.message) ? err.message : '上传失败')
      })
      reject(err)
    }
  })
}

module.exports = {
  request,
  requestStream,
  get,
  post,
  put,
  delete: del,
  getApiRoot,
  getApiHost,
  ping,
  uploadFile
}

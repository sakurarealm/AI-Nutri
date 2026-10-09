let baseUrl = ''
function setBaseUrl(url){ baseUrl = url }
let _cleanedOnce = false

function makeTtsFilePath(ext, key) {
  const safeExt = String(ext || 'wav').replace(/[^a-z0-9]/ig, '').toLowerCase() || 'wav'
  const safeKey = String(key || '').replace(/[^a-z0-9_]/ig, '').toLowerCase() || `${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 9)}`
  return `${wx.env.USER_DATA_PATH}/tts_${safeKey}.${safeExt}`
}

function pickExtByContentType(contentType) {
  const ct = String(contentType || '').toLowerCase()
  if (ct.includes('audio/wav') || ct.includes('audio/x-wav')) return 'wav'
  if (ct.includes('audio/ogg')) return 'ogg'
  if (ct.includes('audio/aac')) return 'aac'
  if (ct.includes('audio/mp4')) return 'm4a'
  if (ct.includes('audio/mpeg') || ct.includes('audio/mp3')) return 'mp3'
  if (ct.includes('audio/webm')) return 'webm'
  return ''
}

function pickExtByHeader(buffer) {
  try {
    if (!buffer || buffer.byteLength < 12) return ''
    const u8 = new Uint8Array(buffer)
    const s4 = String.fromCharCode(u8[0], u8[1], u8[2], u8[3])
    if (s4 === 'RIFF') return 'wav'
    if (s4 === 'OggS') return 'ogg'
    if (s4 === 'ID3') return 'mp3'
    if (u8[0] === 0xff && (u8[1] & 0xe0) === 0xe0) return 'mp3'
    if (s4 === 'fLaC') return 'flac'
    const s8 = String.fromCharCode(u8[4], u8[5], u8[6], u8[7])
    if (s8 === 'ftyp') return 'm4a'
    return ''
  } catch (e) {
    return ''
  }
}

function arrayBufferToUtf8(buffer) {
  const u8 = new Uint8Array(buffer || new ArrayBuffer(0))
  let out = ''
  const chunk = 0x8000
  for (let i = 0; i < u8.length; i += chunk) {
    out += String.fromCharCode.apply(null, u8.subarray(i, i + chunk))
  }
  try {
    return decodeURIComponent(escape(out))
  } catch (e) {
    return out
  }
}

function tryParseJsonFromArrayBuffer(buffer) {
  try {
    const raw = arrayBufferToUtf8(buffer || new ArrayBuffer(0))
    const trimmed = String(raw || '').trim()
    if (!trimmed) return null
    const first = trimmed[0]
    if (first !== '{' && first !== '[') return null
    return JSON.parse(trimmed)
  } catch (e) {
    return null
  }
}

function looksLikeJsonArrayBuffer(buffer) {
  try {
    if (!buffer || buffer.byteLength < 1) return false
    const u8 = new Uint8Array(buffer)
    const n = Math.min(u8.length, 64)
    for (let i = 0; i < n; i++) {
      const c = u8[i]
      if (c === 0x20 || c === 0x0a || c === 0x0d || c === 0x09) continue
      return c === 0x7b || c === 0x5b
    }
    return false
  } catch (e) {
    return false
  }
}

function readdirUserData(fs) {
  return new Promise((resolve) => {
    fs.readdir({
      dirPath: wx.env.USER_DATA_PATH,
      success: (res) => {
        const files = (res && (res.files || res.fileList)) || []
        resolve(Array.isArray(files) ? files : [])
      },
      fail: () => resolve([])
    })
  })
}

function unlinkSafe(fs, filePath) {
  return new Promise((resolve) => {
    if (!filePath) { resolve(false); return }
    fs.unlink({
      filePath,
      success: () => resolve(true),
      fail: () => resolve(false)
    })
  })
}

function statSafe(fs, path) {
  return new Promise((resolve) => {
    if (!path) { resolve(null); return }
    fs.stat({
      path,
      success: (res) => resolve(res && res.stats ? res.stats : null),
      fail: () => resolve(null)
    })
  })
}

function readdirSafe(fs, dirPath) {
  return new Promise((resolve) => {
    if (!dirPath) { resolve([]); return }
    fs.readdir({
      dirPath,
      success: (res) => {
        const files = (res && (res.files || res.fileList)) || []
        const out = Array.isArray(files) ? files : []
        resolve(out.map((it) => (typeof it === 'string' ? it : (it && it.name) ? it.name : '')).filter(Boolean))
      },
      fail: () => resolve([])
    })
  })
}

function rmdirSafe(fs, dirPath) {
  return new Promise((resolve) => {
    if (!dirPath) { resolve(false); return }
    if (typeof fs.rmdir === 'function') {
      fs.rmdir({
        dirPath,
        recursive: true,
        success: () => resolve(true),
        fail: () => {
          fs.rmdir({
            dirPath,
            success: () => resolve(true),
            fail: () => resolve(false)
          })
        }
      })
      return
    }
    resolve(false)
  })
}

async function rmrfSafe(fs, path) {
  const stats = await statSafe(fs, path)
  if (!stats) {
    await unlinkSafe(fs, path)
    return
  }
  if (typeof stats.isDirectory === 'function' && stats.isDirectory()) {
    const children = await readdirSafe(fs, path)
    for (const name of children) {
      await rmrfSafe(fs, `${path}/${name}`)
    }
    const ok = await rmdirSafe(fs, path)
    if (!ok) {
      try { await unlinkSafe(fs, path) } catch (e) {}
    }
    return
  }
  await unlinkSafe(fs, path)
}

async function cleanupTtsFiles(fs) {
  const files = await readdirUserData(fs)
  const names = files
    .map((it) => (typeof it === 'string' ? it : (it && it.name) ? it.name : ''))
    .filter(Boolean)

  const targets = names.filter((n) => /^tts_/.test(n) || /^tts_latest\./i.test(n))
  if (!targets.length) return

  for (const name of targets) {
    const fp = `${wx.env.USER_DATA_PATH}/${name}`
    await unlinkSafe(fs, fp)
  }
}

async function cleanupTtsLatestVariants(fs) {
  const names = await readdirSafe(fs, wx.env.USER_DATA_PATH)
  for (const name of names) {
    if (!/^tts_latest\./i.test(String(name || ''))) continue
    await rmrfSafe(fs, `${wx.env.USER_DATA_PATH}/${name}`)
  }
}

function getFileExt(name) {
  const s = String(name || '')
  const idx = s.lastIndexOf('.')
  if (idx < 0) return ''
  return s.slice(idx + 1).toLowerCase()
}

function isKeepUserDataFile(name) {
  const n = String(name || '').toLowerCase()
  if (!n) return false
  if (n.startsWith('avatar_latest.')) return true
  return false
}

function getKeepUserDataNamesFromStorage() {
  try {
    const user = wx.getStorageSync('userInfo') || {}
    const avatar = user.avatarUrl || user.avatar
    const s = String(avatar || '')
    if (!s.startsWith('wxfile://')) return new Set()
    const parts = s.split('/')
    const last = parts[parts.length - 1]
    const set = new Set()
    if (last) set.add(String(last).toLowerCase())
    set.add('avatar_latest.jpg')
    set.add('avatar_latest.jpeg')
    set.add('avatar_latest.png')
    set.add('avatar_latest.webp')
    return set
  } catch (e) {
    return new Set()
  }
}

async function cleanupSavedFileList(force) {
  if (typeof wx.getSavedFileList !== 'function' || typeof wx.removeSavedFile !== 'function') return
  const keepNames = getKeepUserDataNamesFromStorage()
  await new Promise((resolve) => {
    wx.getSavedFileList({
      success: async (res) => {
        const list = (res && res.fileList) ? res.fileList : []
        for (const it of list) {
          const filePath = it && it.filePath
          if (!filePath) continue
          try {
            const parts = String(filePath).split('/')
            const last = parts[parts.length - 1]
            if (last && keepNames.has(String(last).toLowerCase())) continue
          } catch (e) {}
          if (!force && /\/tts_/.test(String(filePath))) {
            await new Promise((r2) => wx.removeSavedFile({ filePath, complete: r2 }))
            continue
          }
          if (force) {
            await new Promise((r2) => wx.removeSavedFile({ filePath, complete: r2 }))
          }
        }
        resolve()
      },
      fail: () => resolve()
    })
  })
}

async function cleanupUserDataFiles(fs, force) {
  const names = await readdirSafe(fs, wx.env.USER_DATA_PATH)

  const keepNames = getKeepUserDataNamesFromStorage()

  for (const name of names) {
    const lower = String(name || '').toLowerCase()
    if (!lower) continue
    if (keepNames.has(lower)) continue
    if (isKeepUserDataFile(lower)) continue

    const shouldDelete = /^tts_/.test(lower) || !!force

    if (!shouldDelete) continue
    await rmrfSafe(fs, `${wx.env.USER_DATA_PATH}/${name}`)
  }
}

async function ensureCleanOnce(fs, force) {
  if (_cleanedOnce && !force) return
  await cleanupSavedFileList(!!force)
  await cleanupUserDataFiles(fs, !!force)
  _cleanedOnce = true
}

function synthesize(text, options) {
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
  const fileKey = (options && options.key) ? String(options.key) : ''
  const doRequest = () => new Promise((resolve, reject) => {
    wx.request({
      url: baseUrl + '/api/tts',
      method: 'POST',
      header: { 'content-type': 'application/json' },
      data: { text },
      responseType: 'arraybuffer',
      timeout: 120000,
      success: (res) => {
        const status = res.statusCode || 0
        const ct = (res.header && (res.header['content-type'] || res.header['Content-Type'])) || ''
        const data = res.data
        const isArrayBuffer = data && typeof data === 'object' && typeof data.byteLength === 'number'
        if (status !== 200) {
          let detail = ''
          if (isArrayBuffer) {
            const obj = tryParseJsonFromArrayBuffer(data)
            if (obj && (obj.message || obj.msg)) detail = String(obj.message || obj.msg)
          }
          console.error('tts request failed', { status, ct, detail })
          reject(new Error(detail ? `HTTP_${status}:${detail}` : `HTTP_${status}`))
          return
        }
        try {
          const fs = wx.getFileSystemManager()
          const lowerCt = String(ct || '').toLowerCase()
          const writeFileOnce = (filePath, dataToWrite) => new Promise((res2, rej2) => {
            fs.writeFile({
              filePath,
              data: dataToWrite,
              success: () => res2(true),
              fail: (err) => rej2(err)
            })
          })

          if ((lowerCt.includes('application/json') || looksLikeJsonArrayBuffer(data)) && isArrayBuffer) {
            const obj = tryParseJsonFromArrayBuffer(data) || {}
            const base64 = obj && obj.audio
            if (!base64) {
              const msg = obj && (obj.message || obj.msg) ? String(obj.message || obj.msg) : ''
              const code = obj && (obj.code || obj.status) ? String(obj.code || obj.status) : ''
              console.warn('tts: json response without audio field', { code, msg })
              reject(new Error(msg ? (code ? `TTS_${code}:${msg}` : msg) : 'UNEXPECTED_TTS_RESPONSE'))
              return
            }
            const binary = wx.base64ToArrayBuffer(base64)
            const ext = 'mp3'
            const fpath = makeTtsFilePath(ext, fileKey)
            Promise.resolve()
              .then(() => ensureCleanOnce(fs, false))
              .then(() => writeFileOnce(fpath, binary))
              .then(() => {
                resolve({ audioPath: fpath })
              })
              .catch(async (err) => {
                const msg = err && (err.errMsg || err.message) ? String(err.errMsg || err.message) : ''
                if (msg.includes('maximum size of the file storage limit is exceeded')) {
                  await ensureCleanOnce(fs, true)
                  try {
                    await writeFileOnce(fpath, binary)
                    resolve({ audioPath: fpath })
                    return
                  } catch (err2) {
                    const msg2 = err2 && (err2.errMsg || err2.message) ? String(err2.errMsg || err2.message) : ''
                    if (msg2.includes('maximum size of the file storage limit is exceeded')) {
                      _cleanedOnce = false
                      await ensureCleanOnce(fs, true)
                      await writeFileOnce(fpath, binary)
                      resolve({ audioPath: fpath })
                      return
                    }
                    reject(err2)
                    return
                  }
                }
                reject(err)
              })
            return
          }

          if (isArrayBuffer && data.byteLength >= 2) {
            const ext = pickExtByContentType(ct) || pickExtByHeader(data) || 'mp3'
            const fpath = makeTtsFilePath(ext, fileKey)
            Promise.resolve()
              .then(() => ensureCleanOnce(fs, false))
              .then(() => writeFileOnce(fpath, data))
              .then(() => {
                resolve({ audioPath: fpath })
              })
              .catch(async (err) => {
                const msg = err && (err.errMsg || err.message) ? String(err.errMsg || err.message) : ''
                if (msg.includes('maximum size of the file storage limit is exceeded')) {
                  await ensureCleanOnce(fs, true)
                  try {
                    await writeFileOnce(fpath, data)
                    resolve({ audioPath: fpath })
                    return
                  } catch (err2) {
                    const msg2 = err2 && (err2.errMsg || err2.message) ? String(err2.errMsg || err2.message) : ''
                    if (msg2.includes('maximum size of the file storage limit is exceeded')) {
                      _cleanedOnce = false
                      await ensureCleanOnce(fs, true)
                      await writeFileOnce(fpath, data)
                      resolve({ audioPath: fpath })
                      return
                    }
                    reject(err2)
                    return
                  }
                }
                reject(err)
              })
            return
          }

          console.warn('tts: unexpected response type', { status, ct, hasData: !!data })
          reject(new Error('UNEXPECTED_TTS_RESPONSE'))
        } catch (e) { console.error('tts writeFile error', e); reject(e) }
      },
      fail: (err) => {
        console.error('tts request failed', err)
        reject(err)
      }
    })
  })

  return doRequest().catch((err) => {
    const msg = err && (err.errMsg || err.message) ? String(err.errMsg || err.message) : ''
    const isTimeout = msg.includes('timeout')
    if (!isTimeout) throw err
    return new Promise((resolve) => setTimeout(resolve, 500)).then(() => doRequest())
  })
}
function cleanup() {
  try {
    const fs = wx.getFileSystemManager()
    _cleanedOnce = false
    return ensureCleanOnce(fs, true)
  } catch (e) {
    return Promise.resolve(false)
  }
}

module.exports = { synthesize, setBaseUrl, cleanup }

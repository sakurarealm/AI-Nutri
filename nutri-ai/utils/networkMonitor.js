let _online = true
let _listening = false
let _listeners = []

function isOnline() {
  return _online
}

function updateStatus(online) {
  const prev = _online
  _online = !!online
  if (prev !== _online) {
    _listeners.forEach((fn) => {
      try { fn(_online); } catch (e) {}
    })
  }
}

function start() {
  if (_listening) return

  try {
    wx.getNetworkType({
      success(res) {
        updateStatus(res.networkType !== 'none')
      }
    })
  } catch (e) {}

  try {
    wx.onNetworkStatusChange((res) => {
      updateStatus(res.isConnected)
    })
  } catch (e) {}

  _listening = true
}

function stop() {
  try {
    wx.offNetworkStatusChange()
  } catch (e) {}
  _listeners = []
  _listening = false
}

function onStatusChange(fn) {
  if (typeof fn !== 'function') return
  _listeners.push(fn)
  return () => {
    const idx = _listeners.indexOf(fn)
    if (idx >= 0) _listeners.splice(idx, 1)
  }
}

function guard(fn, offlineMessage) {
  if (!_online) {
    wx.showToast({ title: offlineMessage || '当前无网络连接', icon: 'none' })
    return Promise.reject(new Error('OFFLINE'))
  }
  return typeof fn === 'function' ? fn() : Promise.resolve()
}

module.exports = { isOnline, start, stop, onStatusChange, guard }

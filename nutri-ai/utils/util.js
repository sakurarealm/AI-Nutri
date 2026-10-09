// utils/util.js

/**
 * 格式化时间
 * @param {Date} date 日期对象
 * @param {String} format 格式化字符串，默认 'YYYY-MM-DD HH:mm:ss'
 */
const formatTime = (date, format = 'YYYY-MM-DD HH:mm:ss') => {
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hour = date.getHours()
  const minute = date.getMinutes()
  const second = date.getSeconds()

  return format
    .replace('YYYY', year)
    .replace('MM', month.toString().padStart(2, '0'))
    .replace('DD', day.toString().padStart(2, '0'))
    .replace('HH', hour.toString().padStart(2, '0'))
    .replace('mm', minute.toString().padStart(2, '0'))
    .replace('ss', second.toString().padStart(2, '0'))
}

/**
 * 获取当前时间戳
 */
const getCurrentTimestamp = () => {
  return Date.now()
}

/**
 * 时间戳转日期字符串
 * @param {Number} timestamp 时间戳
 * @param {String} format 格式化字符串
 */
const timestampToDate = (timestamp, format = 'YYYY-MM-DD HH:mm:ss') => {
  return formatTime(new Date(timestamp), format)
}

/**
 * 防抖函数
 * @param {Function} func 要防抖的函数
 * @param {Number} delay 延迟时间
 */
const debounce = (func, delay = 300) => {
  let timer = null
  return function(...args) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      func.apply(this, args)
    }, delay)
  }
}

/**
 * 节流函数
 * @param {Function} func 要节流的函数
 * @param {Number} delay 延迟时间
 */
const throttle = (func, delay = 300) => {
  let timer = null
  return function(...args) {
    if (!timer) {
      timer = setTimeout(() => {
        func.apply(this, args)
        timer = null
      }, delay)
    }
  }
}

/**
 * 深拷贝
 * @param {*} obj 要拷贝的对象
 */
const deepClone = (obj) => {
  if (obj === null || typeof obj !== 'object') return obj
  if (obj instanceof Date) return new Date(obj)
  if (obj instanceof Array) return obj.map(item => deepClone(item))
  if (typeof obj === 'object') {
    const clonedObj = {}
    for (let key in obj) {
      if (obj.hasOwnProperty(key)) {
        clonedObj[key] = deepClone(obj[key])
      }
    }
    return clonedObj
  }
}

/**
 * 生成随机字符串
 * @param {Number} length 字符串长度
 */
const generateRandomString = (length = 8) => {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
  let result = ''
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return result
}

/**
 * 验证手机号
 * @param {String} phone 手机号
 */
const validatePhone = (phone) => {
  const reg = /^1[3-9]\d{9}$/
  return reg.test(phone)
}

/**
 * 验证邮箱
 * @param {String} email 邮箱
 */
const validateEmail = (email) => {
  const reg = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
  return reg.test(email)
}

/**
 * 获取文件扩展名
 * @param {String} filename 文件名
 */
const getFileExtension = (filename) => {
  return filename.slice((filename.lastIndexOf('.') - 1 >>> 0) + 2)
}

/**
 * 格式化文件大小
 * @param {Number} bytes 字节数
 */
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

/**
 * 本地存储封装
 */
const storage = {
  set: (key, value) => {
    try {
      wx.setStorageSync(key, value)
      return true
    } catch (e) {
      console.error('存储失败:', e)
      return false
    }
  },
  get: (key, defaultValue = null) => {
    try {
      const value = wx.getStorageSync(key)
      return value !== '' ? value : defaultValue
    } catch (e) {
      console.error('获取存储失败:', e)
      return defaultValue
    }
  },
  remove: (key) => {
    try {
      wx.removeStorageSync(key)
      return true
    } catch (e) {
      console.error('删除存储失败:', e)
      return false
    }
  },
  clear: () => {
    try {
      wx.clearStorageSync()
      return true
    } catch (e) {
      console.error('清空存储失败:', e)
      return false
    }
  }
}

/**
 * 显示提示信息
 */
const showToast = (title, icon = 'none', duration = 2000) => {
  wx.showToast({
    title,
    icon,
    duration
  })
}

/**
 * 显示确认对话框
 */
const showConfirm = (content, title = '提示') => {
  return new Promise((resolve) => {
    wx.showModal({
      title,
      content,
      success: (res) => {
        resolve(res.confirm)
      }
    })
  })
}

module.exports = {
  formatTime,
  getCurrentTimestamp,
  timestampToDate,
  debounce,
  throttle,
  deepClone,
  generateRandomString,
  validatePhone,
  validateEmail,
  getFileExtension,
  formatFileSize,
  storage,
  showToast,
  showConfirm
}
function compressImage(src, options = {}) {
  const maxWidth = options.maxWidth || 1280
  const quality = options.quality || 0.75
  const maxSizeKB = options.maxSizeKB || 800

  return new Promise((resolve, reject) => {
    wx.getImageInfo({
      src,
      success(imgInfo) {
        let w = imgInfo.width
        let h = imgInfo.height
        const ratio = w / h

        if (w > maxWidth) {
          w = maxWidth
          h = Math.round(w / ratio)
        }

        wx.compressImage({
          src,
          quality: Math.round(quality * 100),
          compressedWidth: w,
          compressedHeight: h,
          success(res) {
            wx.getFileInfo({
              filePath: res.tempFilePath,
              success(fileInfo) {
                const sizeKB = Math.round((fileInfo.size || 0) / 1024)
                if (sizeKB > maxSizeKB && quality > 0.4) {
                  compressImage(res.tempFilePath, {
                    maxWidth: Math.round(w * 0.7),
                    quality: quality - 0.15,
                    maxSizeKB
                  }).then(resolve).catch(() => resolve({ tempFilePath: res.tempFilePath, compressed: true }))
                } else {
                  resolve({ tempFilePath: res.tempFilePath, compressed: true })
                }
              },
              fail() {
                resolve({ tempFilePath: res.tempFilePath, compressed: true })
              }
            })
          },
          fail() {
            resolve({ tempFilePath: src, compressed: false })
          }
        })
      },
      fail() {
        resolve({ tempFilePath: src, compressed: false })
      }
    })
  })
}

function compressOrPassthrough(src, options) {
  if (!src || typeof src !== 'string') {
    return Promise.resolve({ tempFilePath: src || '', compressed: false })
  }
  if (src.startsWith('http://') || src.startsWith('https://') || src.startsWith('wxfile://')) {
    return Promise.resolve({ tempFilePath: src, compressed: false })
  }
  return compressImage(src, options)
}

module.exports = { compressImage, compressOrPassthrough }

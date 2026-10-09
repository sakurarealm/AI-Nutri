const CACHE_NAME = 'glb-model-cache-v1';
const GLB_URL = 'https://avatars.readyplayer.me/67f8d2bbe11e99081d155cd6.glb?morphTargetsGroup=ARKit,Oculus+Visemes&morphTargets=mouthSmile,mouthOpen,eyesClosed,eyesLookUp,eyesLookDown&textureSizeLimit=1024&textureFormat=png';

self.addEventListener('install', (event) => {
  console.log('SW: 安装中，预缓存GLB...');
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll([GLB_URL]).then(() => {
        console.log('SW: GLB预缓存成功');
        self.skipWaiting();  // 立即激活新 SW
      }).catch((err) => {
        console.error('SW: 预缓存失败:', err);
      });
    })
  );
});

self.addEventListener('activate', (event) => {
  console.log('SW: 激活，清旧缓存');
  event.waitUntil(
    Promise.all([
      caches.keys().then((cacheNames) => {
        return Promise.all(
          cacheNames.map((cacheName) => {
            if (cacheName !== CACHE_NAME) {
              return caches.delete(cacheName);
            }
          })
        );
      }),
      self.clients.claim()  // 立即接管所有客户端
    ]).then(() => console.log('SW: 激活完成，接管页面'))
  );
});

self.addEventListener('fetch', (event) => {
  const url = event.request.url;
  if (url.includes('readyplayer.me') && url.endsWith('.glb')) {
    console.log('SW: 拦截GLB请求:', url);
    event.respondWith(
      caches.match(event.request).then((cached) => {
        if (cached) {
          console.log('SW: 从缓存返回GLB');
          return cached;
        }
        console.log('SW: 缓存miss，从网络fetch并缓存');
        return fetch(event.request).then((response) => {
          if (response.ok) {
            const clone = response.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(event.request, clone));
          }
          return response;
        }).catch((err) => {
          console.error('SW: fetch失败:', err);
          // 优化：离线返回空 GLB Blob，避免 3D 渲染崩
          return new Response(new Blob([]), {
            status: 503,
            statusText: 'Service Unavailable',
            headers: { 'Content-Type': 'model/gltf-binary' }
          });
        });
      })
    );
  }
});
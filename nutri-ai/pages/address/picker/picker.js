function normalizeText(v) {
  return String(v || '').trim();
}

function normalizeCityKey(v) {
  const s = normalizeText(v);
  return s
    .replace(/(省|市|地区|盟|特别行政区)$/, '')
    .replace(/(壮族自治区|回族自治区|维吾尔自治区|自治区)$/, '');
}

function buildDefaultLocations(cityName) {
  const city = normalizeCityKey(cityName) || '上海';
  if (city === '北京') {
    return [
      { id: 'b1', name: '国贸 CBD', address: '北京市 朝阳区 建国门外大街', latitude: 39.9083, longitude: 116.4612 },
      { id: 'b2', name: '中关村软件园', address: '北京市 海淀区 西北旺', latitude: 40.0416, longitude: 116.2791 },
      { id: 'b3', name: '望京 SOHO', address: '北京市 朝阳区 望京街', latitude: 39.9967, longitude: 116.4819 }
    ];
  }
  if (city === '广州') {
    return [
      { id: 'g1', name: '珠江新城', address: '广州市 天河区 珠江新城', latitude: 23.1185, longitude: 113.3247 },
      { id: 'g2', name: '广州塔', address: '广州市 海珠区 阅江西路', latitude: 23.1086, longitude: 113.3194 },
      { id: 'g3', name: '体育西路', address: '广州市 天河区 体育西路', latitude: 23.1356, longitude: 113.3230 }
    ];
  }
  if (city === '深圳') {
    return [
      { id: 's1', name: '福田 CBD', address: '深圳市 福田区 益田路', latitude: 22.5408, longitude: 114.0650 },
      { id: 's2', name: '南山科技园', address: '深圳市 南山区 科苑路', latitude: 22.5333, longitude: 113.9508 },
      { id: 's3', name: '深圳湾公园', address: '深圳市 南山区 滨海大道', latitude: 22.5081, longitude: 113.9502 }
    ];
  }
  return [
    { id: 'sh1', name: '陆家嘴金融城', address: '上海市 浦东新区 世纪大道', latitude: 31.2354, longitude: 121.5071 },
    { id: 'sh2', name: '静安寺商圈', address: '上海市 静安区 南京西路', latitude: 31.2236, longitude: 121.4456 },
    { id: 'sh3', name: '徐家汇', address: '上海市 徐汇区 漕溪北路', latitude: 31.1937, longitude: 121.4365 }
  ];
}

function getCityCenter(cityName) {
  const city = normalizeCityKey(cityName) || '上海';
  if (city === '北京') return { latitude: 39.9042, longitude: 116.4074 };
  if (city === '广州') return { latitude: 23.1291, longitude: 113.2644 };
  if (city === '深圳') return { latitude: 22.5431, longitude: 114.0579 };
  return { latitude: 31.2304, longitude: 121.4737 };
}

function buildRegionShort(region) {
  const r = Array.isArray(region) ? region : [];
  const a = normalizeText(r[0]);
  const b = normalizeText(r[1]);
  const c = normalizeText(r[2]);
  if (b && c) return `${b} ${c}`;
  if (b) return b;
  return a || '请选择';
}

function roundFixed(value, digits) {
  const n = Number(value);
  if (!Number.isFinite(n)) return '';
  const d = Number.isFinite(Number(digits)) ? Number(digits) : 5;
  return n.toFixed(d);
}

function toLocationId(prefix, latitude, longitude, tail) {
  const lat = roundFixed(latitude, 5);
  const lon = roundFixed(longitude, 5);
  const t = normalizeText(tail);
  return `${prefix}_${lat}_${lon}${t ? '_' + t : ''}`;
}

function buildMockNearbyLocations({ latitude, longitude, regionShort, pickedLocation } = {}) {
  const lat = Number(latitude);
  const lon = Number(longitude);
  const base = normalizeText(regionShort) || '附近地点';
  const centerText = roundFixed(lat, 5) && roundFixed(lon, 5) ? `${roundFixed(lat, 5)},${roundFixed(lon, 5)}` : '';
  const out = [];

  if (pickedLocation && pickedLocation.latitude != null && pickedLocation.longitude != null) {
    const pLat = Number(pickedLocation.latitude);
    const pLon = Number(pickedLocation.longitude);
    out.push({
      id: normalizeText(pickedLocation.id) || toLocationId('picked', pLat, pLon),
      name: normalizeText(pickedLocation.name) || '已选地点',
      address: normalizeText(pickedLocation.address) || base,
      latitude: pLat,
      longitude: pLon
    });
  }

  const offsets = [
    { dLat: 0.00045, dLon: 0.00020 },
    { dLat: 0.00015, dLon: -0.00055 },
    { dLat: -0.00035, dLon: 0.00060 },
    { dLat: -0.00060, dLon: -0.00020 },
    { dLat: 0.00070, dLon: -0.00010 },
    { dLat: -0.00010, dLon: 0.00085 }
  ];

  for (let i = 0; i < offsets.length; i += 1) {
    const pLat = Number.isFinite(lat) ? lat + offsets[i].dLat : 0;
    const pLon = Number.isFinite(lon) ? lon + offsets[i].dLon : 0;
    const id = toLocationId('poi', pLat, pLon, String(i + 1));
    out.push({
      id,
      name: `${base} · 附近地点 ${i + 1}`,
      address: `${base}${centerText ? `（${centerText}）` : ''}`,
      latitude: pLat,
      longitude: pLon
    });
  }

  return out;
}

function buildMockSearchLocations({ keyword, latitude, longitude, regionShort } = {}) {
  const k = normalizeText(keyword);
  const lat = Number(latitude);
  const lon = Number(longitude);
  const base = normalizeText(regionShort) || '附近';
  if (!k) return [];

  const out = [];
  const offsets = [
    { dLat: 0.00020, dLon: 0.00010 },
    { dLat: 0.00055, dLon: -0.00025 },
    { dLat: -0.00015, dLon: 0.00050 },
    { dLat: -0.00045, dLon: -0.00015 }
  ];

  for (let i = 0; i < offsets.length; i += 1) {
    const pLat = Number.isFinite(lat) ? lat + offsets[i].dLat : 0;
    const pLon = Number.isFinite(lon) ? lon + offsets[i].dLon : 0;
    out.push({
      id: toLocationId('search', pLat, pLon, `${k}_${i + 1}`),
      name: `${k} · 结果 ${i + 1}`,
      address: `${base} · 与中心点相关`,
      latitude: pLat,
      longitude: pLon
    });
  }

  return out;
}

Page({
  data: {
    statusBarHeight: 0,
    region: ['上海市', '上海市', '浦东新区'],
    regionShort: '上海市 浦东新区',
    keyword: '',
    latitude: 31.2304,
    longitude: 121.4737,
    scale: 16,
    locations: [],
    filteredLocations: [],
    selectedId: '',
    selectedLocation: null,
    syncingCenter: false
  },

  onLoad() {
    try {
      const sys = wx.getSystemInfoSync();
      const statusBarHeight = sys && typeof sys.statusBarHeight === 'number' ? sys.statusBarHeight : 0;
      this.setData({ statusBarHeight });
    } catch (e) {}

    let region = this.data.region;
    try {
      const cached = wx.getStorageSync('ADDRESS_PICKER_REGION');
      if (Array.isArray(cached) && cached.length === 3) region = cached;
    } catch (e) {}

    const cityName = Array.isArray(region) ? (region[1] || region[0]) : '';
    const center = getCityCenter(cityName);
    this.setData({
      region,
      regionShort: buildRegionShort(region),
      latitude: center.latitude,
      longitude: center.longitude
    });
    this.rebuildLocations({ clearSelection: true });

    wx.getLocation({
      type: 'gcj02',
      success: (res) => {
        const lat = Number(res.latitude);
        const lon = Number(res.longitude);
        if (!Number.isFinite(lat) || !Number.isFinite(lon)) return;
        this.setData({ latitude: lat, longitude: lon }, () => {
          this.rebuildLocations({ clearSelection: true });
        });
      },
      fail: () => {}
    });
  },

  onBack() {
    wx.navigateBack({ delta: 1 });
  },

  onAddAddress() {
    const selected = this.data.selectedLocation || null;
    if (!selected) return;
    const payload = {
      region: Array.isArray(this.data.region) ? this.data.region : [],
      regionShort: normalizeText(this.data.regionShort),
      location: selected,
      latitude: Number(this.data.latitude) || 0,
      longitude: Number(this.data.longitude) || 0
    };
    try {
      wx.setStorageSync('ADDRESS_PICK_RESULT', payload);
    } catch (e) {}
    wx.showToast({ title: '已添加', icon: 'success' });
    setTimeout(() => wx.navigateBack({ delta: 1 }), 350);
  },

  onRegionPick(e) {
    const value = e && e.detail ? e.detail.value : null;
    const region = Array.isArray(value) && value.length === 3 ? value : this.data.region;
    const cityName = Array.isArray(region) ? (region[1] || region[0]) : '';
    const center = getCityCenter(cityName);
    this.setData({
      region,
      regionShort: buildRegionShort(region),
      latitude: center.latitude,
      longitude: center.longitude,
      keyword: ''
    });
    this.rebuildLocations({ clearSelection: true });
    try {
      wx.setStorageSync('ADDRESS_PICKER_REGION', region);
    } catch (e) {}
  },

  onKeywordInput(e) {
    const keyword = normalizeText(e && e.detail ? e.detail.value : '');
    this.setData({ keyword, selectedId: '', selectedLocation: null }, () => {
      this.rebuildLocations({ clearSelection: false });
    });
  },

  onPickLocation(e) {
    const id = normalizeText(e && e.currentTarget && e.currentTarget.dataset ? e.currentTarget.dataset.id : '');
    if (!id) return;
    const list = Array.isArray(this.data.filteredLocations) ? this.data.filteredLocations : [];
    const selected = list.find((x) => x && normalizeText(x.id) === id) || null;
    if (!selected) return;

    const keyword = normalizeText(this.data.keyword);
    const nextLat = Number(selected.latitude);
    const nextLon = Number(selected.longitude);
    const canMove = Number.isFinite(nextLat) && Number.isFinite(nextLon);

    if (keyword) {
      if (canMove) {
        this.setData(
          { latitude: nextLat, longitude: nextLon, keyword: '', selectedId: id, selectedLocation: selected, syncingCenter: true },
          () => {
            this.rebuildLocations({ clearSelection: false, pickedLocation: selected });
            setTimeout(() => this.setData({ syncingCenter: false }), 250);
          }
        );
        return;
      }
      this.setData({ keyword: '', selectedId: id, selectedLocation: selected }, () => {
        this.rebuildLocations({ clearSelection: false, pickedLocation: selected });
      });
      return;
    }

    this.setData({ selectedId: id, selectedLocation: selected });
  },

  onRegionChange(e) {
    const type = e && e.type ? String(e.type) : '';
    if (type !== 'end') return;
    if (this.data.syncingCenter) return;
    this.setData({ syncingCenter: true });
    const ctx = wx.createMapContext('pickMap', this);
    ctx.getCenterLocation({
      success: (res) => {
        const lat = Number(res.latitude);
        const lon = Number(res.longitude);
        if (Number.isFinite(lat) && Number.isFinite(lon)) {
          this.setData({ latitude: lat, longitude: lon, selectedId: '', selectedLocation: null }, () => {
            this.rebuildLocations({ clearSelection: false });
          });
        }
      },
      complete: () => {
        this.setData({ syncingCenter: false });
      }
    });
  },

  rebuildLocations({ clearSelection, pickedLocation } = {}) {
    const keyword = normalizeText(this.data.keyword);
    const regionShort = normalizeText(this.data.regionShort);
    const latitude = Number(this.data.latitude);
    const longitude = Number(this.data.longitude);

    const list = keyword
      ? buildMockSearchLocations({ keyword, latitude, longitude, regionShort })
      : buildMockNearbyLocations({ latitude, longitude, regionShort, pickedLocation: pickedLocation || null });

    const next = {
      locations: list,
      filteredLocations: list
    };

    if (clearSelection) {
      next.selectedId = '';
      next.selectedLocation = null;
    }

    this.setData(next);
  }
});

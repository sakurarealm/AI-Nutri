# AI营养助手小程序

一个基于微信小程序的AI营养分析助手，帮助用户识别食物并获取营养信息。

## 项目结构

```
ai-nutri-wxmini/
├── app.js                 # 小程序主逻辑文件
├── app.json               # 小程序全局配置
├── app.wxss               # 小程序全局样式
├── project.config.json    # 项目配置文件
├── sitemap.json           # 站点地图配置
├── pages/                 # 页面目录
│   ├── index/             # 首页
│   │   ├── index.wxml
│   │   ├── index.js
│   │   ├── index.wxss
│   │   └── index.json
│   └── profile/           # 个人中心
│       ├── profile.wxml
│       ├── profile.js
│       ├── profile.wxss
│       └── profile.json
├── components/            # 自定义组件
│   └── loading/           # 加载组件
│       ├── loading.wxml
│       ├── loading.js
│       ├── loading.wxss
│       └── loading.json
├── utils/                 # 工具函数
│   ├── request.js         # 网络请求封装
│   └── util.js            # 通用工具函数
├── images/                # 图片资源
│   ├── icon_home.svg
│   ├── icon_home_selected.svg
│   ├── icon_profile.svg
│   ├── icon_profile_selected.svg
│   ├── icon_scan.svg
│   ├── icon_analysis.svg
│   ├── icon_plan.svg
│   └── default_avatar.svg
└── README.md              # 项目说明
```

## 功能特性

- 🏠 **首页**: 展示主要功能入口
- 👤 **个人中心**: 用户信息管理和统计数据
- 🔧 **工具函数**: 网络请求、通用工具等
- 🎨 **自定义组件**: 可复用的UI组件
- 📱 **响应式设计**: 适配不同屏幕尺寸

## 开发环境

- 微信开发者工具
- 小程序基础库版本: 2.19.4+

## 快速开始

1. 使用微信开发者工具打开项目
2. 配置小程序AppID（在project.config.json中修改appid字段）
3. 编译运行

## 待实现功能

- [ ] 食物识别功能
- [ ] 营养分析页面
- [ ] 饮食计划功能
- [ ] 历史记录管理
- [ ] 用户数据统计
- [ ] 设置页面

## 技术栈

- 微信小程序原生开发
- JavaScript ES6+
- WXSS样式
- SVG图标

## 注意事项

1. 请在project.config.json中配置正确的AppID
2. 图标文件使用SVG格式，需要转换为PNG格式用于小程序
3. 网络请求需要配置合法域名
4. 部分功能需要用户授权

## 贡献

欢迎提交Issue和Pull Request来改进项目。
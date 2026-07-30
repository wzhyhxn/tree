---
name: wechat-miniprogram
description: 【Phase 2】微信小程序开发规范。日后将 Web 前端的地图展示和树木上传分别包装为两个独立小程序，复用现有后端 API。涵盖 map 组件、扫码识别、图片直传 OSS。
---

# 微信小程序开发规范（Phase 2）

## 拆分为两个小程序

| 小程序 | 功能 | 调用的 API |
|--------|------|-----------|
| 校园植物地图 | 地图浏览、标记点、植物详情、扫码查看 | `GET /api/plants`、`GET /api/plants/{id}` |
| 树木信息采集 | 拍照上传、标位置、管理员身份校验 | `POST /api/admin/plants`、`POST /api/admin/plants/{id}/images` |

## 地图组件规范

```xml
<map
  id="campusMap"
  longitude="{{centerLng}}"
  latitude="{{centerLat}}"
  scale="{{scale}}"
  markers="{{markers}}"
  bindmarkertap="onMarkerTap"
  show-location
/>
```

- `markers` 坐标必须是 **GCJ-02**，后端 API 已通过高德转换
- 超过 200 个标记点开启点聚合：`enable-clustering`

## 扫码识别植物

```javascript
wx.scanCode({
  onlyFromCamera: true,
  success: (res) => {
    const plantCode = extractPlantCode(res.result);
    wx.navigateTo({ url: `/pages/plant/detail?code=${plantCode}` });
  }
});
```

## 请求封装
- 统一封装在 `utils/request.js`
- 基地址从 `app.js` 的 `globalData.apiBase` 读取
- Phase 2 采集端小程序统一注入 `X-Admin-Token` 请求头

## 注意事项
- 开发期在"详情" → "本地设置" → 勾选"不校验合法域名"
- 真机调试时 `localhost` 换成电脑局域网 IP

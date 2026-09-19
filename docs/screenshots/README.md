# 截圖規格與重拍指引

本目錄存放 [根目錄 README](../../README.md) 「視覺展示」章節所使用的應用截圖。

## 目前收錄的截圖

| 檔名 | 頁面 | 路徑 | 需登入 |
|------|------|------|:---:|
| `home.png` | 首頁 — 商品瀏覽 | `/` | — |
| `product.png` | 商品詳情 | `/products/:productId` | — |
| `cart.png` | 購物車 | `/cart` | — |
| `checkout.png` | Stripe 結帳 | `/checkout` | ✓ |
| `orders.png` | 我的歷史訂單 | `/orders` | ✓ |
| `profile.png` | 個人檔案 | `/profile` | ✓ |
| `contact.png` | 聯絡我們 | `/contact` | — |
| `orderManage.png` | 管理後台 — 訂單管理 | `/admin/orderManage` | ADMIN |
| `messages.png` | 管理後台 — 信息管理 | `/admin/messages` | ADMIN |

新增或更名截圖後，需同步更新根目錄 `README.md` 的「應用截圖」表格。

## 規格

| 項目 | 現況 | 說明 |
|------|------|------|
| 格式 | PNG | — |
| 解析度 | 1920 × 959 | 全部截圖統一，勿混用不同尺寸以免表格高度不一致 |
| 主題 | 淺色模式 | 專案支援深色模式切換（`Header.jsx`），如需展示可另存 `*-dark.png` |
| 檔案大小 | 14–84 KB | 建議單張 < 500 KB，必要時以 TinyPNG 等工具壓縮 |

## 重拍前準備

```bash
# 1. 啟動後端（自動植入 30 筆商品種子資料）
cd backend && ./mvnw spring-boot:run

# 2. 啟動前端
cd frontend && npm install && npm run dev
```

以 `admin@gmail.com` / `1234` 登入（該帳號同時具備 ADMIN 與 USER 角色）。

需注意的前置狀態：

- `checkout.png`、`cart.png` — 購物車需先加入商品，否則畫面為空
- `orders.png` — 需先完成至少一筆結帳
- `orderManage.png` — 僅顯示 `status = CREATED` 的訂單，全部處理完畢後畫面會空白
- `messages.png` — 僅顯示 `status = OPEN` 的留言，可從 `/contact` 送出新留言補充資料

結帳頁請使用 Stripe 測試卡號 `4242 4242 4242 4242`，有效日期填任意未來日期、CVC 任意三碼。

## 隱私注意事項

截圖前確認畫面中不含真實個人資料，一律使用種子帳號與 Stripe 測試卡號。

`contact.png` 會顯示 `application.properties` 中 `contact.*` 的聯絡資訊（電話、Email、地址）。這些值本就提交於版本庫中，但截圖會讓它們在 README 首頁更為顯眼；若不希望公開，可改用 qa profile（`contact.email=qa@gmail.com`、`contact.address=Taiwan`）啟動後再拍攝。

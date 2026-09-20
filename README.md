# StickerStore — 生產級全端電商系統

![React](https://img.shields.io/badge/React-19.2-61DAFB?logo=react&logoColor=white)
![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-7.1.0-6DB33F?logo=springsecurity&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-8.0-646CFF?logo=vite&logoColor=white)
![Redux Toolkit](https://img.shields.io/badge/Redux%20Toolkit-2.12-764ABC?logo=redux&logoColor=white)
![Axios](https://img.shields.io/badge/Axios-1.15-5A29E4?logo=axios&logoColor=white)
![Stripe](https://img.shields.io/badge/Stripe-32.1-635BFF?logo=stripe&logoColor=white)


> **React 19 × Spring Boot 4** 打造的完整電商解決方案 — 從前端互動體驗、Stripe 金流整合，到後端安全架構與多環境部署，每一層都按業界標準設計。

一套從零架構、可直接上線的貼紙電商系統。本專案貫穿全端工程的核心議題：**JWT 無狀態認證**、**Cookie-based CSRF 雙重防護**、**React Router Data API 資料流設計**、**Redux 與 Context 的狀態管理分工**，以及針對生產環境的多 Profile 配置與快取策略。

---

## 目錄

1. [視覺展示](#1-視覺展示)
2. [系統架構與專案結構](#2-系統架構與專案結構)
3. [核心功能與亮點](#3-核心功能與亮點)
4. [技術棧](#4-技術棧)
5. [快速開始與本地部署](#5-快速開始與本地部署)
6. [附錄](#6-附錄)

---

## 1. 視覺展示

### 應用截圖

**公開頁面**（無須登入）

| 首頁 — 搜尋與排序 | 商品詳情 |
|:---:|:---:|
| [![首頁](docs/screenshots/home.png)](docs/screenshots/home.png) | [![商品詳情](docs/screenshots/product.png)](docs/screenshots/product.png) |
| 關鍵字搜尋、三種排序、商品卡片網格 | 商品大圖、價格、數量選擇與加入購物車 |

| 購物車 | 聯絡我們 |
|:---:|:---:|
| [![購物車](docs/screenshots/cart.png)](docs/screenshots/cart.png) | [![聯絡我們](docs/screenshots/contact.png)](docs/screenshots/contact.png) |
| 數量調整、移除、即時小計；未登入亦可加入商品 | 左側聯絡資訊依 Profile 注入，表單為公開端點且免 CSRF |

**需登入**（`ProtectedRoute`）

| Stripe 結帳 | 我的歷史訂單 | 個人檔案 |
|:---:|:---:|:---:|
| [![結帳](docs/screenshots/checkout.png)](docs/screenshots/checkout.png) | [![歷史訂單](docs/screenshots/orders.png)](docs/screenshots/orders.png) | [![個人檔案](docs/screenshots/profile.png)](docs/screenshots/profile.png) |
| 分離式卡片欄位（卡號／有效日期／CVC） | 訂單狀態、總價、日期與明細品項 | 個人資料與收件地址一站管理 |

**管理後台**（需登入，ADMIN 權限由後端強制）

| 訂單管理 | 信息管理 |
|:---:|:---:|
| [![訂單管理](docs/screenshots/orderManage.png)](docs/screenshots/orderManage.png) | [![信息管理](docs/screenshots/messages.png)](docs/screenshots/messages.png) |
| 待處理訂單一鍵成立或取消 | 客服留言集中處理、標記已讀並關閉 |

### 使用者旅程

```mermaid
flowchart LR
    A["瀏覽商品<br/>搜尋 / 排序"] --> B["商品詳情"]
    B --> C["加入購物車<br/>localStorage 持久化"]
    C --> D{"已登入？"}
    D -- 否 --> E["登入 / 註冊"]
    E --> F["Stripe 結帳"]
    D -- 是 --> F
    F --> G["建立訂單<br/>status = CREATED"]
    G --> H["訂單成功頁"]
    H --> I["我的訂單<br/>追蹤狀態"]
    G -.-> J["管理員確認 / 取消<br/>CONFIRMED / CANCELLED"]
    J -.-> I
```

### 登入認證時序

```mermaid
sequenceDiagram
    participant U as 使用者
    participant F as React 前端<br/>(apiClient)
    participant C as CsrfController
    participant A as AuthController
    participant M as AuthenticationManager<br/>(ProviderManager)
    participant P as MyAuthenticationProvider
    participant J as JwtUtil
    participant DB as 資料庫

    U->>F: 輸入 email + 密碼
    opt 尚無 XSRF-TOKEN cookie
        F->>C: GET /api/v1/csrf-token
        C-->>F: Set-Cookie: XSRF-TOKEN
    end
    F->>A: POST /api/v1/auth/login<br/>{ userName, password } + X-XSRF-TOKEN
    A->>M: authenticate(UsernamePasswordAuthenticationToken)<br/>由前端傳入的 userName + password 封裝（尚未驗證）
    M->>P: authenticate()
    P->>DB: findByEmail(userName)
    DB-->>P: Customer + Roles
    P->>P: BCrypt.matches(密碼, password_hash)
    alt 密碼正確
        P-->>M: Authentication（principal = Customer）
        M-->>A: Authentication
        A->>A: 組裝 UserDto<br/>BeanUtils 複製 id/name/email/mobileNumber<br/>roles 併為逗號字串；address 另轉 AddressDto（無則略過）
        A->>J: generateJwtToken(authentication)
        J-->>A: JWT（HMAC-SHA256，20 分鐘效期）
        A-->>F: 200 LoginResponseDto<br/>{ message, user: UserDto, jwtToken }
        F->>F: 寫入 localStorage<br/>jwtToken 存字串、user 存 JSON.stringify(UserDto)
    else 密碼錯誤 / 查無使用者
        P-->>M: BadCredentialsException<br/>UsernameNotFoundException
        M-->>A: AuthenticationException
        A-->>F: 401 { message, null, null }
    end

    Note over F,A: 後續請求皆帶 Authorization 標頭（Bearer token）<br/>由 JWTTokenValidatorFilter 驗證
```

### 頁面地圖

```mermaid
flowchart TD
    Root["/ （App 版面）"]

    subgraph Public["公開路由"]
        H["/ ・/home<br/>Home + productsLoader"]
        AB["/about"]
        CT["/contact<br/>contactLoader + contactAction"]
        LG["/login<br/>loginAction"]
        RG["/register<br/>registerAction"]
        CA["/cart"]
        PD["/products/:productId"]
    end

    subgraph Protected["ProtectedRoute — 需登入"]
        CK["/checkout"]
        OS["/order-success"]
        OD["/orders<br/>ordersLoader"]
        PF["/profile<br/>profileLoader + profileAction"]
    end

    subgraph Admin["ADMIN 功能（後端強制授權）"]
        OM["/admin/orderManage<br/>orderManageLoader"]
        MS["/admin/messages<br/>messagesLoader"]
    end

    Root --> Public
    Root --> Protected
    Root --> Admin
    Root --> EP["ErrorPage<br/>errorElement"]
```

---

## 2. 系統架構與專案結構

### 系統架構

```mermaid
flowchart TB
    Browser["瀏覽器<br/>React 19 + Vite :5173"]

    subgraph Backend["Spring Boot 後端 :8080"]
        direction TB
        SFC["SecurityFilterChain"]
        CORS["CorsFilter<br/>CORS 檢查"]
        CSRF["CsrfFilter<br/>CSRF 驗證"]
        JWT["JWTTokenValidatorFilter<br/>OncePerRequestFilter"]
        AUTHZ["路徑授權規則<br/>permitAll / hasRole"]
        CTRL["Controller Layer<br/>/api/v1/*　@Valid 驗證"]
        SVC["Service Layer<br/>interface + impl"]
        REPO["Repository Layer<br/>Spring Data JPA"]
        GEH["GlobalExceptionHandler<br/>@RestControllerAdvice"]
    end

    CSRFR["CookieCsrfTokenRepository<br/>XSRF-TOKEN"]

    Cache["Caffeine Cache<br/>products 10min / roles 1day"]
    DB[("H2 file-based（dev）<br/>MySQL（prod）")]
    Stripe["Stripe API<br/>PaymentIntent"]

    Browser -- "HTTP/HTTPS<br/>Authorization: Bearer JWT<br/>X-XSRF-TOKEN" --> SFC
    SFC --> CORS --> CSRF --> JWT --> AUTHZ --> CTRL
    CSRF -.使用.-> CSRFR
    CTRL --> SVC
    SVC <--> Cache
    SVC --> REPO --> DB
    SVC --> Stripe
    CTRL -.拋出例外.-> GEH
    GEH -. "ExceptionResponseDto" .-> Browser
    Browser -. "卡號資料直送（不經後端）" .-> Stripe
```

### 前後端分離設計

- 後端為純 REST API，以 JSON 通訊，不渲染任何 HTML 頁面
- CORS 允許來源由 `stickerstore.cors.allowed-origins` 屬性控制，預設 `http://localhost:5173,http://localhost:8080`
- 前端以 Vite 多環境 `.env` 檔管理 `VITE_API_BASE_URL`，不硬編碼 API 位址
- 所有跨切面關注點（JWT 注入、CSRF token、401 處理）集中在單一 `apiClient.js`

### 前端專案結構

```
frontend/
├── .env / .env.dev / .env.production     三套 VITE_API_BASE_URL
├── vite.config.js                        port 5173、manualChunks 程式碼分割
├── eslint.config.js
├── index.html
├── public/
│   └── stickers/                         43 張商品貼紙圖（PNG）
└── src/
    ├── main.jsx                          createBrowserRouter 路由定義 + Provider 巢狀
    ├── App.jsx / App.css / index.css / custom.scss
    ├── api/
    │   └── apiClient.js                  Axios 實例 + Request/Response 攔截器
    ├── assets/util/                      emptycart.png、error.png、order-confirmed.png
    ├── components/
    │   ├── Header.jsx                    導覽列、購物車徽章、深色模式、使用者/管理選單
    │   ├── ErrorPage.jsx                 errorElement 統一錯誤頁
    │   ├── about/About.jsx
    │   ├── contact/Contact.jsx
    │   ├── footer/                       Footer.jsx + footer.module.css
    │   ├── cart/                         Cart、CartTable、CheckoutForm、OrderSuccess
    │   ├── home/
    │   │   ├── Home.jsx、PageHeading.jsx、PageTitle.jsx
    │   │   └── product/                  ProductListing、ProductCard、ProductDetail
    │   │                                 SearchBox、DropDown、Price
    │   └── login/
    │       ├── Login、Register、Profile、Orders、ProtectedRoute
    │       └── admin/                    OrderManage、Message
    ├── data/products.js                  靜態商品備援資料
    ├── store/
    │   ├── store.js                      configureStore + subscribe 同步 localStorage
    │   ├── cart-slice.js                 購物車 Redux slice
    │   ├── auth-context.jsx              AuthContext（JWT / 登入狀態）
    │   └── cart-context.jsx              遷移 Redux 前的遺留實作
    └── utils/authRouteGuards.js          登入後導回原頁的輔助函式
```

### 後端專案結構

```
backend/src/main/java/com/example/backend/
├── BackendApplication.java
├── config/
│   ├── CaffeineCacheConfig.java     products TTL 10min；roles TTL 1 day
│   ├── AuditorAwareImpl.java        從 SecurityContext 取 email 作為 auditor
│   ├── CorsConfig.java              allowedOrigins 由 application.properties 注入
│   └── StripeConfig.java            @PostConstruct 初始化 Stripe.apiKey
├── constant/
│   └── ApplicationConstants.java    JWT_SECRET、ORDER_STATUS_* 等常數集中定義
├── controller/                      @RestController，@RequestMapping("/api/v1/...")
│   ├── AuthController               登入、註冊
│   ├── ProductController            商品列表
│   ├── ContactController            聯絡表單（含 @ConfigurationProperties 聯絡資訊）
│   ├── ProfileController            個人資料 GET / PUT
│   ├── OrderController              訂單 GET / POST
│   ├── PaymentController            Stripe PaymentIntent
│   ├── AdminController              訂單管理、留言管理
│   ├── CsrfController               CSRF token 端點
│   ├── DummyController              教學示範：query param / path variable / headers
│   └── ScopeController              教學示範：Bean scope
├── dto/                             Response 物件（大量使用 Java Record）
├── entity/                          JPA 實體，繼承 BaseEntity
├── exception/
│   ├── GlobalExceptionHandler.java  @RestControllerAdvice 統一錯誤處理
│   ├── ResourceNotFoundException
│   └── DuplicateFieldException
├── payload/                         Request 物件（@Valid + Bean Validation）
├── repository/                      Spring Data JPA（介面自動生成實作）
├── scope/                           教學示範：Application/Request/Session Scoped Bean
├── security/
│   ├── MySecurityConfig.java        SecurityFilterChain 定義
│   ├── JWTTokenValidatorFilter.java OncePerRequestFilter，解析 Bearer token
│   ├── MyAuthenticationProvider.java 自訂認證邏輯（查 DB + BCrypt）
│   ├── JwtUtil.java                 generateJwtToken / 解析 Claims
│   └── PublicPathConfig.java        公開路徑 bean，集中管理
└── service/
    ├── *Service.java                介面定義（依賴倒置原則）
    └── impl/*ServiceImpl.java       業務邏輯實作

backend/src/main/resources/
├── application.properties           預設 profile（H2）
├── application-qa.properties        QA profile
├── application-prod.properties      Production profile（MySQL）
├── stripe.properties                stripe.apiKey（支援 STRIPE_API_KEY 覆寫）
└── sql/
    ├── schema.sql                   建立所有資料表
    └── data.sql                     MERGE INTO 冪等種子資料
```

### 資料模型

```mermaid
erDiagram
    CUSTOMERS ||--o| ADDRESS        : "address.customer_id UNIQUE NOT NULL"
    CUSTOMERS ||--o{ CUSTOMER_ROLES : "customer_roles.customer_id"
    ROLES     ||--o{ CUSTOMER_ROLES : "customer_roles.role_id"
    CUSTOMERS ||--o{ ORDERS         : "orders.customer_id NOT NULL"
    ORDERS    ||--o{ ORDER_ITEMS    : "order_items.order_id NOT NULL"
    PRODUCTS  ||--o{ ORDER_ITEMS    : "order_items.product_id NOT NULL"
    CUSTOMER_ROLES {
        bigint customer_id PK "FK，ON DELETE CASCADE"
        bigint role_id PK "FK，ON DELETE CASCADE"
    }
    ORDER_ITEMS {
        bigint order_item_id PK "代理鍵"
        bigint order_id FK "RESTRICT"
        bigint product_id FK "RESTRICT"
        int quantity "自身欄位"
        decimal price "下單當下的單價快照"
    }
    CONTACTS {
        bigint contact_id PK
        string status "OPEN / CLOSED"
    }
```

| 符號 | 讀作 |
|---|---|
| `\|\|` | 剛好一筆（必填） |
| `o\|` | 零或一筆（選填） |
| `}o` / `o{` | 零到多筆 |

逐條關聯：

| 關聯 | 讀法 | 外鍵位置 |
|---|---|---|
| `CUSTOMERS \|\|--o\| ADDRESS` | 一位客戶最多一筆地址；**註冊時不填，之後在個人檔案補** | `address.customer_id` **NOT NULL + UNIQUE** |
| `CUSTOMERS \|\|--o{ CUSTOMER_ROLES`<br/>`ROLES \|\|--o{ CUSTOMER_ROLES` | 兩條合起來構成多對多：一位客戶可有多個角色，一個角色可給多人 | 中介表 `customer_roles`（`customers` 表**不含** `role_id`）<br/>owning side：`Customer.roles` 的 `@JoinTable` |
| `CUSTOMERS \|\|--o{ ORDERS` | 一位客戶可有多筆訂單；**每筆訂單一定屬於某位客戶** | `orders.customer_id` **NOT NULL** |
| `ORDERS \|\|--o{ ORDER_ITEMS` | 一筆訂單含多個品項 | `order_items.order_id` **NOT NULL** |
| `PRODUCTS \|\|--o{ ORDER_ITEMS` | 一個商品可出現在多筆訂單明細中 | `order_items.product_id` **NOT NULL** |
| `CONTACTS` | 獨立資料表，沒有任何外鍵 | 無 |

#### 這些關聯寫在哪

與「外鍵全集中在單一資料表」的設計不同，**本專案的外鍵是分散的** —— 因此關聯宣告散落在四個 Entity 中，而非集中於 `Customer`。

```java
// Customer.java —— 只擁有 roles 這一個關聯
@ManyToMany(fetch = FetchType.EAGER)                                  // 明示 EAGER
@JoinTable(name = "customer_roles",                                   // 中介表
        joinColumns = @JoinColumn(name = "customer_id"),              // 指回本類別 Customer
        inverseJoinColumns = @JoinColumn(name = "role_id"))           // 指向集合元素 Role
private Set<Role> roles = new LinkedHashSet<>();

@OneToOne(mappedBy = "customer", cascade = CascadeType.ALL)           // mappedBy → inverse side
private Address address;                                              // 外鍵不在 CUSTOMERS，而在 ADDRESS
```

```java
// Address.java —— 持有 @JoinColumn，是 owning side
@OneToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "CUSTOMER_ID", nullable = false)                   // 存進 address.customer_id
private Customer customer;
```

```java
// Order.java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@OnDelete(action = OnDeleteAction.RESTRICT)                           // 有訂單的客戶不得刪除
@JoinColumn(name = "CUSTOMER_ID", nullable = false)
private Customer customer;

@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
private List<OrderItem> orderItems = new ArrayList<>();               // inverse side，預設 LAZY
```

```java
// OrderItem.java —— 兩個關聯都是 owning side
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@OnDelete(action = OnDeleteAction.RESTRICT)                           // 保留歷史訂單，禁止刪除來源
@JoinColumn(name = "ORDER_ID", nullable = false)
private Order order;

@ManyToOne(fetch = FetchType.LAZY, optional = false)
@OnDelete(action = OnDeleteAction.RESTRICT)
@JoinColumn(name = "PRODUCT_ID", nullable = false)
private Product product;
```

四個補充重點：

1. **沒有單一中心表** —— 外鍵分別落在 `ADDRESS`、`customer_roles`、`ORDERS`、`ORDER_ITEMS` 四處。整張圖實際上是兩個群組：以 `CUSTOMERS` 為核心的帳號群（地址、角色），以及 `ORDERS → ORDER_ITEMS → PRODUCTS` 的訂單鏈，兩者靠 `orders.customer_id` 相接。
2. **`CUSTOMER_ROLES` 與 `ORDER_ITEMS` 結構相同但性質不同** —— 兩者在圖上都是「兩條 `||--o{` 指向中間表」，但只有後者是獨立的 Entity：

   | | `CUSTOMER_ROLES` | `ORDER_ITEMS` |
   |---|---|---|
   | 定位 | **純中介表**（pure join table） | **關聯實體**（association entity） |
   | 主鍵 | 複合主鍵 `(customer_id, role_id)` | 代理鍵 `order_item_id` |
   | 自身欄位 | 無 | `quantity`、`price` |
   | 稽核欄位 | **無**（全專案唯一） | 有，繼承 `BaseEntity` |
   | JPA | 無 Entity，`@ManyToMany` + `@JoinTable` | `OrderItem` Entity，`@OneToMany` + 兩個 `@ManyToOne` |
   | 重複組合 | 複合主鍵擋掉 | 允許（代理鍵不同） |
   | 刪除來源 | `ON DELETE CASCADE`，關聯自動清除 | `RESTRICT`，禁止刪除以保全歷史訂單 |

   判準很單純：**中介表自己有沒有業務資料**。`order_items` 必須記錄「買了幾個、當時單價多少」，所以它得是 Entity；`customer_roles` 只是把客戶和角色連起來，不需要。
3. **`Customer.address` 是 inverse side** —— 判準是「誰身上有 `@JoinColumn`」，而 `@JoinColumn` 在 `Address` 上。所以 `Customer` 標的是 `mappedBy = "customer"`，僅為唯讀視角；真正寫入 `address.customer_id` 的是儲存 `Address` 的動作。`cascade = ALL` 讓儲存／刪除 `Customer` 時連帶處理其 `Address`。
4. **三個 `@OnDelete(RESTRICT)` 是刻意的** —— 訂單與訂單明細指向的來源（客戶、訂單、商品）都禁止刪除，以免歷史訂單失去參照。

| 類別 | 類型 | 注意事項 |
|---|---|---|
| `Customer` | JPA Entity | `roles` 為 **EAGER**；`address` 未指定 fetch，`@OneToOne` 預設也是 **EAGER** → 載入一次會連帶發出數筆 SQL |
| `Order` | JPA Entity | `orderItems` 為 **LAZY**（`@OneToMany` 預設） |
| `Address`、`OrderItem` | JPA Entity | 關聯皆明示 `LAZY`（`Address` 1 個、`OrderItem` 2 個） |
| `Product` | JPA Entity | **自身未宣告任何關聯** —— 被 `OrderItem.product` 單向參照，未設 inverse side（`Product` 上沒有 `orderItems` 集合） |
| `Contact` | JPA Entity | **完全獨立**，無任何外鍵進出 |
| `Role` | JPA Entity | `customers` 為 inverse side（`mappedBy = "roles"`），`@ManyToMany` 預設 **LAZY** |
| `BaseEntity` | `@MappedSuperclass` | 稽核欄位 `createdAt` / `createdBy` / `updatedAt` / `updatedBy` |

#### 資料層重點

- 開發環境使用 **H2 file-based**（`jdbc:h2:file:./h2db/myDb;AUTO_SERVER=true`），**資料會跨重啟保留** —— 與記憶體模式不同，devtools 熱重載或重新啟動都不會清空，手動建立的測試資料會一直累積。想重置就直接刪掉 `backend/h2db/` 整個目錄，下次啟動會依 `schema.sql` + `data.sql` 重建
- **`spring.jpa.hibernate.ddl-auto=validate`** —— Hibernate 在啟動時逐一比對每個 Entity 與實際資料表，**欄位缺失或型別不符會直接讓啟動失敗**；但它**只驗證，不建表也不修改任何結構**，因此 `sql/schema.sql` 仍是 schema 的唯一真相。
- 初始資料 `sql/data.sql` 全部使用 **H2 專屬的 `MERGE INTO ... KEY(...)`**（共 35 條）達成冪等 upsert，重複啟動不會產生重複資料。⚠️ **這個語法在 MySQL 上不成立** —— prod profile 設定 `spring.sql.init.mode=never` 迴避了這點，所以**正式環境的 schema 與種子資料必須另行建置**，不能指望這兩個檔案
- 種子資料內容：30 筆商品、3 個角色（`ROLE_ADMIN` / `ROLE_USER` / `ROLE_OP`）、1 個管理員（`admin@gmail.com`）、2 則示範留言。⚠️ `ROLE_OP` 已寫入且指派給管理員，但 `MySecurityConfig` 的授權規則**從未使用它**，屬預留角色
- 所有 Entity 繼承 `entity/BaseEntity` 的四個稽核欄位（`Instant createdAt` / `updatedAt`、`String createdBy` / `updatedBy`），由 `@EnableJpaAuditing` + `config/AuditorAwareImpl` 自動填入。未登入時 auditor 回傳的是 **`"SYSTEM"`**；已登入時取 `Customer.email`。註冊這類未登入寫入流程即靠此機制才不會因 `created_by` 為 null 而失敗
- `ORDER_ITEMS.price` 儲存的是**下單當下的單價快照**，與 `PRODUCTS.price` 解耦 —— 日後調整商品售價不會回頭改寫歷史訂單金額
- ⚠️ 商品列表掛了 `@Cacheable("products")`，TTL 10 分鐘。**直接改資料庫的商品資料後，最長需等 10 分鐘前端才會看到變化**；開發時要立即生效請重啟應用程式

**Fetch 策略**（決定一次查詢會連帶撈出多少資料）

| 關聯 | 實際註解 | Fetch | 來源 |
|------|---------|-------|------|
| `Customer.roles` → `Role` | `@ManyToMany(fetch = EAGER)` + `@JoinTable` | **EAGER** | 明示（覆寫掉 `@ManyToMany` 的 LAZY 預設） |
| `Customer.address` → `Address` | `@OneToOne(mappedBy = "customer", cascade = ALL)` | **EAGER** | 未指定 → `@OneToOne` 預設即 EAGER |
| `Address.customer` | `@OneToOne(fetch = LAZY, optional = false)` + `@JoinColumn` | LAZY | 明示；owning side（FK 在 `ADDRESS`） |
| `Order.customer` | `@ManyToOne(fetch = LAZY, optional = false)` | LAZY | 明示（覆寫掉 `@ManyToOne` 的 EAGER 預設） |
| `Order.orderItems` → `OrderItem` | `@OneToMany(mappedBy = "order", ...)` | LAZY | 未指定 → `@OneToMany` 預設即 LAZY |
| `OrderItem.order` / `.product` | `@ManyToOne(fetch = LAZY, optional = false)` ×2 | LAZY | 明示 |
| `Role.customers` | `@ManyToMany(mappedBy = "roles")` | LAZY | 未指定 → `@ManyToMany` 預設即 LAZY |

---

## 3. 核心功能與亮點

### 購物體驗

- **商品瀏覽**：30 款商品，支援關鍵字搜尋（同時比對名稱與描述，不分大小寫）與三種排序（熱門度、價格由低至高、價格由高至低），全部以 `useMemo` 在前端完成，零額外 API 呼叫
- **購物車持久化**：Redux Toolkit 管理，`store.subscribe()` 自動同步至 `localStorage`，刷新頁面零狀態遺失
- **Stripe 嵌入式結帳**：分離式卡片元件（卡號／到期日／CVC），逐欄位即時驗證與錯誤回饋
- **訂單追蹤**：完整訂單歷史，含付款狀態與訂單狀態
- **個人資料管理**：姓名、手機、送貨地址一站更新
- **深色模式**：`Header.jsx` 切換後寫入 `localStorage("mode")`，透過 `document.documentElement.classList` 搭配 Tailwind `dark:` 變體全站生效，Stripe 卡片元件樣式亦隨之調整
- **登入後導回原頁**：未登入存取受保護頁面時，原路徑存入 `sessionStorage.redirectPath`，登入完成後自動導回

### 管理後台

- **訂單看板**：列出所有 `status = CREATED` 的待處理訂單，一鍵確認或取消
- **客服留言管理**：集中處理所有 `status = OPEN` 的留言，可標記為已關閉
- **Swagger UI / OpenAPI**：SpringDoc 自動產生完整 API 規格，限 ADMIN 存取

### 平台安全

#### JWT 無狀態認證

```
1. POST /api/v1/auth/login
       ↓ { userName, password }　（userName 實際傳入 email）
         此端點雖為 permitAll，但未列入 CSRF 豁免清單
         → 必須先持有 XSRF-TOKEN cookie 並帶上 X-XSRF-TOKEN，否則 403
2. MyAuthenticationProvider
       ↓ 依 email 查詢 CUSTOMERS 資料表 → BCrypt.matches() 驗證密碼
3. JwtUtil.generateJwtToken()
       ↓ HMAC-SHA256 簽名，20 分鐘效期
         iss = "StickerStore"、sub = "JWT Token"
         Claims: username、email、mobileNumber、roles（逗號分隔字串）
4. 回傳 { message, user, jwtToken } → 前端存入 localStorage
5. 後續請求 Header: Authorization: Bearer <token>
6. JWTTokenValidatorFilter (OncePerRequestFilter)
       ↓ 驗證簽名 + 檢查效期 → 以 email 為 principal 設定 SecurityContext
7. 驗證失敗 → Filter 直接寫入 401 JSON（繞過 GlobalExceptionHandler）
8. 前端 Axios Response Interceptor
       ↓ 捕捉 401 → 清除 localStorage → 導向 /login
```

**設計要點**：JWT 為無狀態（Stateless），後端不需維護 Session，適合水平擴展。Token payload 內嵌 roles，省去每次請求查詢資料庫的開銷。

#### Cookie-based CSRF 雙重防護

JWT 與 CSRF 對應不同攻擊面，兩者並存：

| 攻擊類型 | 防護機制 |
|----------|----------|
| 未授權存取 API | JWT Bearer token 驗證 |
| 跨站請求偽造（CSRF） | Cookie + Header 雙重確認 |

實作細節：

- 後端設定 `CookieCsrfTokenRepository.withHttpOnlyFalse()`，在回應中附上 `XSRF-TOKEN` cookie（JavaScript 可讀）
- 前端 Axios Request Interceptor 在 POST / PUT / PATCH / DELETE 自動從 cookie 讀取 token，加入 `X-XSRF-TOKEN` header
- 若 cookie 不存在（初次載入），先呼叫 `GET /api/v1/csrf-token` 取得 token 後再送出原請求
- 兩組豁免（`ignoringRequestMatchers`）：
  - `/api/v1/contacts`、`/api/v1/contacts/**` —— 公開聯絡表單，讓未持有 token 的訪客也能送出留言
  - `PathRequest.toH2Console()` —— H2 Console 的登入表單（`login.do`）是一般 form POST，不會帶 `X-XSRF-TOKEN`

#### 存取控制（RBAC）

```
公開路徑             → permitAll()
  /error
  /api/v1/products/**
  /api/v1/contacts/**
  /api/v1/auth/**
  /api/v1/csrf-token
  /actuator/health/**

/api/v1/admin/**     → hasRole("ADMIN")
/swagger-ui/**       → hasRole("ADMIN")
/v3/api-docs/**      → hasRole("ADMIN")
/actuator/**         → hasRole("ADMIN")  （health 除外）

其餘所有路徑          → hasAnyRole("USER", "ADMIN")
```

公開路徑定義在 `PublicPathConfig.java` bean，集中管理避免分散於多個設定類別。

> **前後端授權分工**：前端 `ProtectedRoute` 僅檢查「是否已登入」，`Header.jsx` 則依 `user.role` 決定是否顯示管理選單。**ADMIN 權限的實際強制在後端** — 一般使用者即使手動輸入 `/admin/orderManage`，後端 `/api/v1/admin/**` 仍會回傳 403。這是刻意的設計：前端權限檢查只是 UX，不可作為安全邊界。

#### 密碼安全

- 註冊時以 `BCryptPasswordEncoder` 雜湊後儲存，不存明文
- 登入時 `BCrypt.matches()` 比對，不可逆推原始密碼

#### Stripe 付款流程

```
1. 使用者填寫信用卡資料
      ↓  CardNumberElement / CardExpiryElement / CardCvcElement
      （卡號資料只在 Stripe iframe 內，不進入 React state，亦不經過本專案後端）
2. POST /api/v1/payment/create-payment-intent
      ↓  { amount, currency }
      後端建立 PaymentIntent → 回傳 clientSecret
3. stripe.confirmCardPayment(clientSecret, { payment_method: { card } })
      ↓  Stripe.js 直接與 Stripe 伺服器通訊確認付款
4. 付款成功 → POST /api/v1/orders → 建立訂單紀錄 → 跳轉 /order-success
```

後端全程不接觸卡號，符合 **PCI-DSS**（Payment Card Industry Data Security Standard，支付卡產業資料安全標準）中 **SAQ A**（Self-Assessment Questionnaire A，最輕量的自評問卷等級，適用於卡號完全外包給第三方、自身伺服器從不接觸的商家）的整合模式。

### 性能設計

#### Caffeine 本地快取

| 快取名稱 | TTL | 觸發條件 | 失效時機 |
|---------|-----|---------|---------|
| `products` | 10 分鐘 | `GET /api/v1/products` | TTL 到期後下次請求時重建 |
| `roles` | 1 天 | 角色查詢 | TTL 到期後下次請求時重建 |

商品資料為唯讀且更新頻率極低，快取可大幅減少 DB 查詢。角色資料幾乎不變，適合較長的 TTL。

### 工程設計亮點

#### 狀態管理策略 — Redux 與 Context 的分工

本專案同時使用 **Redux Toolkit 與 React Context 兩種狀態管理方案，主要目的是練習兩者的寫法與差異**。以實際需求而言，這個規模的應用用單一方案就足夠。

不過兩者的分工仍有其合理性 —— 以下說明各自被放在什麼位置、以及為何適合：

**Redux Toolkit — 購物車**

- 購物車狀態需跨多個頁面共享（首頁、商品頁、購物車頁、結帳頁）
- 需要複雜的 Reducer 邏輯（加入相同商品時累加數量而非新增條目）
- `store.subscribe()` 在每次狀態變更後自動同步至 `localStorage["cart"]`，刷新頁面不遺失
- `createSlice` + Immer（內建）允許直接「修改」state，無需手寫展開運算子

```js
// cartSlice 核心邏輯示意
addToCart: (state, action) => {
  const existing = state.items.find(i => i.productId === action.payload.productId);
  existing ? existing.quantity++ : state.items.push({ ...action.payload, quantity: 1 });
}
```

**React Context — 身份驗證**

- 認證狀態（token、user）更新頻率極低（僅登入／登出時變動）
- 不需要 Redux DevTools 調試認證流程
- 避免過度工程化：Context 對於低頻更新的全局狀態已完全足夠
- `localStorage` 持久化讓頁面刷新後自動恢復登入狀態

#### React Router 7 Data API

放棄傳統 `useEffect` 在元件內拉資料的方式，改用 React Router Data API：

| 模式 | 說明 | 優點 |
|------|------|------|
| `loader` | 路由匹配時即執行資料獲取 | 元件掛載時資料已就位，無 Loading 狀態閃爍 |
| `action` | 表單提交時執行副作用（POST / PUT） | 提交邏輯與元件渲染解耦，複用性高 |
| `shouldRevalidate` | 控制 loader 是否重新執行 | 精細控制重新取資料的時機，避免不必要的 API 呼叫 |
| `throw new Response()` | 在 loader/action 中丟出錯誤 | 由 `ErrorPage.jsx` 統一捕捉，不需每個元件個別處理錯誤 |

#### Axios 攔截器設計

`apiClient.js` 作為唯一的 HTTP 通訊入口，透過攔截器集中處理跨切面關注點（Cross-cutting Concerns）：

```
Request Interceptor
  ├── 從 localStorage 讀取 jwtToken
  ├── 自動補上 Authorization: Bearer <token>
  ├── 若為非安全方法（POST/PUT/PATCH/DELETE）
  │     ├── 從 cookie 讀取 XSRF-TOKEN
  │     ├── 若無 cookie → GET /api/v1/csrf-token（首次載入）
  │     └── 補上 X-XSRF-TOKEN header
  └── 發送請求

Response Interceptor
  ├── 2xx → 正常回傳資料
  └── 401 → 清除 localStorage token + user → 導向 /login
```

#### 統一錯誤處理

`GlobalExceptionHandler` 以 `@RestControllerAdvice` 攔截所有例外，依例外類型回傳不同格式 — 完整對照表與 JSON 範例見[附錄：錯誤回應格式](#錯誤回應格式)。

#### JPA Auditing

`BaseEntity` 搭配 `@EnableJpaAuditing` 自動填入稽核欄位：

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @CreatedDate      LocalDateTime createdAt;
    @LastModifiedDate LocalDateTime updatedAt;
    @CreatedBy        String createdBy;    // email，由 AuditorAwareImpl 提供
    @LastModifiedBy   String updatedBy;
}
```

所有業務資料表皆繼承 `BaseEntity`，系統可追蹤任何記錄的建立者與修改者。

---

## 4. 技術棧

### 前端

| 技術 | 版本 | 用途 | 選用理由 |
|------|------|------|----------|
| **React** | 19.2.4 | UI 框架 | Concurrent features、函數元件 + Hooks 為主流，生態系豐富 |
| **Vite** | 8.0.4 | 建構工具 | 基於 ESM 的開發伺服器，HMR 速度遠優於 Webpack；多環境 `.env` 分檔管理 |
| **React Router DOM** | 7.14.1 | 用戶端路由 | Data API（loader / action）將資料獲取與元件渲染解耦，取代 useEffect 拉資料的舊模式 |
| **Redux Toolkit** | 2.12.0 | 購物車狀態管理 | 購物車需跨多個頁面共享並持久化，`createSlice` 大幅減少樣板程式碼 |
| **React Redux** | 9.3.0 | Redux 綁定層 | 提供 `useSelector` / `useDispatch`，與 React 18+ 並行渲染相容 |
| **React Context** | 內建 | 登入狀態管理 | 認證狀態更新頻率低，不需 Redux 的效能優化，Context 已足夠 |
| **Axios** | 1.15.0 | HTTP 客戶端 | 攔截器機制讓 JWT 注入與 CSRF token 處理集中在單一位置，避免重複程式碼 |
| **js-cookie** | 3.0.5 | Cookie 讀取 | 讀取 `XSRF-TOKEN` cookie，API 比原生 `document.cookie` 簡潔 |
| **Stripe JS / React Stripe** | 9.4.0 / 6.3.0 | 付款 UI | PCI-DSS 合規的嵌入式表單元件，卡號資料直接傳至 Stripe，後端不經手敏感資訊 |
| **Tailwind CSS** | 4.2.2 | 原子化樣式 | Utility-first 策略提升開發速度，`dark:` 變體讓深色模式實作極為精簡 |
| **Bootstrap** | 5.3.8 | UI 元件補充 | 快速建構表格、Modal 等標準元件，與 Tailwind 並存互補 |
| **Sass** | 1.99.0 | CSS 前處理器 | `custom.scss` 客製 Bootstrap 變數 |
| **styled-components** | 6.4.0 | CSS-in-JS | 局部元件動態樣式 |
| **React Toastify** | 11.1.0 | 通知提示 | API 操作回饋的輕量解決方案，可自訂位置與樣式 |
| **FontAwesome** | 7.2.0 | 圖示 | SVG Icon 方案（Solid / Regular / Brands），可 tree-shaking |
| **ESLint** | 9.39.4 | 靜態檢查 | Flat Config + react-hooks / react-refresh 插件 |

### 後端

| 技術 | 版本 | 用途 | 選用理由 |
|------|------|------|----------|
| **Spring Boot** | 4.1.1 | 應用框架 | 自動配置降低設定成本，與 Spring 生態系深度整合（Security、Data JPA、Actuator） |
| **Spring Framework** | 7.0.9 | 核心容器 | 由 Boot 4.1.1 管理，基線為 Jakarta EE 11 |
| **Java** | 25 | 執行環境 | 採用最新語法特性，Record class 用於 DTO 大幅簡化程式碼 |
| **Spring Security** | 7.1.0 | 認證與授權 | Filter Chain 架構提供細粒度的安全控制；**以 `<spring-security.version>` 明確覆寫**，Boot 4.1.1 原生搭配為 7.1.1 |
| **JJWT** | 0.13.0 | JWT 處理 | 業界標準 JWT 函式庫，支援 HMAC-SHA256 簽名與 Claims 解析 |
| **Jackson** | 3.1.5 | JSON 序列化 | Boot 4 預設改用 `tools.jackson`；另保留 Jackson 2（2.21.5）供 jjwt 使用，詳見下方說明 |
| **Spring Data JPA / Hibernate** | 7.4.5 | ORM | Repository 介面自動生成 CRUD，搭配 JPA Auditing 實現稽核紀錄 |
| **H2** | 2.4.240 | 開發資料庫 | 嵌入式資料庫（本專案用 file-based），無需安裝即可啟動 |
| **MySQL Connector/J** | 9.7.0 | 生產資料庫 | Production profile 切換至 MySQL，透過環境變數注入連線設定 |
| **Caffeine** | 3.2.4 | 記憶體快取 | JVM 本地快取，商品列表 TTL 10 分鐘，角色清單 TTL 1 天，避免頻繁查詢 DB |
| **Tomcat** | 11.0.24 | 內嵌容器 | 由 Boot 4 管理，對應 Jakarta Servlet 6.1 |
| **Stripe Java SDK** | 32.1.0 | 支付處理 | 官方 SDK，後端僅建立 PaymentIntent 並回傳 clientSecret，不接觸卡號資料 |
| **SpringDoc OpenAPI** | 3.1.1 | API 文件 | 3.x 才支援 Boot 4 / Framework 7（2.x 僅支援 Boot 3） |
| **Bean Validation** | — | 輸入驗證 | `@Valid` / `@Validated` 宣告式驗證，錯誤由 GlobalExceptionHandler 統一格式化 |
| **Lombok** | — | 樣板碼消除 | `@RequiredArgsConstructor` 產生建構子注入，`@Data`、`@Builder` 等減少重複程式碼 |
| **Spring Boot Actuator** | — | 健康檢查 | `/actuator/health` 公開，其餘路徑限 ADMIN，適用於 K8s liveness probe |
| **Spring Boot DevTools** | — | 開發體驗 | 程式碼變更自動重啟，縮短回饋循環 |
| **Maven** | 3.9+ | 建構工具 | 成熟穩定的依賴管理，內附 Maven Wrapper (`mvnw`) 免安裝 |

### Spring Boot 4 升級注意事項

本專案已由 Spring Boot 3.5.14 升級至 4.1.1，以下為升級過程中需要處理、且會影響後續維護的幾點。

**1. Jackson 3 與 jjwt 的相依衝突**

Boot 4 預設改用 Jackson 3（套件名為 `tools.jackson`），不再提供 Jackson 2。但 `jjwt-jackson` 0.13.0（目前最新版）仍相依 Jackson 2 的 `com.fasterxml.jackson.core:jackson-databind`，缺少時**簽發 JWT 會在執行期拋 `NoClassDefFoundError`**。

所幸 Boot 4 的 BOM 仍同時管理 Jackson 2（`jackson-2-bom` 2.21.5），因此 `pom.xml` 只需補上免版本號的相依即可：

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <scope>runtime</scope>
</dependency>
```

待 jjwt 推出支援 Jackson 3 的版本後即可移除。

**2. JSON 欄位順序改變（行為變更）**

Jackson 3 會將 `@Data` 等一般 POJO 的欄位以**字母順序**輸出，Java Record 則維持宣告順序。例如 `UserDto`：

```jsonc
// Boot 3 / Jackson 2
{ "id": 1, "name": "Admin", "email": "...", "address": { ... } }

// Boot 4 / Jackson 3
{ "address": { ... }, "email": "...", "id": 1, "name": "Admin" }
```

前端以具名解構（`const { message, user, jwtToken } = response.data`）取值，不依賴欄位順序，因此不受影響。若有外部串接方對順序有假設，需另行確認。

**3. Spring Security 版本覆寫**

Boot 4.1.1 原生搭配 Spring Security 7.1.1，本專案在 `pom.xml` 明確覆寫為 7.1.0：

```xml
<spring-security.version>7.1.0</spring-security.version>
```

移除該屬性即可回到 Boot 原生管理的 7.1.1。

**4. 隨升級一併清理的項目**

| 項目 | 原因 |
|------|------|
| 移除 `MySecurityConfig` 的 `userDetailsService` 記憶體 bean | Security 7 啟動時警告其與 `MyAuthenticationProvider` 衝突；該 bean 原本即未參與登入驗證，屬死碼 |
| 移除 `spring.jpa.database-platform` | Hibernate 7 會依 JDBC 連線自動判斷方言，明確指定會觸發 `HHH90000025` deprecation 警告 |
| Caffeine 快取加上 `recordStats()` | 未開啟時 Actuator 僅能取得 `cache.size`，並於啟動時發出警告 |
| prod profile 關閉 springdoc 端點 | springdoc 3 預設開啟 `/v3/api-docs` 與 `/swagger-ui.html` |

**5. `spring.jpa.open-in-view` 的啟動警告**

Boot 4 啟動時會出現：

```
spring.jpa.open-in-view is enabled by default. Therefore, database queries
may be performed during view rendering. Explicitly configure
spring.jpa.open-in-view to disable this warning
```

重點在最後一句 —— Spring Boot 要的是**顯式宣告**，不是一定要你關閉。本專案選擇寫上 `spring.jpa.open-in-view=true` 保留預設行為，警告即消失。

若要改為 `false`，實測會讓 `/api/v1/orders` 與 `/api/v1/admin/orderManage` 回 500 （`Cannot lazily initialize collection of role 'Order.orderItems' - no session`），因為 `Order.orderItems` 為 LAZY 而 DTO 組裝發生在交易之外。`OrderServiceImpl` 的 `getCustomerOrders()` 與 `getAllPendingOrders()` 已補上 `@Transactional(readOnly = true)`（這兩個是唯一會走到 `order.getOrderItems()` 的進入點），因此改為 `false` 也可正常運作 —— 兩種設定皆已實測通過。

---

## 5. 快速開始與本地部署

### 環境需求

| 項目 | 版本 | 備註 |
|------|------|------|
| **Java** | 25+ | `pom.xml` 指定 `<java.version>25</java.version>` |
| **Maven** | 3.9+ | 或直接使用專案內附的 `./mvnw` / `mvnw.cmd`，免安裝 |
| **Node.js** | 20.19+ 或 22.12+ | Vite 8 的最低要求 |
| **Stripe 帳號** | — | 選用。專案已內建測試金鑰，可直接試跑 |

### 步驟

**1. 複製專案**

```bash
git clone git@github.com:tonysk0210/react-reactSpringBoot.git
cd react-reactSpringBoot
```

**2. 啟動後端**

```bash
cd backend
./mvnw spring-boot:run        # Windows：mvnw.cmd spring-boot:run
```

伺服器啟動於 `http://localhost:8080`。首次啟動會自動：

- 從 `classpath:sql/schema.sql` 建立所有資料表
- 從 `classpath:sql/data.sql` 以 `MERGE INTO`（冪等 upsert）植入 30 筆商品、3 個角色、1 個管理員帳號與 2 則示範留言
- 資料庫檔案寫入 `backend/h2db/myDb`（file-based，重啟不遺失）

**3. 啟動前端**

```bash
cd frontend
npm install
npm run dev
```

應用程式啟動於 `http://localhost:5173`，並自動讀取 `.env` 指向 `http://localhost:8080/api/v1`。

**4. 登入試用**

開啟 `http://localhost:5173`，以下方預設帳號登入，或自行註冊新帳號。

### 預設帳號

| 角色 | Email | 密碼 | 說明 |
|------|-------|------|------|
| ADMIN | `admin@gmail.com` | `1234` | 種子資料帳號，同時擁有 `ROLE_ADMIN`、`ROLE_USER`、`ROLE_OP` 三個角色，可存取管理後台 |

> 透過 `/register` 自行註冊的帳號僅取得 `ROLE_USER`，無法存取 `/api/v1/admin/**`。
> 登入以 **email 作為帳號**（`MyAuthenticationProvider` 依 email 查詢）。

### 以 curl 直接呼叫 API

登入端點雖為公開路徑，但**未豁免 CSRF**，直接 POST 會得到 403。正確順序是先取 `XSRF-TOKEN` cookie，再帶著 `X-XSRF-TOKEN` 送出（這正是 `apiClient.js` 攔截器在做的事）：

```bash
# 1. 取得 CSRF token 並存入 cookie jar
TOKEN=$(curl -s -c cookies.txt http://localhost:8080/api/v1/csrf-token \
        | sed -E 's/.*"token":"([^"]+)".*/\1/')

# 2. 登入，取得 JWT（userName 傳入 email）
curl -s -b cookies.txt -X POST http://localhost:8080/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -H "X-XSRF-TOKEN: $TOKEN" \
     -d '{"userName":"admin@gmail.com","password":"1234"}'

# 3. 以 JWT 呼叫受保護端點
curl -s -H "Authorization: Bearer <上一步取得的 jwtToken>" \
     http://localhost:8080/api/v1/profile
```

商品列表為公開端點，不需任何認證：

```bash
curl -s http://localhost:8080/api/v1/products
```

### H2 Console 與 Swagger UI 的實際存取方式

> **⚠️ 這兩者目前無法直接用瀏覽器開啟。** 以下為實測結果，非設計預期。

`/h2-console` 與 `/swagger-ui/**` 都不在公開路徑清單中，而 `JWTTokenValidatorFilter` 對所有非公開路徑在缺少 `Authorization: Bearer` header 時就直接回 **401**，比 `formLogin` / `httpBasic` 更早執行。瀏覽器在網址列輸入 URL 無法附帶該 header，因此：

| 路徑 | 匿名 | 帶 Bearer JWT | 瀏覽器可用？ |
|------|------|--------------|------------|
| `/actuator/health` | 200 | 200 | ✅ |
| `/h2-console/` | 401 | 200 | ❌ 另受 `X-Frame-Options: DENY` 阻擋 frame，且 `login.do` 的 POST 會被 CSRF 擋下（403） |
| `/swagger-ui/index.html` | 401 | 200 | ❌ 頁面本身載入不了 |
| `/v3/api-docs` | 401 | 200 | ❌ |

**查看資料庫的可行做法** — `application.properties` 的 JDBC URL 帶有 `AUTO_SERVER=true`，即使應用程式正在執行，也能用外部工具直接連線同一個檔案資料庫：

```bash
# 以 H2 內附的 Shell 查詢（jar 位於本機 Maven repository）
cd backend
java -cp ~/.m2/repository/com/h2database/h2/2.4.240/h2-2.4.240.jar org.h2.tools.Shell \
  -url "jdbc:h2:file:./h2db/myDb;AUTO_SERVER=TRUE" -user sa -password "" \
  -sql "SELECT COUNT(*) FROM products;"
```

或在 IntelliJ IDEA Database、DBeaver 等工具中新增 H2 連線：

| 欄位 | 值 |
|------|----|
| JDBC URL | `jdbc:h2:file:<專案路徑>/backend/h2db/myDb;AUTO_SERVER=TRUE` |
| 帳號（Username） | `sa` |
| 密碼（Password） | （空白，不填） |

**查看 API 文件的可行做法** — 直接取得 OpenAPI JSON：

```bash
curl -H "Authorization: Bearer <你的 JWT>" http://localhost:8080/v3/api-docs
```

將輸出存成檔案後匯入 Postman / Insomnia，或貼到 [Swagger Editor](https://editor.swagger.io/) 檢視。

**若要讓兩者恢復瀏覽器可用**，需修改 `MySecurityConfig`：將 `/h2-console/**`、`/swagger-ui/**`、`/v3/api-docs/**` 加入 `PublicPathConfig`（或至少讓 JWT filter 略過），並補上 `http.headers(h -> h.frameOptions(f -> f.sameOrigin()))` 與 `/h2-console/**` 的 CSRF 豁免。本專案尚未套用此變更。

### Stripe 測試刷卡資訊

結帳頁面使用 Stripe 測試模式，請輸入以下測試卡號：

| 欄位 | 值 |
|------|----|
| 卡號 | `4242 4242 4242 4242` |
| 到期日 | 任意未來日期（例如 `12/34`） |
| CVC | 任意三位數（例如 `123`） |

### 可用指令

**前端**

```bash
npm run dev             # 開發伺服器（localhost:5173，支援 HMR）
npm run build           # 生產建構（使用 .env.production）
npm run build:dev       # 指向 dev 環境的建構（使用 .env.dev）
npm run build:localhost # --mode localhost（注意：無 .env.localhost，實際回退至 .env）
npm run preview         # 本機預覽生產建構結果（localhost:5173）
npm run lint            # ESLint 檢查
```

**後端**

```bash
./mvnw spring-boot:run                                   # 以 H2 啟動（預設 profile）
./mvnw spring-boot:run -Dspring-boot.run.profiles=qa     # QA profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod   # Production profile（需 MySQL）
./mvnw test                                              # 執行測試
./mvnw package -DskipTests                               # 打包為 JAR
```

### 環境變數

**後端**

| 變數 | 預設值 | 說明 |
|------|--------|------|
| `JWT_SECRET` | `jxgEQeXHuPq8VdbyYFNkANdudQ53YUn4` | JWT 簽名密鑰，**生產環境必須替換** |
| `STRIPE_API_KEY` | `stripe.properties` 內建測試金鑰 | 正式環境替換為 live key |
| `LOG_LEVEL` | `INFO` | Root logger 等級 |
| `LOG_FILE_NAME` | `./logs/app.log` | 日誌檔輸出路徑 |
| `JPA_SHOW_SQL` | `true` | 是否輸出 SQL 至日誌 |
| `HIBERNATE_FORMAT_SQL` | `true` | SQL 是否格式化輸出 |
| `DATABASE_HOST` | `localhost` | MySQL 主機（prod only） |
| `DATABASE_PORT` | `3306` | MySQL 連接埠（prod only） |
| `DATABASE_NAME` | `stickerstore` | MySQL 資料庫名稱（prod only） |
| `DATABASE_USERNAME` | `root` | MySQL 帳號（prod only） |
| `DATABASE_PASSWORD` | `root` | MySQL 密碼（prod only） |

**前端**

| 檔案 | `VITE_API_BASE_URL` | 用途 |
|------|---------------------|------|
| `.env` | `http://localhost:8080/api/v1` | 本機開發（預設） |
| `.env.dev` | `https://dev.stickerstore.com/api/v1` | Dev 測試環境 |
| `.env.production` | `https://d1llf3j3ji3al9.cloudfront.net/api/v1` | CloudFront 生產環境 |

### ⚠️ 部署前的安全檢查清單

本專案為教學／作品展示用途，多項敏感設定以**明文預設值**提交在版本庫中，方便開箱即用。正式部署前務必處理：

- [ ] **`JWT_SECRET`** — `ApplicationConstants.java` 內有明文預設值，須以環境變數覆寫為隨機長字串
- [ ] **Stripe Secret Key** — `backend/src/main/resources/stripe.properties` 內含明文 `sk_test_...` 測試金鑰，須以 `STRIPE_API_KEY` 環境變數覆寫（建議一併在 Stripe Dashboard 輪替該金鑰）
- [ ] **Stripe Publishable Key** — 硬編碼於 `frontend/src/main.jsx`，建議改為 `VITE_STRIPE_PUBLISHABLE_KEY` 環境變數
- [ ] **CORS 來源** — `application-prod.properties` 未覆寫 `stickerstore.cors.allowed-origins`，須補上生產域名
- [ ] **Actuator 暴露範圍** — 預設 `management.endpoints.web.exposure.include=*` 且 `env` / `configprops` 顯示實際值；雖已由 ADMIN 角色保護，生產環境建議收斂為必要端點
- [ ] **H2 Console** — `application-prod.properties` 已設為 `false`，確認生效
- [x] **API 文件端點** — Boot 4 升級時已於 prod profile 加入 `springdoc.api-docs.enabled=false` 與 `springdoc.swagger-ui.enabled=false`
- [ ] **資料庫憑證** — prod profile 的 `DATABASE_USERNAME` / `DATABASE_PASSWORD` 預設值為 `root`/`root`，務必覆寫

---

## 6. 附錄

### API 文件

所有端點前綴為 `/api/v1`。後端另提供 SpringDoc 產生的完整規格（均需 ADMIN 身份，且**須以 Bearer JWT 存取**，無法直接用瀏覽器開啟 —— 原因與取用方式見[此節](#h2-console-與-swagger-ui-的實際存取方式)）：

| 用途 | URL |
|------|-----|
| 互動式文件（Swagger UI） | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON Spec（可匯入 Postman / Insomnia） | `http://localhost:8080/v3/api-docs` |

下方表格已依實際 DTO / Payload 程式碼核對，可直接作為對接依據。

#### 認證（公開）

| 方法 | 路徑 | 請求 Body | 回應 |
|------|------|----------|------|
| POST | `/auth/login` | `{ userName, password }` | `{ message, user, jwtToken }` |
| POST | `/auth/register` | `{ name, email, mobileNumber, password }` | 201 Created |

> **`userName` 欄位傳入的是 email** — `MyAuthenticationProvider` 依 email 查詢使用者。
> 兩個端點雖為 `permitAll`，但**未列入 CSRF 豁免**，呼叫前必須先取得 `XSRF-TOKEN`（見下方 curl 範例）。
>
> `user` 物件為 `{ id, name, email, mobileNumber, role, address }`，其中 `role` 是逗號分隔字串，例如 `"ROLE_ADMIN,ROLE_OP,ROLE_USER"`。

#### 商品（公開）

| 方法 | 路徑 | 回應 |
|------|------|------|
| GET | `/products` | `[{ id, name, description, price, popularity, imageUrl, createdAt }]` |

> 主鍵欄位在 API 回應中名為 **`id`**（資料庫欄位為 `product_id`，由 `ProductDto` 映射）。

#### 聯絡表單（公開，免 CSRF）

| 方法 | 路徑 | 請求 Body | 回應 |
|------|------|----------|------|
| POST | `/contacts` | `{ name, email, mobileNumber, message }` | 201 Created |
| GET | `/contacts` | — | `ContactInfoDto`（從 `application.properties` 綁定） |

#### 個人資料（USER / ADMIN）

| 方法 | 路徑 | 請求 Body | 回應 |
|------|------|----------|------|
| GET | `/profile` | — | `{ name, email, mobileNumber, address, emailUpdated }` |
| PUT | `/profile` | `{ name, email, mobileNumber, street, city, state, postalCode, country }` | 同上 |

`address` 為巢狀物件 `{ street, city, state, postalCode, country }`。
`emailUpdated` 為布林值：當本次更新變動了 email 時回傳 `true`，前端 `Profile.jsx` 會**強制登出並導向 `/login`**（因為 JWT 內的 `email` claim 是後端辨識使用者的依據，已失效）。

#### 訂單（USER / ADMIN）

| 方法 | 路徑 | 請求 Body | 回應 |
|------|------|----------|------|
| POST | `/orders` | `{ totalPrice, paymentId, paymentStatus, orderItems: [{ productId, quantity, price }] }` | `"訂單建立成功"` |
| GET | `/orders` | — | `[{ orderId, status, totalPrice, createdAt, orderItems }]` |

建立訂單時由前端傳入 Stripe 回傳的 `paymentId` 與 `paymentStatus`；`orderItems` 需帶 `price`（下單當下的單價快照）。
查詢回應中的 `orderItems` 為 `[{ productName, quantity, price, imageUrl }]` —— 呈現的是商品名稱而非 `productId`。

#### 支付（USER / ADMIN）

| 方法 | 路徑 | 請求 Body | 回應 |
|------|------|----------|------|
| POST | `/payment/create-payment-intent` | `{ amount, currency }` | `{ clientSecret }` |

`amount` 的單位為**最小貨幣單位（cents）** —— 前端以 `totalPrice * 100` 換算後送出，`currency` 固定為 `"usd"`。

#### 管理功能（ADMIN only）

| 方法 | 路徑 | 說明 |
|------|------|------|
| GET | `/admin/orderManage` | 所有 `status=CREATED` 的待處理訂單 |
| PATCH | `/admin/orderManage/{orderId}/confirm` | 訂單狀態改為 `CONFIRMED` |
| PATCH | `/admin/orderManage/{orderId}/cancel` | 訂單狀態改為 `CANCELLED` |
| GET | `/admin/messages` | 所有 `status=OPEN` 的未處理留言 |
| PATCH | `/admin/messages/{contactId}/close` | 留言狀態改為 `CLOSED` |

`GET /admin/messages` 回應為 `[{ contactId, name, email, mobileNumber, message, status }]`。

#### 系統端點

| 方法 | 路徑 | 存取限制 | 說明 |
|------|------|---------|------|
| GET | `/api/v1/csrf-token` | 公開 | 取得 CSRF token（回傳 `{ parameterName, token, headerName }`） |
| GET | `/actuator/health` | 公開 | 健康檢查（適用 K8s probe） |
| GET | `/actuator/**` | ADMIN + Bearer JWT | 其餘 Actuator 端點 |
| GET | `/swagger-ui/index.html` | ADMIN + Bearer JWT | Swagger UI（瀏覽器無法直接開啟） |
| GET | `/v3/api-docs` | ADMIN + Bearer JWT | OpenAPI JSON Spec |
| — | `/h2-console/**` | 登入使用者 + Bearer JWT | H2 Web Console（瀏覽器無法直接開啟；prod profile 已停用） |

#### 教學示範端點（非業務功能）

專案保留兩組示範用 Controller，用於展示 Spring MVC 與 Bean Scope 機制，不屬於電商業務邏輯：

| Controller | 路徑 | 示範主題 |
|-----------|------|---------|
| `DummyController` | `/api/v1/dummy/param`、`/multiple-param`、`/user/{pv}`、`/multiple/{pv1}/posts/{pv2}`、`/headers`、`/request-entity` | `@RequestParam`、`@PathVariable`、`@RequestHeader`、`RequestEntity` 與 `@Validated` 參數驗證 |
| `ScopeController` | `/api/v1/scope/request`、`/session`、`/application`、`/test` | Request / Session / Application Bean Scope 生命週期 |

### 錯誤回應格式

`GlobalExceptionHandler`（`@RestControllerAdvice`）依例外類型回傳不同格式：

| 例外 | HTTP | 回應格式 |
|------|------|---------|
| `Exception`（未捕獲） | 500 | `ExceptionResponseDto` |
| `ResourceNotFoundException` | 404 | `ExceptionResponseDto` |
| `MethodArgumentNotValidException`（`@Valid @RequestBody`） | 400 | `Map<String, List<String>>` |
| `ConstraintViolationException`（`@RequestParam` / `@PathVariable`） | 400 | `Map<String, String>` |
| `DuplicateFieldException` | 400 | `Map<String, List<String>>` |

**一般錯誤**（`ExceptionResponseDto`）：

```json
{
  "apiPath": "uri=/api/v1/profile",
  "errorCode": "NOT_FOUND",
  "errorMessage": "Customer not found with id: 42",
  "errorTime": "2026-06-20T10:30:00"
}
```

**`@Valid` DTO 驗證失敗**（`MethodArgumentNotValidException`）：

```json
{
  "name": ["名字是必填的", "名字必須在 2 到 30 個字符之間"],
  "email": ["無效的電子郵件地址"]
}
```

**`@RequestParam` / `@PathVariable` 驗證失敗**（`ConstraintViolationException`）：

```json
{
  "param.p": "p 長度必須介於 5 到 30 個字元"
}
```

**欄位重複**（`DuplicateFieldException`，例如更新個人資料時 email 已被他人使用）：

```json
{
  "email": ["此 Email： john@gmail.com 已被其他帳號使用"],
  "mobileNumber": ["此手機號碼： 0912345678 已被其他帳號使用"]
}
```

> JWT 驗證失敗（token 過期或格式錯誤）由 `JWTTokenValidatorFilter` 直接寫入 401 JSON，**不經過** `GlobalExceptionHandler` — 因為 Filter 執行於 DispatcherServlet 之前。

### 資料表說明

| 資料表 | 主鍵 | 關鍵欄位 | 說明 |
|-------|------|---------|------|
| `CUSTOMERS` | customer_id | email UNIQUE, mobile_number UNIQUE, password_hash | 客戶主表，密碼以 BCrypt 雜湊儲存 |
| `ROLES` | role_id | name UNIQUE | `ROLE_ADMIN`、`ROLE_USER`、`ROLE_OP` |
| `CUSTOMER_ROLES` | (customer_id, role_id) | CASCADE DELETE | 多對多關聯的 Junction Table |
| `ADDRESS` | address_id | customer_id UNIQUE FK | 每位客戶最多一個地址，隨客戶刪除而刪除 |
| `PRODUCTS` | product_id | name, price DECIMAL(10,2), popularity, image_url | 30 筆種子資料，`popularity` 欄位供前端排序 |
| `ORDERS` | order_id | customer_id FK, total_price, payment_id, payment_status, order_status | `payment_id` 為 Stripe PaymentIntent ID |
| `ORDER_ITEMS` | order_item_id | order_id FK, product_id FK, quantity, price | 快照下單當下商品價格，與 `PRODUCTS` 解耦 |
| `CONTACTS` | contact_id | status (`OPEN` / `CLOSED`) | 聯絡表單，管理員可標記為 `CLOSED` |

除 `CUSTOMER_ROLES` 外，所有資料表均繼承 `BaseEntity` 的四個稽核欄位：`created_at`、`updated_at`、`created_by`、`updated_by`。`CUSTOMER_ROLES` 為純中介表（複合主鍵 + 兩個外鍵），無對應 Entity，故不帶稽核欄位。

> `ROLE_OP` 存在於種子資料且已指派給 admin 帳號，但目前 `MySecurityConfig` 的授權規則並未使用它 — 屬於預留角色。

### Spring Profiles 對照

| Profile | 資料庫 | SQL 初始化 | Log Level | SQL 輸出 | H2 Console | springdoc |
|---------|--------|-----------|-----------|---------|-----------|-----------|
| `default` | H2 file-based (`./h2db/myDb`) | 自動執行 schema.sql + data.sql | INFO | 顯示並格式化 | 啟用 | 啟用 |
| `qa` | H2 | 自動執行 | WARN | 關閉 | 啟用 | 啟用 |
| `prod` | MySQL（環境變數注入） | `never`（不執行） | ERROR | 關閉 | 停用 | 停用 |

資料庫方言（`spring.jpa.database-platform`）已於 Boot 4 升級時移除，改由 Hibernate 7 依 JDBC 連線自動判斷。

各 Profile 另綁定不同的聯絡資訊（`contact.phone` / `contact.email` / `contact.address`），由 `ContactInfoDto` 經 `@ConfigurationProperties` 注入，供 `GET /api/v1/contacts` 回傳。

### 已知限制與後續規劃

本專案為個人作品，以下為目前的已知缺口，誠實列出：

| 項目 | 現況 | 規劃 |
|------|------|------|
| **測試覆蓋** | 後端僅有 `BackendApplicationTests` 的 `contextLoads()` 冒煙測試；前端無測試框架 | 補 Service 層單元測試、Controller 層 `@WebMvcTest`、前端導入 Vitest + Testing Library |
| **CI/CD** | `main` 分支無 `.github/workflows`，無自動化建構與測試 | 建立 GitHub Actions：後端 `mvn verify`、前端 `npm run lint && npm run build` |
| **容器化** | 無 Dockerfile / docker-compose | 補前後端 Dockerfile 與一鍵啟動的 compose 設定 |
| **遺留程式碼** | `src/store/cart-context.jsx` 為遷移至 Redux 前的實作，`CheckoutForm.jsx` 仍同時引用 Context 與 Redux selector | 統一收斂至 Redux，移除 Context 版本 |
| **前端 Admin 守衛** | `ProtectedRoute` 僅檢查登入狀態，未檢查 ADMIN 角色（安全性由後端保證，但 UX 上會先看到 403） | 新增 `AdminRoute` 元件，前端先行攔截 |
| **H2 Console / Swagger UI 不可用** | `JWTTokenValidatorFilter` 對非公開路徑無 Bearer token 即回 401，早於 `formLogin` / `httpBasic`；加上 `X-Frame-Options: DENY` 與 CSRF，瀏覽器完全無法開啟這兩個工具 | 將相關路徑加入 `PublicPathConfig`、設定 `frameOptions.sameOrigin()`、為 `/h2-console/**` 豁免 CSRF |
| **`formLogin` / `httpBasic` 為無效設定** | `MySecurityConfig` 有啟用，但永遠不會被觸發（同上原因） | 移除以免誤導，或調整 filter 順序讓其真正生效 |
| **JWT 效期偏短** | 20 分鐘，且無 refresh token 機制，閒置後需重新登入 | 導入 refresh token，或延長效期並加上滑動續期 |
| **JWT 未帶 `customerId`** | 以 `email` 作為 principal，每次請求需依 email 反查使用者 | 於 claims 加入 `customerId`，減少查詢 |
| **交易邊界僅涵蓋訂單查詢** | 已為 `OrderServiceImpl` 的兩個查詢方法補上 `@Transactional(readOnly = true)`；但 `createOrder()`、`updateOrderStatus()` 等寫入方法仍未標註，多個 repository 操作非單一原子單位。OSIV 維持開啟（`true`），LAZY 關聯的載入時機因此較寬鬆 | 為寫入方法補上 `@Transactional`；查詢端可再用 `JOIN FETCH` / `@EntityGraph` 消除 N+1 |
| **jjwt 仍相依 Jackson 2** | Boot 4 已改用 Jackson 3，但 `jjwt-jackson` 0.13.0 仍需 Jackson 2，故 `pom.xml` 額外保留 `jackson-databind`（2.21.5） | 待 jjwt 發布支援 Jackson 3 的版本後移除該相依 |
| **`.env.localhost`** | `npm run build:localhost` 指定 `--mode localhost` 但無對應檔案，實際回退至 `.env` | 補上 `.env.localhost` 或移除該 script |
| **授權條款** | 無 LICENSE 檔 | 視用途補上 MIT 或其他授權 |

### 專案文件索引

| 檔案 | 用途 |
|------|------|
| `README.md` | 本文件 — 專案總覽 |
| `CLAUDE.md` | 給 Claude Code 的專案指引（架構速查、開發規範） |
| `AGENTS.md` | 給 AI 編碼代理的通用倉庫指引（英文） |
| `backend/CLAUDE.md` / `backend/AGENTS.md` | 後端專屬指引 |
| `frontend/README.md` | 前端快速啟動與指令 |
| `docs/screenshots/README.md` | 截圖規格與拍攝指引 |
| `backend/src/main/resources/getProducts.http` | HTTP Client 請求範例（IntelliJ / VS Code REST Client） |

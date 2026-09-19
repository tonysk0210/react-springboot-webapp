import axios from "axios";
import Cookies from "js-cookie";

// 1. 建立一個 Axios 實例，設定 baseURL、timeout、headers 等預設值
const apiClient = axios.create({
  // import.meta.env 是 Vite 提供的環境變數存取方式，VITE_ 開頭的變數會被 Vite 注入到前端程式碼中
  baseURL: import.meta.env.VITE_API_BASE_URL, // 從環境變數讀取 API 基礎 URL
  timeout: 50000, // request 最多等 50 秒
  headers: {
    "Content-Type": "application/json", // request body 的資料格式是 JSON
    Accept: "application/json", // 告訴後端希望 response 回傳 JSON
  },
  // apiClient 發出的 API 請求，在跨來源時允許瀏覽器攜帶及接收 cookie；
  // 讓登入等請求可以帶上 XSRF-TOKEN cookie。
  withCredentials: true,
});

// 2. 註冊 request interceptor：在 request 送出前統一補上 JWT 與必要的 CSRF token。
apiClient.interceptors.request.use(
  // 第一個函式處理正常情況：會在 request 送出前執行；這裡可以修改 config，讓這次 request 自動帶上 JWT header。 ** 處理 JWT 認證 **
  async (config) => {
    const jwtToken = localStorage.getItem("jwtToken");
    if (jwtToken) {
      // 2.1 在 HTTP request header 加上一個 Authorization 欄位，裡面放 JWT token。
      config.headers.Authorization = `Bearer ${jwtToken}`;
    }

    // 定義安全的 HTTP 方法，這些方法不需要 CSRF token ** 處理 CSRF 攻擊 **
    const safeMethods = ["GET", "HEAD", "OPTIONS"];
    // 只有非安全方法才需要 CSRF token
    if (!safeMethods.includes(config.method.toUpperCase())) {
      // a. 從 cookies 中取得 CSRF token
      let csrfToken = Cookies.get("XSRF-TOKEN");
      if (!csrfToken) {
        // b. 如果沒有 XSRF-TOKEN cookie，先呼叫後端 endpoint，讓 Spring Security 透過 Set-Cookie 設定 CSRF token。
        await axios.get(`${import.meta.env.VITE_API_BASE_URL}/csrf-token`, {
          // 這裡使用原始 axios，不會繼承上方 apiClient 的設定，
          // 因此要在這個獨立請求中再次允許跨來源 cookie，讓瀏覽器能接收並保存後端回傳的 Set-Cookie: XSRF-TOKEN=...。
          withCredentials: true,
        });
        // c. 從 cookies 中取得 CSRF token
        csrfToken = Cookies.get("XSRF-TOKEN");
        // d. 如果還是沒有，則拋出錯誤
        if (!csrfToken) {
          throw new Error("無法取得 CSRF token");
        }
      }
      // e. 將 CSRF token 加入 request header 供後端驗證 ( 關鍵：Spring Security CSRF filter 會檢查這個 header)
      // 惡意網站通常不能讀取你網站的 XSRF-TOKEN cookie 並放進自訂 header
      config.headers["X-XSRF-TOKEN"] = csrfToken;
    }

    return config;
  },
  // 第二個函式處理錯誤情況：不要在這裡吞掉錯誤，而是把錯誤繼續往外丟，讓呼叫 apiClient.get(...) / apiClient.post(...) 的地方可以用 catch 或 try...catch 接到。
  (error) => Promise.reject(error),
);

// 3. 註冊 apiClient 的 response interceptor：統一處理透過 apiClient 發出的請求中，後端回傳的 401 Unauthorized，例如 JWT 過期或無效
apiClient.interceptors.response.use(
  (response) => response, // 2xx response 會進這裡，直接回傳給呼叫端
  async (error) => {
    // 非 2xx response 預設會進這裡；error.response 代表後端有回應，只是 status 被 Axios 視為錯誤
    if (error.response && error.response.status === 401) {
      console.log(error.response.data); // "JWT Token 已過期！" - 401 Unauthorized 由後端給的 status

      const jwtToken = localStorage.getItem("jwtToken");
      if (jwtToken) {
        localStorage.removeItem("jwtToken"); // 移除過期的 JWT token
        localStorage.removeItem("user"); // 移除使用者資訊
        setTimeout(() => {
          window.location.href = "/login"; // 2 秒後跳轉到登入頁面，讓 ErrorPage 有時間短暫顯示
        }, 2000);
      }
    }
    return Promise.reject(error); // 不吞掉錯誤，讓原本呼叫 apiClient 的地方繼續用 catch / loader errorElement 處理
  },
);
/**
 * 所有「透過這個 apiClient instance 發出去」且後端回傳 401 的 request，都會被這個 response interceptor 攔到。
 * 這裡的 response / error 不是看 backend「有沒有正常寫出 response body」，而是看 Axios 怎麼判斷 HTTP status。
 * Axios 預設會把非 2xx HTTP status 視為錯誤；即使後端有回傳 response body，也會進入 error callback。
 *
 * 沒有登入時 /profile 通常是 route guard (requireAuth) 接住；token 過期時才比較可能是 apiClient response interceptor 接住。
 * 若 request path 符合後端 publicPaths，例如 /api/v1/products/**，JWTTokenValidatorFilter.shouldNotFilter() 會跳過 JWT 驗證；因此即使 request 帶了過期 token，只要後端仍回 2xx，就不會進這個 response interceptor 的 error callback。
 */

export default apiClient;

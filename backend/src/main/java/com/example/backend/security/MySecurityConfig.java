package com.example.backend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.password.HaveIBeenPwnedRestApiPasswordChecker;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity // 「我要開始用 Spring Security 保護我的 Web 應用」optional
public class MySecurityConfig {

    private final List<String> publicPaths;

    @Value("${stickerstore.cors.allowed-origins}") // 從 application.properties 檔案中取得跨域設定
    private String allowedOrigins;

    @Autowired
    public MySecurityConfig(@Qualifier("publicPaths") List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }

    // SecurityFilterChain = 建立一條自訂的 Spring Security 過濾鏈，告訴 Spring Security 每個 HTTP request 要經過哪些安全檢查。
    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        //http.csrf(csrfConfig -> csrfConfig.disable()); // 把 Spring Security 的 CSRF 保護關掉。


        // 1. 啟用 CSRF 防護：將 token 存在可由前端讀取的 XSRF-TOKEN cookie，讓前端在非安全請求中帶回 X-XSRF-TOKEN header 供後端驗證。(403 without handling CSRF token)
        http.csrf(csrfConfig ->
                csrfConfig.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()) // 讓 Spring Security 用 cookie 保存 CSRF token。cookie 名稱通常是：XSRF-TOKEN，讓前端 JavaScript 可以讀取這個 Cookie。
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()) // 讓 Spring Security 把 CsrfToken 放到 HttpServletRequest 的 attribute 裡。 CsrfController，也就是 /api/v1/csrf-token 可以回傳目前的 CSRF token 給前端。
                        .ignoringRequestMatchers("/api/v1/contacts", "/api/v1/contacts/**")); // 忽略 Swagger 文件與公開 contact API 的 CSRF 檢查，讓 Swagger 測試 contact form 時可不帶 X-XSRF-TOKEN 呼叫。
        /**
         * Backend 不另外保存 CSRF token 紀錄；
         * CookieCsrfTokenRepository 讓 cookie 本身成為 token 的保存來源，後端驗證時比對 UI 帶回來的 cookie token 與 header token 是否一致。
         *
         * 前端送出 POST / PUT / DELETE ...
         * ↓
         * request 進入 Spring Security filter chain
         * ↓
         * CsrfFilter 攔截 request
         * ↓
         * CookieCsrfTokenRepository 從 cookie 讀取/載入 server 期待的 token
         * ↓
         * CsrfTokenRequestAttributeHandler / CSRF 機制從 request header 讀 X-XSRF-TOKEN
         * ↓
         * Spring Security 比對 cookie 裡的 token 和 header 裡的 token
         * ↓
         * 一致：放行 request
         * 不一致或缺少：回 403 Forbidden
         */

        // 2. 在 Spring Security 中開啟 CORS，並指定它使用 corsConfigurationSource() 這份跨域設定。CORS：限制哪些網站可以存取我的 API。
        http.cors(corsConfig -> corsConfig.configurationSource(corsConfigurationSource()));

        // 3. 設定哪些路徑不需要驗證；越具體、越嚴格的規則放前面；越籠統、fallback 的規則放後面；anyRequest() 永遠放最後
        http.authorizeHttpRequests((request) -> {
            // 3.1 公開路徑
            publicPaths.forEach(path ->
                    request.requestMatchers(path).permitAll());
            // 3.2 限制路徑：需要 ADMIN 角色
            request.requestMatchers(
                    "/api/v1/admin/**",
                    "/actuator/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**").hasRole("ADMIN");
            // 3.3 其他路徑：需要 USER 或 ADMIN 角色
            request.anyRequest().hasAnyRole("USER", "ADMIN");
        });

        // 4. 將自訂 JWT 驗證 filter 加入 Spring Security filter chain，並排在 BasicAuthenticationFilter 前面。讓 protected API 可以被授權訪問。
        // 這樣帶有 Authorization: Bearer <token> 的 request 會先被 JWT filter 驗證，驗證成功後會把 Authentication 放進 SecurityContext，供後續授權規則使用。
        http.addFilterBefore(new JWTTokenValidatorFilter(publicPaths), BasicAuthenticationFilter.class);

        http.formLogin(withDefaults()); // 啟用 Spring Security 預設表單登入頁面與表單登入流程
        http.httpBasic(withDefaults()); // 啟用 HTTP Basic Auth，Client 透過 Authorization header 傳送帳號密碼
        return http.build();
    }

    // CorsConfigurationSource = 提供 CORS 規則給 Spring Security 使用。
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // 1. 創建一個 CorsConfiguration 物件，用於配置 CORS 設置
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(","))); // 允許哪些來源的請求，例如 http://localhost:5173。
        config.setAllowedMethods(List.of("*")); // 允許哪些 HTTP method，例如 GET / POST / PUT / DELETE / OPTIONS。
        config.setAllowedHeaders(List.of("*")); // 允許前端 request 可以帶哪些 header
        config.setAllowCredentials(true); // 允許瀏覽器在跨來源 request 攜帶 credentials，例如 cookies、HTTP auth。這需要前端也設定 axios withCredentials: true，否則瀏覽器仍不會送 cookies。
        config.setMaxAge(3600L); // preflight OPTIONS 檢查結果可被瀏覽器快取 3600 秒，減少重複預檢請求。

        // 2. 創建一個 UrlBasedCorsConfigurationSource 物件，用於註冊 CORS 設置
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // 將這組 CORS 規則套用到所有後端路徑。
        return source;
    }

    // PasswordEncoder = 當 Spring Security 系統需要 PasswordEncoder 時，請使用 BCryptPasswordEncoder (會直接影響{noop}造成衝突: Encoded password does not look like BCrypt)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /*
     * AuthenticationManager = 當 AuthController 呼叫 authenticate() 時，使用 ProviderManager 調度自訂的 MyAuthenticationProvider。
     *
     * 註：本專案不註冊 UserDetailsService bean。
     * 帳號驗證一律由 MyAuthenticationProvider 直接查詢 CUSTOMERS 資料表完成，
     * 若同時存在 UserDetailsService bean，Spring Security 7 會在啟動時發出
     * InitializeUserDetailsManagerConfigurer 警告（兩者擇一即可）。
     */
    @Bean
    public AuthenticationManager authenticationManager(MyAuthenticationProvider myAuthenticationProvider) {
        return new ProviderManager(myAuthenticationProvider);
    }

    // 檢查密碼是否在「被竊取的密碼清單」裡面
    @Bean
    public CompromisedPasswordChecker compromisedPasswordChecker() {
        return new HaveIBeenPwnedRestApiPasswordChecker();
    }

}

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

        // 啟用 CSRF 防護：修改資料的請求必須帶有正確的 CSRF token，否則回傳 403 forbidden。
        http.csrf(csrfConfig ->
                // 透過 XSRF-TOKEN cookie 提供 token，並允許 React (JavaScript) 讀取。
                csrfConfig.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // 讓 CSRF filter 在 request 中準備 token，供 CsrfController 取得並回傳。
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        // 聯絡表單免除 CSRF 檢查。
                        .ignoringRequestMatchers("/api/v1/contacts", "/api/v1/contacts/**"));
        /**
         * CSRF 流程：
         * 1. 前端沒有 token 時，先 GET /api/v1/csrf-token。
         * 2. 瀏覽器保存後端透過 Set-Cookie 提供的 XSRF-TOKEN。
         * 3. 前端將 cookie 中的 token 放入 X-XSRF-TOKEN header。
         * 4. CsrfFilter 比對 cookie 與 header；相同就放行，不同或缺少就回傳 403。
         *
         * GET、HEAD、OPTIONS 等安全方法通常不需要 CSRF token。
         */

        // 2. 在 Spring Security 中開啟 CORS，並指定它使用 corsConfigurationSource() 這份跨域設定。CORS：限制哪些網站可以存取我的 API。
        http.cors(corsConfig -> corsConfig.configurationSource(corsConfigurationSource()));

        // 3. 設定哪些路徑不需要驗證；越具體、越嚴格的規則放前面；越籠統、fallback 的規則放後面；anyRequest() 永遠放最後
        http.authorizeHttpRequests((request) -> {
            // 3.1 公開路徑
            publicPaths.forEach(path -> request.requestMatchers(path).permitAll());
            // 3.2 限制路徑：需要 ADMIN 角色
            request.requestMatchers("/api/v1/admin/**", "/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").hasRole("ADMIN");
            // 3.3 其他路徑：需要 USER 或 ADMIN 角色
            request.anyRequest().hasAnyRole("USER", "ADMIN");
        });

        // 4. 將自訂 JWT 驗證 filter 加入 Spring Security filter chain，並排在 BasicAuthenticationFilter 前面。讓 protected API 可以被授權訪問。
        // 這樣帶有 Authorization: Bearer <token> 的 request 會先被 JWT filter 驗證，驗證成功後會把 Authentication 放進 SecurityContext，供後續授權規則使用。
        http.addFilterBefore(new JWTTokenValidatorFilter(publicPaths), BasicAuthenticationFilter.class);

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

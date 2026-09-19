package com.example.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/csrf-token")
public class CsrfController {

    // 讓 Spring Security 建立 CSRF token，並透過 Set-Cookie 提供 XSRF-TOKEN。
    // response body 會回傳 token；目前前端主要從 cookie 讀取它。
    @GetMapping
    public CsrfToken getCsrfToken(HttpServletRequest request) {
        // 取出 token 並回傳；序列化過程也會觸發 token 產生與 cookie 設定。
        return (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    }
    /**
     * 前端發現自己沒有 CSRF token
     * ↓
     * 呼叫後端 /api/v1/csrf-token
     * ↓
     * Spring Security 產生/取得 CsrfToken
     * ↓
     * 後端透過 CookieCsrfTokenRepository 設定 XSRF-TOKEN cookie，並回傳 token body
     * ↓
     * 前端之後從 cookie 讀 XSRF-TOKEN
     * ↓
     * 非 GET/HEAD/OPTIONS request 加上 X-XSRF-TOKEN header
     * ↓
     * 後端驗證 cookie token 和 header token 是否一致
     */
}

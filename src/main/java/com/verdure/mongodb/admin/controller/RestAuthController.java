package com.verdure.mongodb.admin.controller;

import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.request.AuthGithubRequest;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.utils.AuthStateUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author verdure
 * @date 2021/5/25 12:07 下午
 */
@RestController
@RequestMapping("/oauth")
public class RestAuthController {

    @RequestMapping("/render")
    public void renderAuth(HttpServletResponse response) throws IOException {
        AuthRequest authRequest = getAuthRequest();
        String authorize = authRequest.authorize(AuthStateUtils.createState());
        response.sendRedirect(authorize);
    }

    @RequestMapping("/callback")
    public Object login(AuthCallback callback) {
        AuthRequest authRequest = getAuthRequest();
        return authRequest.login(callback);
    }

//    private AuthRequest getAuthRequest() {
//        return new AuthGithubRequest(AuthConfig.builder()
//                .clientId("5cc7d6c50b931557b644")
//                .clientSecret("909dc93434980c95eeceba7e5601a15c7bb3a35d")
//                .redirectUri("http://47.98.136.116:8080/oauthed/")
//                .build());
//    }

    private AuthRequest getAuthRequest() {
        return new AuthGithubRequest(AuthConfig.builder()
                .clientId("0517e32ea6ebf6bac9ed")
                .clientSecret("4004c07b00e93710fc3fa790774ddae49d2c85c6")
                .redirectUri("http://127.0.0.1:8080/oauthed/")
                .build());
    }
}
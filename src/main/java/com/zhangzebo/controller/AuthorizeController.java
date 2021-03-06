package com.zhangzebo.controller;

import com.zhangzebo.dto.AccessTokenDTO;
import com.zhangzebo.dto.GitHubUser;
import com.zhangzebo.model.User;
import com.zhangzebo.provider.GitHubProvider;
import com.zhangzebo.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

//  第三方登录授权
@Controller
@Slf4j
public class AuthorizeController {
    @Autowired
    private GitHubProvider gitHubProvider;

    // @Value("${github.client.id}")
    private String clientId = "cefddbb5756bb3e83b2c";
    // @Value("${github.client.secret}")
    private String clientsecret = "d9b13209aee21471bed8299ceba0c9ff9c3f169a";
    // @Value("${github.redirect.uri}")
    private String redirectUri = "http://101.132.135.99/callback";

    @Autowired
    private UserService userService;

    @GetMapping("/callback")
    public String callback(@RequestParam("code") String code, @RequestParam("state") String state,
                           HttpServletResponse response) {
        //  获取用户信息
        AccessTokenDTO accessTokenDTO = new AccessTokenDTO();
        accessTokenDTO.setClient_id(clientId);
        accessTokenDTO.setClient_secret(clientsecret);
        accessTokenDTO.setCode(code);
        accessTokenDTO.setRedirect_uri(redirectUri);
        accessTokenDTO.setState(state);
        //  生成token
        String accessToken = gitHubProvider.getAccessToken(accessTokenDTO);
        GitHubUser gitHubUser = gitHubProvider.getUser(accessToken);
        if (gitHubUser != null && gitHubUser.getId() != null) {
            User user = new User();
            String token = UUID.randomUUID().toString();
            user.setAccountId(String.valueOf(gitHubUser.getId()));
            user.setName(gitHubUser.getName());
            //  把token放到user对象中
            user.setToken(token);
            user.setAvatarUrl(gitHubUser.getAvatarUrl());
            userService.createOrUpdate(user);
            //  把token存储到cookie中
            response.addCookie(new Cookie("token", token));
            return "redirect:/index";
        } else {
            //  登录失败，重新登录
            log.error("callback get github user info failure, {}", gitHubUser);
            return "redirect:/index";
        }
    }

    //  退出登录
    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        request.getSession().removeAttribute("user");
        Cookie cookie = new Cookie("token", null);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/";
    }
}

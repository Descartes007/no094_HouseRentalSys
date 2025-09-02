package org.example.springboot.util;


import cn.hutool.core.date.DateUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.example.springboot.entity.User;
import org.example.springboot.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;

@Component
public class JwtTokenUtils {
    private static UserService staticUserService;
    @Resource
    private  UserService userService;
    public static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenUtils.class);
    @PostConstruct
    public void setUserService() {
        staticUserService=userService;
    }
    public static String genToken(String userId,String sign){
    return JWT.create().withAudience(userId).withExpiresAt(DateUtil.offsetHour(new Date(),2)).sign(Algorithm.HMAC256(sign));
    }
    public static User getCurrentUser(){
        String token=null;
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            if (request == null) {
                LOGGER.error("获取当前请求上下文失败");
                return null;
            }
            
            // 从请求头获取token
            token = request.getHeader("token");
            
            // 如果请求头没有，尝试从请求参数获取
            if (StringUtils.isBlank(token)) {
                token = request.getParameter("token");
                LOGGER.debug("从请求参数获取token: {}", token);
            }
            
            // 如果还是没有，尝试从cookie获取
            if (StringUtils.isBlank(token)) {
                jakarta.servlet.http.Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    for (jakarta.servlet.http.Cookie cookie : cookies) {
                        if ("token".equals(cookie.getName())) {
                            token = cookie.getValue();
                            LOGGER.debug("从cookie获取token: {}", token);
                            break;
                        }
                    }
                }
            }
            
            if (StringUtils.isBlank(token)) {
                LOGGER.warn("获取当前登录的token失败，未找到token");
                return null;
            }

            try {
                String userId = JWT.decode(token).getAudience().get(0);
                LOGGER.debug("解析token成功，用户ID: {}", userId);
                return staticUserService.getUserById(Long.valueOf(userId));
            } catch (Exception e) {
                LOGGER.error("解析token失败: {}", token, e);
                return null;
            }
        } catch (Exception e) {
            LOGGER.error("获取当前用户信息失败，token: {}, 异常: {}", token, e.getMessage());
            return null;
        }
    }
}

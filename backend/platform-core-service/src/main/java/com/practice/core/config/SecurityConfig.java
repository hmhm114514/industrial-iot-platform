package com.practice.core.config;

import jakarta.servlet.http.*;import org.springframework.beans.factory.annotation.Value;import org.springframework.context.annotation.Configuration;import org.springframework.http.MediaType;import org.springframework.web.servlet.config.annotation.*;import java.io.IOException;

@Configuration
public class SecurityConfig implements WebMvcConfigurer {
    @Value("${app.token:panda-iot-demo-token}") String token;
    @Override public void addCorsMappings(CorsRegistry registry) { registry.addMapping("/**").allowedOriginPatterns("*").allowedMethods("GET","POST","PUT","DELETE","PATCH","OPTIONS").allowedHeaders("*").allowCredentials(false).maxAge(3600); }
    @Override public void addInterceptors(InterceptorRegistry registry) { registry.addInterceptor(new TokenInterceptor(token)).addPathPatterns("/api/**").excludePathPatterns("/api/auth/login"); }
}
class TokenInterceptor implements org.springframework.web.servlet.HandlerInterceptor {
    private final String token; TokenInterceptor(String token) { this.token = token; }
    @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.equals("Bearer " + token)) return true;
        response.setStatus(401); response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"未登录或Token无效\",\"data\":null}"); return false;
    }
}

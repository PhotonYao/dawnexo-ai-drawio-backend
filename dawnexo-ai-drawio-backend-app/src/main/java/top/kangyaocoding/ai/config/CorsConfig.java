package top.kangyaocoding.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * 全局跨域配置
 * 允许的来源从配置读取（application.yml 的 app.cors.allowed-origins，默认取环境变量 CORS_ALLOWED_ORIGINS），
 * 多个来源用英文逗号分隔，* 表示全部允许；
 * 生产环境在 docker-compose.yml 中注入，例如 http://your-domain.com,http://your-server-ip
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // originPatterns 兼容 * 与多域名混配，也为将来开启凭证（cookie）预留余地
                .allowedOriginPatterns(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

}

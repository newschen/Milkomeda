package pub.yizzuide.milkomeda.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import yiz.milkomeda.pulsar.Pulsar;

/**
 * WebMvcConfig
 *
 * @author yizzuide
 * Create at 2019/03/26 22:10
 */
@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    // 配置异步支持：超时、线程池等
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        Pulsar.configureAsyncSupport(configurer, 1000);
    }
}

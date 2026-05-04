package com.tony.kingdetective.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.config
 * @className: AsyncConfig
 * @author: Tony Wang
 * @date: 2025/9/22 22:24
 */
@Configuration
public class WebMvcAsyncConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        AsyncTaskExecutor asyncTaskExecutor = new ConcurrentTaskExecutor(VirtualThreadConfig.VIRTUAL_EXECUTOR);
        configurer.setTaskExecutor(asyncTaskExecutor);
        configurer.setDefaultTimeout(120_000); // 可选：120秒
    }
}
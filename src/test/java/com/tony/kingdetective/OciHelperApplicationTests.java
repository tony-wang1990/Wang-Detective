package com.tony.kingdetective;

import com.tony.kingdetective.service.IOciKvService;
import com.tony.kingdetective.utils.CustomExpiryGuavaCache;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:target/king-detective-test.db",
        "king-detective.startup.tasks-enabled=false",
        "king-detective.websocket.server-endpoint-exporter-enabled=false",
        "oci-cfg.key-dir-path=target/test-keys"
})
class OciHelperApplicationTests {

    @Resource
    private ApplicationContext applicationContext;

    @Resource
    private IOciKvService kvService;

    @Resource
    private CustomExpiryGuavaCache<String, Object> customCache;

    @Test
    void contextLoadsWithLocalTestDatabase() {
        assertThat(applicationContext).isNotNull();
        assertThat(kvService.count()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void customCacheExpiresValues() throws InterruptedException {
        customCache.put("cache-test-key", "value", 50);

        assertThat(customCache.get("cache-test-key")).isEqualTo("value");
        Thread.sleep(80);
        assertThat(customCache.get("cache-test-key")).isNull();
    }
}

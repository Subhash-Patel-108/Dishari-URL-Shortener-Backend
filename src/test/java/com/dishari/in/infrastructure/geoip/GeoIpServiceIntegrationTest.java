package com.dishari.in.infrastructure.geoip;

import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

// ✅ No @SpringBootTest — GeoIpService needs no Spring context
@DisplayName("GeoIpService Integration Tests")
class GeoIpServiceIntegrationTest {

    private GeoIpService geoIpService;

    @BeforeEach
    void setUp() {
        geoIpService = new GeoIpService();
        ReflectionTestUtils.setField(geoIpService,
                "countryDbPath",
                "src/test/resources/geoip/GeoLite2-Country.mmdb");
        ReflectionTestUtils.setField(geoIpService,
                "cityDbPath",
                "src/test/resources/geoip/GeoLite2-City.mmdb");
        geoIpService.init();
    }

    @AfterEach
    void tearDown() {
        geoIpService.close();
    }

    @Test
    @DisplayName("Service should initialize successfully")
    void serviceShouldInitializeSuccessfully() {
        assertThat(geoIpService).isNotNull();
    }

    @Test
    @DisplayName("Should resolve country for public IP without exception")
    void shouldResolveCountryForPublicIp() {
        assertThatCode(() ->
                geoIpService.getCountryCode("8.8.8.8"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should resolve location for public IP without exception")
    void shouldResolveLocationForPublicIp() {
        GeoIpService.GeoLocation location =
                geoIpService.getLocation("8.8.8.8");

        assertThat(location).isNotNull();
    }

    @Test
    @DisplayName("Should handle concurrent requests without race condition")
    void shouldHandleConcurrentRequestsWithoutRaceCondition()
            throws InterruptedException {

        int threadCount   = 20;
        Thread[] threads  = new Thread[threadCount];
        boolean[] results = new boolean[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    String ip = index % 2 == 0
                            ? "8.8.8.8"
                            : "81.2.69.142";
                    geoIpService.getCountryCode(ip);
                    geoIpService.getLocation(ip);
                    results[index] = true;
                } catch (Exception ex) {
                    results[index] = false;
                }
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        for (boolean result : results) {
            assertThat(result).isTrue();
        }
    }
}
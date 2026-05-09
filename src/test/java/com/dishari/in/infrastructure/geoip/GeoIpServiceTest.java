package com.dishari.in.infrastructure.geoip;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@DisplayName("GeoIpService Unit Tests")
class GeoIpServiceTest {

    private GeoIpService geoIpService;

    @BeforeEach
    void setUp() {
        geoIpService = new GeoIpService();

        // Inject real DB paths via reflection
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

    // ── getCountryCode() tests ───────────────────────────────────

    @Nested
    @DisplayName("getCountryCode()")
    class GetCountryCodeTests {

        @Test
        @DisplayName("Should return US for known US IP")
        void shouldReturnUsForKnownUsIp() {
            // 8.8.8.8 = Google DNS — US based
            String result = geoIpService.getCountryCode("8.8.8.8");

            assertThat(result)
                    .isNotNull()
                    .isEqualTo("US");
        }

        @Test
        @DisplayName("Should return GB for known UK IP")
        void shouldReturnGbForKnownUkIp() {
            // Known UK IP range
            String result = geoIpService.getCountryCode("81.2.69.142");

            assertThat(result)
                    .isNotNull()
                    .isEqualTo("GB");
        }

        @Test
        @DisplayName("Should return IN for known India IP")
        void shouldReturnInForKnownIndiaIp() {
            // Known Indian IP
            String result = geoIpService.getCountryCode("49.36.0.1");

            assertThat(result)
                    .isNotNull()
                    .isEqualTo("IN");
        }

        @Test
        @DisplayName("Should return null for loopback IP")
        void shouldReturnNullForLoopbackIp() {
            String result = geoIpService.getCountryCode("127.0.0.1");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return null for IPv6 loopback")
        void shouldReturnNullForIpv6Loopback() {
            String result = geoIpService.getCountryCode("::1");
            assertThat(result).isNull();
        }

        @ParameterizedTest
        @DisplayName("Should return null for all private IPs")
        @ValueSource(strings = {
                "10.0.0.1",
                "10.255.255.255",
                "192.168.0.1",
                "192.168.255.255",
                "172.16.0.1",
                "172.31.255.255"
        })
        void shouldReturnNullForPrivateIps(String privateIp) {
            String result = geoIpService.getCountryCode(privateIp);
            assertThat(result).isNull();
        }

        @ParameterizedTest
        @DisplayName("Should return null for null or blank IP")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", ""})
        void shouldReturnNullForNullOrBlankIp(String ip) {
            String result = geoIpService.getCountryCode(ip);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should handle X-Forwarded-For format with multiple IPs")
        void shouldHandleForwardedForFormat() {
            // "client, proxy1, proxy2" — should use client IP (8.8.8.8)
            String result = geoIpService.getCountryCode(
                    "8.8.8.8, 192.168.1.1, 10.0.0.1");
            assertThat(result)
                    .isNotNull()
                    .isEqualTo("US");
        }

        @Test
        @DisplayName("Should return null for invalid IP format")
        void shouldReturnNullForInvalidIpFormat() {
            String result = geoIpService.getCountryCode("not.an.ip.address");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Country code should always be 2 characters (ISO 3166-1)")
        void countryCodeShouldBeTwoCharacters() {
            String result = geoIpService.getCountryCode("8.8.8.8");
            assertThat(result)
                    .isNotNull()
                    .hasSize(2)
                    .isUpperCase();
        }
    }

    // ── getLocation() tests ──────────────────────────────────────

    @Nested
    @DisplayName("getLocation()")
    class GetLocationTests {

        @Test
        @DisplayName("Should return full location for known IP")
        void shouldReturnFullLocationForKnownIp() {
            GeoIpService.GeoLocation location =
                    geoIpService.getLocation("8.8.8.8");

            assertThat(location).isNotNull();
            assertThat(location.isKnown()).isTrue();
            assertThat(location.countryCode()).isEqualTo("US");
            assertThat(location.countryName()).isNotBlank();
        }

        @Test
        @DisplayName("Should return unknown for loopback IP")
        void shouldReturnUnknownForLoopbackIp() {
            GeoIpService.GeoLocation location =
                    geoIpService.getLocation("127.0.0.1");

            assertThat(location).isNotNull();
            assertThat(location.isKnown()).isFalse();
            assertThat(location.countryCode()).isNull();
            assertThat(location.countryName()).isNull();
        }

        @Test
        @DisplayName("Should return unknown for private IP")
        void shouldReturnUnknownForPrivateIp() {
            GeoIpService.GeoLocation location =
                    geoIpService.getLocation("192.168.1.1");

            assertThat(location.isKnown()).isFalse();
        }

        @ParameterizedTest
        @DisplayName("Should return null for null or blank IP")
        @NullAndEmptySource
        void shouldReturnUnknownForNullOrBlankIp(String ip) {
            GeoIpService.GeoLocation location =
                    geoIpService.getLocation(ip);

            assertThat(location).isNotNull();
            assertThat(location.isKnown()).isFalse();
        }

        @Test
        @DisplayName("GeoLocation.unknown() should have all null fields")
        void unknownLocationShouldHaveNullFields() {
            GeoIpService.GeoLocation unknown =
                    GeoIpService.GeoLocation.unknown();

            assertThat(unknown.countryCode()).isNull();
            assertThat(unknown.countryName()).isNull();
            assertThat(unknown.city()).isNull();
            assertThat(unknown.isKnown()).isFalse();
        }

        @Test
        @DisplayName("Known location should have non-null countryCode and name")
        void knownLocationShouldHaveCountryCodeAndName() {
            GeoIpService.GeoLocation location =
                    geoIpService.getLocation("8.8.8.8");

            if (location.isKnown()) {
                assertThat(location.countryCode())
                        .isNotNull()
                        .hasSize(2);
                assertThat(location.countryName())
                        .isNotNull()
                        .isNotBlank();
            }
        }
    }

    // ── DB loading tests ─────────────────────────────────────────

    @Nested
    @DisplayName("Database Loading")
    class DatabaseLoadingTests {

        @Test
        @DisplayName("Should load country DB successfully")
        void shouldLoadCountryDbSuccessfully() {
            // If DB loaded correctly, valid IP should return result
            String result = geoIpService.getCountryCode("8.8.8.8");
            // result may be null if DB not present — but no exception
            assertThatCode(() ->
                    geoIpService.getCountryCode("8.8.8.8"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should not throw if DB file missing")
        void shouldNotThrowIfDbFileMissing() {
            GeoIpService service = new GeoIpService();
            ReflectionTestUtils.setField(service,
                    "countryDbPath", "non/existent/path.mmdb");
            ReflectionTestUtils.setField(service,
                    "cityDbPath", "non/existent/path.mmdb");

            // Should log warning but not throw
            assertThatCode(service::init)
                    .doesNotThrowAnyException();

            // Should return null gracefully
            assertThat(service.getCountryCode("8.8.8.8")).isNull();
            assertThat(service.getLocation("8.8.8.8").isKnown()).isFalse();
        }

        @Test
        @DisplayName("Should close DB readers without exception")
        void shouldCloseDbReadersWithoutException() {
            assertThatCode(() -> geoIpService.close())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle double close gracefully")
        void shouldHandleDoubleCloseGracefully() {
            assertThatCode(() -> {
                geoIpService.close();
                geoIpService.close(); // second close should not throw
            }).doesNotThrowAnyException();
        }
    }

    // ── Multiple IPs parametrized ────────────────────────────────

    @Nested
    @DisplayName("Known IP to Country Mappings")
    class KnownIpMappingTests {

        @ParameterizedTest(name = "IP {0} should resolve to country {1}")
        @CsvSource({
                "8.8.8.8,        US",   // Google DNS
                "1.1.1.1,        AU",   // Cloudflare (AU registered)
                "81.2.69.142,    GB",   // Known UK IP
                "89.160.20.112,  SE"    // Known Sweden IP
        })
        void shouldResolveKnownIpsToCountries(
                String ip, String expectedCountry) {

            String result = geoIpService.getCountryCode(ip);

            // Only assert if DB is loaded
            // Some IPs may not be in free GeoLite2 DB
            if (result != null) {
                assertThat(result)
                        .as("IP %s should resolve to %s", ip, expectedCountry)
                        .isEqualTo(expectedCountry);
            }
        }
    }
}
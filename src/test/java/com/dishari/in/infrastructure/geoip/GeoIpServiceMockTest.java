package com.dishari.in.infrastructure.geoip;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.model.CountryResponse;
import com.maxmind.geoip2.record.Country;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetAddress;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GeoIpService Mock Tests")
class GeoIpServiceMockTest {

    @InjectMocks
    private GeoIpService geoIpService;

    @Mock
    private DatabaseReader countryReader;

    @Mock
    private DatabaseReader cityReader;

    @BeforeEach
    void setUp() {
        // Inject mocked readers directly
        ReflectionTestUtils.setField(
                geoIpService, "countryReader", countryReader);
        ReflectionTestUtils.setField(
                geoIpService, "cityReader", cityReader);
    }

    @Test
    @DisplayName("Should return country code from mocked reader")
    void shouldReturnCountryCodeFromMockedReader() throws Exception {
        // ── Arrange ──────────────────────────────────────────────
        CountryResponse mockResponse = mock(CountryResponse.class);
        Country mockCountry          = mock(Country.class);

        when(mockCountry.getIsoCode()).thenReturn("US");
        when(mockResponse.getCountry()).thenReturn(mockCountry);
        when(countryReader.country(any(InetAddress.class)))
                .thenReturn(mockResponse);

        // ── Act ───────────────────────────────────────────────────
        String result = geoIpService.getCountryCode("8.8.8.8");

        // ── Assert ───────────────────────────────────────────────
        assertThat(result).isEqualTo("US");
        verify(countryReader, times(1))
                .country(any(InetAddress.class));
    }

    @Test
    @DisplayName("Should return null when AddressNotFoundException thrown")
    void shouldReturnNullWhenAddressNotFound() throws Exception {
        // ── Arrange ──────────────────────────────────────────────
        when(countryReader.country(any(InetAddress.class)))
                .thenThrow(new AddressNotFoundException("Not found"));

        // ── Act ───────────────────────────────────────────────────
        String result = geoIpService.getCountryCode("8.8.8.8");

        // ── Assert ───────────────────────────────────────────────
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return null when IOException thrown")
    void shouldReturnNullWhenIoExceptionThrown() throws Exception {
        // ── Arrange ──────────────────────────────────────────────
        when(countryReader.country(any(InetAddress.class)))
                .thenThrow(new IOException("DB read error"));

        // ── Act ───────────────────────────────────────────────────
        String result = geoIpService.getCountryCode("8.8.8.8");

        // ── Assert — should not propagate exception ───────────────
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should never call reader for private IP")
    void shouldNeverCallReaderForPrivateIp() throws Exception {
        geoIpService.getCountryCode("192.168.1.1");
        verify(countryReader, never()).country(any());
    }

    @Test
    @DisplayName("Should never call reader for loopback IP")
    void shouldNeverCallReaderForLoopbackIp() throws Exception {
        geoIpService.getCountryCode("127.0.0.1");
        verify(countryReader, never()).country(any());
    }

    @Test
    @DisplayName("Should never call reader for null IP")
    void shouldNeverCallReaderForNullIp() throws Exception {
        geoIpService.getCountryCode(null);
        verify(countryReader, never()).country(any());
    }

    @Test
    @DisplayName("Should extract first IP from X-Forwarded-For")
    void shouldExtractFirstIpFromForwardedFor() throws Exception {
        // ── Arrange ──────────────────────────────────────────────
        CountryResponse mockResponse = mock(CountryResponse.class);
        Country mockCountry          = mock(Country.class);
        when(mockCountry.getIsoCode()).thenReturn("US");
        when(mockResponse.getCountry()).thenReturn(mockCountry);
        when(countryReader.country(any(InetAddress.class)))
                .thenReturn(mockResponse);

        // ── Act — send comma-separated IPs ───────────────────────
        String result = geoIpService.getCountryCode(
                "8.8.8.8, 192.168.1.1, 10.0.0.1");

        // ── Assert — should use 8.8.8.8, not the private IPs ─────
        assertThat(result).isEqualTo("US");
        verify(countryReader, times(1))
                .country(InetAddress.getByName("8.8.8.8"));
    }

    @Test
    @DisplayName("Should return GeoLocation.unknown() when city reader is null")
    void shouldReturnUnknownWhenCityReaderIsNull() {
        ReflectionTestUtils.setField(
                geoIpService, "cityReader", null);

        GeoIpService.GeoLocation result =
                geoIpService.getLocation("8.8.8.8");

        assertThat(result.isKnown()).isFalse();
        assertThat(result).isEqualTo(GeoIpService.GeoLocation.unknown());
    }
}
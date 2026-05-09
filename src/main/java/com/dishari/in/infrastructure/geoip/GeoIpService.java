package com.dishari.in.infrastructure.geoip;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.model.CountryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

@Service
@Slf4j
public class GeoIpService {

    @Value("${app.geoip.country-db-path}")
    private String countryDbPath;

    @Value("${app.geoip.city-db-path}")
    private String cityDbPath;

    // ── Two separate readers ─────────────────────────────────────
    // Country reader — lightweight, used for routing decisions
    private DatabaseReader countryReader;

    // City reader — heavier, used for analytics data
    private DatabaseReader cityReader;

    @PostConstruct
    public void init() {
        countryReader = loadDatabase(countryDbPath,"Country");
        cityReader = loadDatabase(cityDbPath,"City");
    }

    private DatabaseReader loadDatabase(String path, String type) {
        File db = new File(path);
//        InputStream db = getClass().getClassLoader().getResourceAsStream(path);
        if (!db.exists()) {
            log.warn("GeoIP {} DB not found at: {} — {} features disabled",
                    type, path, type.toLowerCase());
            return null;
        }
        try {
            DatabaseReader reader = new DatabaseReader.Builder(db).build();
            log.info("GeoIP {} DB loaded: {}", type, path);
            return reader;
        } catch (IOException ex) {
            log.error("Failed to load GeoIP {} DB: {}",
                    type, ex.getMessage());
            return null;
        }
    }

    // ── Country code only — used by GeoRuleResolver ──────────────
    // Uses Country DB — fast and lightweight
    public String getCountryCode(String ip) {
        if (countryReader == null) return null;

        String cleanIp = cleanIp(ip);
        if (cleanIp == null || isPrivateIp(cleanIp)) return null;

        try {
            InetAddress address    = InetAddress.getByName(cleanIp);
            CountryResponse response = countryReader.country(address);
            String code = response.getCountry().getIsoCode();

            log.debug("GeoIP country: ip={} code={}", cleanIp, code);
            return code;

        } catch (AddressNotFoundException ex) {
            // IP not in database — not an error, just unknown
            log.debug("GeoIP: no country data for ip={}", cleanIp);
            return null;
        } catch (GeoIp2Exception | IOException ex) {
            log.warn("GeoIP country lookup failed for ip={}: {}",
                    cleanIp, ex.getMessage());
            return null;
        }
    }

    // ── Full location — used by ClickEvent analytics ─────────────
    // Uses City DB — more data, used for storing analytics
    public GeoLocation getLocation(String ip) {
        if (cityReader == null) return GeoLocation.unknown();

        String cleanIp = cleanIp(ip);
        if (cleanIp == null || isPrivateIp(cleanIp)) {
            return GeoLocation.unknown();
        }

        try {
            InetAddress address  = InetAddress.getByName(cleanIp);
            CityResponse cityResponse = cityReader.city(address);

            //If the value of city is null then we extract the value of time zone and store it into the db
            String cityName ;
            if (cityResponse.getCity().getName() == null) {
                cityName = cityResponse.getLocation().getTimeZone() ;
            }else{
                cityName = cityResponse.getCity().getName() ;
            }
            GeoLocation location = new GeoLocation(
                    cityResponse.getCountry().getIsoCode(),
                    cityResponse.getCountry().getName(),
                    cityName ,
                    cityResponse.getLocation().getTimeZone()
            );

            log.debug("GeoIP city: ip={} location={}",
                    cleanIp, location);
            return location;

        } catch (AddressNotFoundException ex) {
            log.debug("GeoIP: no city data for ip={}", cleanIp);
            return GeoLocation.unknown();
        } catch (GeoIp2Exception | IOException ex) {
            log.warn("GeoIP city lookup failed for ip={}: {}",
                    cleanIp, ex.getMessage());
            return GeoLocation.unknown();
        }
    }

    // ── Helpers ──────────────────────────────────────────────────

    // Extract real IP from X-Forwarded-For header
    private String cleanIp(String ip) {
        if (ip == null || ip.isBlank()) return null;
        // X-Forwarded-For can be "client, proxy1, proxy2"
        // take the first one — the original client IP
        return ip.split(",")[0].trim();
    }

    private boolean isPrivateIp(String ip) {
        return ip.equals("127.0.0.1")
                || ip.equals("0:0:0:0:0:0:0:1")
                || ip.equals("::1")
                || ip.startsWith("10.")
                || ip.startsWith("192.168.")
                || ip.startsWith("172.16.")
                || ip.startsWith("172.17.")
                || ip.startsWith("172.18.")
                || ip.startsWith("172.19.")
                || ip.startsWith("172.2")
                || ip.startsWith("172.30.")
                || ip.startsWith("172.31.");
    }

    // ── Clean shutdown ────────────────────────────────────────────
    @PreDestroy
    public void close() {
        closeReader(countryReader, "Country");
        closeReader(cityReader,    "City");
    }

    private void closeReader(DatabaseReader reader, String type) {
        if (reader != null) {
            try {
                reader.close();
                log.info("GeoIP {} DB closed", type);
            } catch (IOException ex) {
                log.warn("Failed to close GeoIP {} DB: {}",
                        type, ex.getMessage());
            }
        }
    }

    // ── Result record ────────────────────────────────────────────
    public record GeoLocation(
            String countryCode,
            String countryName,
            String city ,
            String timeZone
    ) {
        public static GeoLocation unknown() {
            return new GeoLocation(null, null, null , null);
        }

        public boolean isKnown() {
            return countryCode != null;
        }
    }
}
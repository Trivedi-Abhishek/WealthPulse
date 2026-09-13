package com.marketdataservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class MarketDataServiceApplication {

    public static void main(String[] args) {
        // Must run before any JDBC connection is opened. The Postgres driver sends the JVM's
        // default zone in its startup packet; a Windows host resolves to the legacy alias
        // "Asia/Calcutta", which the Postgres 14 image does not know (FATAL: invalid value for
        // parameter "TimeZone"). UTC also keeps audit timestamps identical across developer
        // machines and CI. Explicit zones elsewhere - e.g. FinnhubService's America/New_York
        // trading date - are unaffected.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(MarketDataServiceApplication.class, args);
    }

}

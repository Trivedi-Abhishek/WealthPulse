package com.roboadvisorservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class RoboAdvisorServiceApplication {

    public static void main(String[] args) {
        // Must run before any JDBC connection is opened. The Postgres driver sends the JVM's
        // default zone in its startup packet; a Windows host resolves to the legacy alias
        // "Asia/Calcutta", which the Postgres 14 image does not know (FATAL: invalid value for
        // parameter "TimeZone"). UTC also keeps audit timestamps identical across developer
        // machines and CI. Explicit zones elsewhere - e.g. FinnhubService's America/New_York
        // trading date - are unaffected.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(RoboAdvisorServiceApplication.class, args);
    }

}

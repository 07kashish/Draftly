package com.draftly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class DraftlyApplication {

    public static void main(String[] args) {
        // PostgreSQL accepts Asia/Kolkata, while some Windows/Java setups report the legacy Asia/Calcutta alias.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(DraftlyApplication.class, args);
    }
}

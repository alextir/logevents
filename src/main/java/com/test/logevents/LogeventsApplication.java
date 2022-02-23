package com.test.logevents;

import com.test.logevents.service.LogReaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RequiredArgsConstructor
public class LogeventsApplication implements CommandLineRunner {

    private final LogReaderService logReaderService;

    public static void main(String[] args) {
        SpringApplication.run(LogeventsApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        logReaderService.read();
    }
}

package com.test.logevents.service;

import com.test.logevents.model.LogEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class LogReaderServiceITest {
    //integration test

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LogReaderService logReaderService;

    @Test
    void read() {
        assertNumEvent(3);

        await().atMost(7, TimeUnit.SECONDS).until(() -> logReaderService.getConsumerEvents() == 3);

        assertOnEvent("scsmbstgra", 3, "APPLICATION_LOG", "12345", false);

        assertOnEvent("scsmbstgrb", 3, null, null, false);

        assertOnEvent("scsmbstgrc", 8, null, null, true);
    }

    private void assertNumEvent(final int numEvents) {
        final String sql = "SELECT COUNT(*) FROM LOG_EVENTS";
        final int rows = jdbcTemplate.queryForObject(sql, Integer.class);
        assertEquals(numEvents, rows);
    }

    private void assertOnEvent(final String id, final long duration, final String type, final String host, boolean alert) {
        final String sql = "SELECT * FROM LOG_EVENTS WHERE ID = ?";
        final LogEvent logEvent = jdbcTemplate.queryForObject(
                sql, new Object[]{id}, (rs, rowNum) -> {
                    LogEvent le = new LogEvent();
                    le.setId(rs.getString("id"));
                    le.setDuration(rs.getLong("duration"));
                    le.setType(rs.getString("type"));
                    le.setHost(rs.getString("host"));
                    le.setAlert(rs.getBoolean("alert"));
                    return le;
                });

        Assertions.assertAll("asserts on log event",
                () -> assertEquals(duration, logEvent.getDuration()),
                () -> assertEquals(type, logEvent.getType()),
                () -> assertEquals(host, logEvent.getHost()),
                () -> assertEquals(alert, logEvent.getAlert())
        );
    }
}
package com.test.logevents.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class LogEventDTO {
    private String id;
    private State state;
    private String type;
    private String host;
    private Instant timestamp;
}

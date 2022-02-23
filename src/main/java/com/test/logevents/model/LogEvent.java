package com.test.logevents.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "log_events")
@NoArgsConstructor
public class LogEvent {
    @Id
    private String id;
    private Long duration;
    private String type;
    private String host;
    private Boolean alert;
}

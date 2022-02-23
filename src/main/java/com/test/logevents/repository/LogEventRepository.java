package com.test.logevents.repository;

import com.test.logevents.model.LogEvent;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LogEventRepository extends CrudRepository<LogEvent, String> {

    @Modifying
    @Query(
            value =
                    "insert into log_events (id, duration, type, host) values (:id, :duration, :type, :host)",
            nativeQuery = true)
    void insertEvent(@Param("id") String id, @Param("duration") long duration,
                     @Param("type") String type, @Param("host") String host);
}

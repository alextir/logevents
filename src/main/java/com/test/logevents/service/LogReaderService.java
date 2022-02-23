package com.test.logevents.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.logevents.model.LogEventDTO;
import com.test.logevents.repository.LogEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Slf4j
@Service
public class LogReaderService {

    @Value("${logfile.path}")
    private Resource resource;

    private final TaskExecutor logEventsTaskExecutor;

    private final LogEventRepository logEventRepository;

    private final ObjectMapper objectMapper;

    private final PlatformTransactionManager transactionManager;

    private final AtomicInteger consumedEvents = new AtomicInteger(0);

    public void read() throws IOException {
        final Path path = resource.getFile().toPath();
        final Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8);
        lines.map(this::convert)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(logEventDTO -> logEventsTaskExecutor.execute(process(logEventDTO)));
    }

    private Optional<LogEventDTO> convert(final String line) {
        try {
            return Optional.of(objectMapper.readValue(line, LogEventDTO.class));
        } catch (JsonProcessingException e) {
            log.error("cannot parse log line [{}]", line, e);
        }
        return Optional.empty();
    }

    private Runnable process(final LogEventDTO logEventDTO) {
        return () -> {
        /*
            Method steps:
            1. try to insert start or finished, which comes first; insert timestamp into duration
            2. on integrity constraint violation it means a STARTED/FINISHED already inserted before a FINISHED/STARTED for that id
                - update existing id with missing info if comes into latest peer event (duration as abs(t1-t2), alert, etc)

            Obs:
            Other possible solutions:
            - selecting first  to see if id already existd & insert/update after is not 100 correct (eg when START/FINISHED processed
            simultaneously by 2 parallel threads one can overwrite other)
            - keeping the event in memory and updating the state finis/start in memory; after that update db in batches more performant; drawback
            here is half of the file can stay in memory worst case (in case files is good not memory wise)
                eg of ids ordered like that in input file:  start(A B C) finish(C B A) keep half of event in memory
            - using a broker eg kafka and keep events in topic & update a state store
         */

            final TransactionTemplate transactionTemplateInsert = new TransactionTemplate(transactionManager);
            boolean inserted = false;
            try {
                inserted = transactionTemplateInsert.execute(status -> insert(logEventDTO));
            } catch (Exception e) {
                // to cont exec
            }

            if (!inserted) {
                // id already was inserted, cannot be processes in a different thread
                final TransactionTemplate transactionTemplateUpdate = new TransactionTemplate(transactionManager);
                transactionTemplateUpdate.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        update(logEventDTO);
                    }
                });
            }
        };
    }

    private boolean insert(final LogEventDTO logEventDTO) {
        try {
            logEventRepository.insertEvent(logEventDTO.getId(), logEventDTO.getTimestamp().toEpochMilli(), logEventDTO.getType(), logEventDTO.getHost());
            log.info("event inserted: [{}]", logEventDTO);
            return true;
        } catch (DataIntegrityViolationException dive) {
            return false;
        }
        //todo: proper exception handling, consider now only pk violation can happen
    }

    private void update(LogEventDTO logEventDTO) {
        // id already exists from a STARTED or FINISHED event
        logEventRepository.findById(logEventDTO.getId()).ifPresent(le -> {
            final long duration = Math.abs(le.getDuration() - logEventDTO.getTimestamp().toEpochMilli()) / 1000;
            le.setAlert(duration > 4);
            le.setDuration(duration);
            if (logEventDTO.getType() != null) {
                le.setType(logEventDTO.getType());
            }
            if (logEventDTO.getHost() != null) {
                le.setHost(logEventDTO.getHost());
            }
            logEventRepository.save(le);
            log.info("event updated: [{}]", le);

            consumedEvents.incrementAndGet();
        });
    }

    public int getConsumerEvents() {
        return consumedEvents.get();
    }
}

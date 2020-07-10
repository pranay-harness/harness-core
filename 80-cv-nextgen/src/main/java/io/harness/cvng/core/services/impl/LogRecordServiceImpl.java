package io.harness.cvng.core.services.impl;

import com.google.inject.Inject;

import io.harness.cvng.beans.LogRecordDTO;
import io.harness.cvng.core.entities.LogRecord;
import io.harness.cvng.core.services.api.LogRecordService;
import io.harness.persistence.HPersistence;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class LogRecordServiceImpl implements LogRecordService {
  @Inject private HPersistence hPersistence;

  public void save(List<LogRecordDTO> logRecords) {
    saveRecords(logRecords.stream().map(this ::toLogRecord).collect(Collectors.toList()));
  }

  private void saveRecords(List<LogRecord> logRecords) {
    hPersistence.save(logRecords);
  }
  private LogRecord toLogRecord(LogRecordDTO logRecordDTO) {
    return LogRecord.builder()
        .accountId(logRecordDTO.getAccountId())
        .cvConfigId(logRecordDTO.getCvConfigId())
        .host(logRecordDTO.getHost())
        .log(logRecordDTO.getLog())
        .timestamp(Instant.ofEpochMilli(logRecordDTO.getTimestamp()))
        .build();
  }
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.impl;

import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.cvng.beans.LogRecordDTO;
import io.harness.cvng.core.entities.LogRecord;
import io.harness.cvng.core.entities.LogRecord.LogRecordKeys;
import io.harness.cvng.core.services.api.LogRecordService;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class LogRecordServiceImpl implements LogRecordService {
  @Inject private HPersistence hPersistence;
  @Override
  public void save(List<LogRecordDTO> logRecords) {
    saveRecords(logRecords.stream().map(this::toLogRecord).collect(Collectors.toList()));
  }

  @Override
  public List<LogRecord> getLogRecords(String verificationTaskId, Instant startTime, Instant endTime) {
    return hPersistence.createQuery(LogRecord.class, excludeAuthority)
        .filter(LogRecordKeys.verificationTaskId, verificationTaskId)
        .field(LogRecordKeys.timestamp)
        .greaterThanOrEq(startTime)
        .field(LogRecordKeys.timestamp)
        .lessThan(endTime)
        .asList();
  }

  private void saveRecords(List<LogRecord> logRecords) {
    hPersistence.save(logRecords);
  }
  private LogRecord toLogRecord(LogRecordDTO logRecordDTO) {
    return LogRecord.builder()
        .accountId(logRecordDTO.getAccountId())
        .verificationTaskId(logRecordDTO.getVerificationTaskId())
        .host(logRecordDTO.getHost())
        .log(logRecordDTO.getLog())
        .timestamp(Instant.ofEpochMilli(logRecordDTO.getTimestamp()))
        .build();
  }
}

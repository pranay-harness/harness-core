package io.harness.cvng.core.services.impl;

import com.google.inject.Inject;

import io.harness.cvng.core.entities.LogRecord;
import io.harness.cvng.core.services.api.LogRecordService;
import io.harness.persistence.HPersistence;

import java.util.List;

public class LogRecordServiceImpl implements LogRecordService {
  @Inject private HPersistence hPersistence;
  @Override
  public void save(List<LogRecord> logRecords) {
    hPersistence.save(logRecords);
  }
}

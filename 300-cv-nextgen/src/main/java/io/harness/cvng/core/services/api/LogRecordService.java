/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.LogRecordDTO;
import io.harness.cvng.core.entities.LogRecord;

import java.time.Instant;
import java.util.List;

public interface LogRecordService {
  void save(List<LogRecordDTO> logRecords);

  /**
   * Start time inclusive and endTime exclusive.
   */
  List<LogRecord> getLogRecords(String verificationTaskId, Instant startTime, Instant endTime);
}

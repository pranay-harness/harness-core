/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.timeseries.processor.instanceeventprocessor.instancereconservice;

import io.harness.exception.WingsException;

public interface IInstanceReconService {
  void doDataMigration(String accountId, Integer dataMigrationIntervalInHours) throws Exception;
  void aggregateEventsForGivenInterval(String accountId, Long intervalStartTime, Long intervalEndTime,
      Integer batchSize, Integer rowLimit) throws WingsException;
}

package io.harness.event.timeseries.processor.instanceeventprocessor.instancereconservice;

import io.harness.exception.WingsException;

public interface IInstanceReconService {
  void doDataMigration(String accountId, Integer dataMigrationIntervalInHours) throws Exception;
  void aggregateEventsForGivenInterval(String accountId, Long intervalStartTime, Long intervalEndTime,
      Integer batchSize, Integer rowLimit) throws WingsException;
}

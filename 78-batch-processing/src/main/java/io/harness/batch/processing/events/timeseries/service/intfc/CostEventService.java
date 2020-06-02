package io.harness.batch.processing.events.timeseries.service.intfc;

import io.harness.batch.processing.events.timeseries.data.CostEventData;

import java.sql.Timestamp;
import java.util.List;

public interface CostEventService {
  boolean create(List<CostEventData> costEventDataList);

  boolean updateDeploymentEvent(CostEventData costEventData);

  // Get latest change timestamp, >= startTimestamp for a given workload
  Timestamp getLastChangeTimestamp(
      String accountId, String clusterId, String instanceId, String costEventType, Timestamp startTimestamp);

  List<CostEventData> getEventsForWorkload(
      String accountId, String clusterId, String instanceId, String costEventType, long startTimeMillis);
}

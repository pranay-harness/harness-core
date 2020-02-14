package io.harness.batch.processing.events.timeseries.service.intfc;

import io.harness.batch.processing.events.timeseries.data.CostEventData;

import java.util.List;

public interface CostEventService {
  boolean create(List<CostEventData> costEventDataList);

  boolean updateDeploymentEvent(CostEventData costEventData);
}

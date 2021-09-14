/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.events.timeseries.service.intfc;

import io.harness.batch.processing.events.timeseries.data.CostEventData;

import java.util.List;

public interface CostEventService {
  boolean create(List<CostEventData> costEventDataList);

  boolean updateDeploymentEvent(CostEventData costEventData);

  List<CostEventData> getEventsForWorkload(
      String accountId, String clusterId, String instanceId, String costEventType, long startTimeMillis);
}

package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.AppdynamicsValidationResponse;
import io.harness.cvng.core.entities.MetricPack;

import java.util.List;
import java.util.Set;

public interface AppDynamicsService {
  Set<AppdynamicsValidationResponse> getMetricPackData(String accountId, String projectIdentifier, String connectorId,
      long appdAppId, long appdTierId, String requestGuid, List<MetricPack> metricPacks);
}

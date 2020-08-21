package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.AppdynamicsValidationResponse;
import io.harness.cvng.beans.appd.AppDynamicsApplication;
import io.harness.cvng.beans.appd.AppDynamicsTier;
import io.harness.cvng.core.entities.MetricPack;

import java.util.List;
import java.util.Set;

public interface AppDynamicsService {
  Set<AppdynamicsValidationResponse> getMetricPackData(String accountId, String connectorId, String orgIdentifier,
      String projectIdentifier, long appdAppId, long appdTierId, String requestGuid, List<MetricPack> metricPacks);

  List<AppDynamicsApplication> getApplications(
      String accountId, String connectorId, String orgIdentifier, String projectIdentifier);

  Set<AppDynamicsTier> getTiers(
      String accountId, String connectorId, String orgIdentifier, String projectIdentifier, long appDynamicsAppId);
}

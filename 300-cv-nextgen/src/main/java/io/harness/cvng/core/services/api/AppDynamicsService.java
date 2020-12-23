package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.AppdynamicsValidationResponse;
import io.harness.cvng.beans.appd.AppDynamicsApplication;
import io.harness.cvng.beans.appd.AppDynamicsTier;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.ng.beans.PageResponse;

import java.util.List;
import java.util.Set;

public interface AppDynamicsService extends MonitoringSourceImportStatusCreator {
  Set<AppdynamicsValidationResponse> getMetricPackData(String accountId, String connectorIdentifier,
      String orgIdentifier, String projectIdentifier, String appName, String tierName, String requestGuid,
      List<MetricPack> metricPacks);

  PageResponse<AppDynamicsApplication> getApplications(String accountId, String connectorIdentifier,
      String orgIdentifier, String projectIdentifier, int offset, int pageSize, String filter);

  PageResponse<AppDynamicsTier> getTiers(String accountId, String connectorIdentifier, String orgIdentifier,
      String projectIdentifier, String appName, int offset, int pageSize, String filter);
}

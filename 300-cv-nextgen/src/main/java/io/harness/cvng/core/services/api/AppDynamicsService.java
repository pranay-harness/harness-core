/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.api;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.AppdynamicsValidationResponse;
import io.harness.cvng.beans.MetricPackDTO;
import io.harness.cvng.beans.appd.AppDynamicsApplication;
import io.harness.cvng.beans.appd.AppDynamicsTier;
import io.harness.ng.beans.PageResponse;

import java.util.List;
import java.util.Set;

@OwnedBy(CV)
public interface AppDynamicsService extends MonitoringSourceImportStatusCreator, DataSourceConnectivityChecker {
  Set<AppdynamicsValidationResponse> getMetricPackData(String accountId, String connectorIdentifier,
      String orgIdentifier, String projectIdentifier, String appName, String tierName, String requestGuid,
      List<MetricPackDTO> metricPacks);

  PageResponse<AppDynamicsApplication> getApplications(String accountId, String connectorIdentifier,
      String orgIdentifier, String projectIdentifier, int offset, int pageSize, String filter);

  PageResponse<AppDynamicsTier> getTiers(String accountId, String connectorIdentifier, String orgIdentifier,
      String projectIdentifier, String appName, int offset, int pageSize, String filter);
}

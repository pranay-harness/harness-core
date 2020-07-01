package io.harness.cvng.core.services.impl;

import com.google.inject.Inject;

import io.harness.cvng.beans.AppdynamicsValidationResponse;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.client.RequestExecutor;
import io.harness.cvng.client.VerificationManagerClient;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.services.api.AppDynamicsService;
import io.harness.cvng.core.services.api.MetricPackService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AppDynamicsServiceImpl implements AppDynamicsService {
  @Inject private VerificationManagerClient verificationManagerClient;
  @Inject private RequestExecutor requestExecutor;
  @Inject private MetricPackService metricPackService;
  @Override
  // TODO: We need to find a testing strategy for Retrofit interfaces. The current way of mocking Call is too cumbersome
  public Set<AppdynamicsValidationResponse> getMetricPackData(String accountId, String projectIdentifier,
      String connectorId, long appdAppId, long appdTierId, String requestGuid, List<MetricPack> metricPacks) {
    metricPacks.forEach(metricPack
        -> metricPackService.populatePaths(accountId, projectIdentifier, DataSourceType.APP_DYNAMICS, metricPack));
    return requestExecutor
        .execute(
            verificationManagerClient.getAppDynamicsMetricData(accountId, projectIdentifier, connectorId, appdAppId,
                appdTierId, requestGuid, metricPacks.stream().map(MetricPack::getDTO).collect(Collectors.toList())))
        .getResource();
  }
}

package io.harness.cvng.core.services.impl;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.cvng.beans.AppdynamicsValidationResponse;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.MetricPackDTO;
import io.harness.cvng.beans.appd.AppDynamicsApplication;
import io.harness.cvng.beans.appd.AppDynamicsTier;
import io.harness.cvng.beans.appd.AppdynamicsMetricPackDataValidationRequest;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.client.RequestExecutor;
import io.harness.cvng.client.VerificationManagerClient;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.services.api.AppDynamicsService;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AppDynamicsServiceImpl implements AppDynamicsService {
  @Inject private VerificationManagerClient verificationManagerClient;
  @Inject private RequestExecutor requestExecutor;
  @Inject private MetricPackService metricPackService;
  @Inject private NextGenService nextGenService;
  @Override
  // TODO: We need to find a testing strategy for Retrofit interfaces. The current way of mocking Call is too cumbersome
  public Set<AppdynamicsValidationResponse> getMetricPackData(String accountId, String connectorIdentifier,
      String orgIdentifier, String projectIdentifier, long appdAppId, long appdTierId, String requestGuid,
      List<MetricPack> metricPacks) {
    Optional<ConnectorInfoDTO> connectorDTO =
        nextGenService.get(accountId, connectorIdentifier, orgIdentifier, projectIdentifier);
    Preconditions.checkState(connectorDTO.isPresent(), "ConnectorDTO should not be null");
    Preconditions.checkState(connectorDTO.get().getConnectorConfig() instanceof AppDynamicsConnectorDTO,
        "ConnectorConfig should be of type AppDynamics");
    AppDynamicsConnectorDTO appDynamicsConnectorDTO = (AppDynamicsConnectorDTO) connectorDTO.get().getConnectorConfig();
    metricPacks.forEach(metricPack
        -> metricPackService.populatePaths(accountId, projectIdentifier, DataSourceType.APP_DYNAMICS, metricPack));
    List<MetricPackDTO> metricPackDTOS = metricPacks.stream().map(MetricPack::toDTO).collect(Collectors.toList());
    return requestExecutor
        .execute(verificationManagerClient.getAppDynamicsMetricData(accountId, orgIdentifier, projectIdentifier,
            appdAppId, appdTierId, requestGuid,
            AppdynamicsMetricPackDataValidationRequest.builder()
                .connector(appDynamicsConnectorDTO)
                .metricPacks(metricPackDTOS)
                .build()))
        .getResource();
  }

  @Override
  public List<AppDynamicsApplication> getApplications(
      String accountId, String connectorIdentifier, String orgIdentifier, String projectIdentifier) {
    Optional<ConnectorInfoDTO> connectorDTO =
        nextGenService.get(accountId, connectorIdentifier, orgIdentifier, projectIdentifier);
    Preconditions.checkState(connectorDTO.isPresent(), "ConnectorDTO should not be null");
    Preconditions.checkState(connectorDTO.get().getConnectorConfig() instanceof AppDynamicsConnectorDTO,
        "ConnectorConfig should be of type AppDynamics");
    AppDynamicsConnectorDTO appDynamicsConnectorDTO = (AppDynamicsConnectorDTO) connectorDTO.get().getConnectorConfig();
    return requestExecutor
        .execute(verificationManagerClient.getAppDynamicsApplications(
            appDynamicsConnectorDTO.getAccountId(), orgIdentifier, projectIdentifier, appDynamicsConnectorDTO))
        .getResource();
  }

  @Override
  public Set<AppDynamicsTier> getTiers(String accountId, String connectorIdentifier, String orgIdentifier,
      String projectIdentifier, long appDynamicsAppId) {
    Optional<ConnectorInfoDTO> connectorDTO =
        nextGenService.get(accountId, connectorIdentifier, orgIdentifier, projectIdentifier);
    Preconditions.checkState(connectorDTO.isPresent(), "ConnectorDTO should not be null");
    Preconditions.checkState(connectorDTO.get().getConnectorConfig() instanceof AppDynamicsConnectorDTO,
        "ConnectorConfig should be of type AppDynamics");
    AppDynamicsConnectorDTO appDynamicsConnectorDTO = (AppDynamicsConnectorDTO) connectorDTO.get().getConnectorConfig();
    return requestExecutor
        .execute(verificationManagerClient.getTiers(appDynamicsConnectorDTO.getAccountId(), orgIdentifier,
            projectIdentifier, appDynamicsAppId, appDynamicsConnectorDTO))
        .getResource();
  }
}

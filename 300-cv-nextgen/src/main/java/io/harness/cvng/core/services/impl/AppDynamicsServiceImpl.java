package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

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
import io.harness.cvng.core.beans.AppdynamicsImportStatus;
import io.harness.cvng.core.beans.MonitoringSourceImportStatus;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.services.api.AppDynamicsService;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.ng.beans.PageResponse;
import io.harness.utils.PageUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
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
  public PageResponse<AppDynamicsApplication> getApplications(String accountId, String connectorIdentifier,
      String orgIdentifier, String projectIdentifier, int offset, int pageSize, String filter) {
    Optional<ConnectorInfoDTO> connectorDTO =
        nextGenService.get(accountId, connectorIdentifier, orgIdentifier, projectIdentifier);
    Preconditions.checkState(connectorDTO.isPresent(), "ConnectorDTO should not be null");
    Preconditions.checkState(connectorDTO.get().getConnectorConfig() instanceof AppDynamicsConnectorDTO,
        "ConnectorConfig should be of type AppDynamics");
    AppDynamicsConnectorDTO appDynamicsConnectorDTO = (AppDynamicsConnectorDTO) connectorDTO.get().getConnectorConfig();
    List<AppDynamicsApplication> applications =
        requestExecutor
            .execute(verificationManagerClient.getAppDynamicsApplications(
                appDynamicsConnectorDTO.getAccountId(), orgIdentifier, projectIdentifier, appDynamicsConnectorDTO))
            .getResource();
    if (isNotEmpty(filter)) {
      applications = applications.stream()
                         .filter(appDynamicsApplication
                             -> appDynamicsApplication.getName().toLowerCase().contains(filter.trim().toLowerCase()))
                         .collect(Collectors.toList());
    }
    Collections.sort(applications);
    return PageUtils.offsetAndLimit(applications, offset, pageSize);
  }

  @Override
  public PageResponse<AppDynamicsTier> getTiers(String accountId, String connectorIdentifier, String orgIdentifier,
      String projectIdentifier, long appDynamicsAppId, int offset, int pageSize, String filter) {
    Optional<ConnectorInfoDTO> connectorDTO =
        nextGenService.get(accountId, connectorIdentifier, orgIdentifier, projectIdentifier);
    Preconditions.checkState(connectorDTO.isPresent(), "ConnectorDTO should not be null");
    Preconditions.checkState(connectorDTO.get().getConnectorConfig() instanceof AppDynamicsConnectorDTO,
        "ConnectorConfig should be of type AppDynamics");
    AppDynamicsConnectorDTO appDynamicsConnectorDTO = (AppDynamicsConnectorDTO) connectorDTO.get().getConnectorConfig();
    Set<AppDynamicsTier> tiers = requestExecutor
                                     .execute(verificationManagerClient.getTiers(appDynamicsConnectorDTO.getAccountId(),
                                         orgIdentifier, projectIdentifier, appDynamicsAppId, appDynamicsConnectorDTO))
                                     .getResource();
    List<AppDynamicsTier> appDynamicsTiers = new ArrayList<>();
    tiers.forEach(appDynamicsTier -> {
      if (isEmpty(filter) || appDynamicsTier.getName().toLowerCase().contains(filter.trim().toLowerCase())) {
        appDynamicsTiers.add(appDynamicsTier);
      }
    });
    Collections.sort(appDynamicsTiers);
    return PageUtils.offsetAndLimit(appDynamicsTiers, offset, pageSize);
  }

  @Override
  public MonitoringSourceImportStatus createMonitoringSourceImportStatus(
      List<CVConfig> cvConfigsGroupedByMonitoringSource, int totalNumberOfEnvironments) {
    Preconditions.checkState(
        isNotEmpty(cvConfigsGroupedByMonitoringSource), "The cv configs belonging to a monitoring source is empty");
    Set<String> applicationSet = cvConfigsGroupedByMonitoringSource.stream()
                                     .map(config -> ((AppDynamicsCVConfig) config).getApplicationName())
                                     .collect(Collectors.toSet());
    Set<String> envIdentifiersList =
        cvConfigsGroupedByMonitoringSource.stream().map(CVConfig::getEnvIdentifier).collect(Collectors.toSet());
    CVConfig firstCVConfigForReference = cvConfigsGroupedByMonitoringSource.get(0);
    List<AppDynamicsApplication> appDynamicsApplications = getApplications(firstCVConfigForReference.getAccountId(),
        firstCVConfigForReference.getConnectorIdentifier(), firstCVConfigForReference.getOrgIdentifier(),
        firstCVConfigForReference.getProjectIdentifier(), 0, Integer.MAX_VALUE, null)
                                                               .getContent();
    return AppdynamicsImportStatus.builder()
        .numberOfApplications(isNotEmpty(applicationSet) ? applicationSet.size() : 0)
        .numberOfEnvironments(isNotEmpty(envIdentifiersList) ? envIdentifiersList.size() : 0)
        .totalNumberOfApplications(isNotEmpty(appDynamicsApplications) ? appDynamicsApplications.size() : 0)
        .totalNumberOfEnvironments(totalNumberOfEnvironments)
        .build();
  }
}

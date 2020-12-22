package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.core.beans.DSConfig;
import io.harness.cvng.core.beans.DSConfig.CVConfigUpdateResult;
import io.harness.cvng.core.beans.MonitoringSourceDTO;
import io.harness.cvng.core.beans.MonitoringSourceDTO.MonitoringSourceDTOBuilder;
import io.harness.cvng.core.beans.MonitoringSourceImportStatus;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.CVConfigTransformer;
import io.harness.cvng.core.services.api.DSConfigService;
import io.harness.cvng.core.services.api.MonitoringSourceImportStatusCreator;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DSConfigServiceImpl implements DSConfigService {
  @Inject private CVConfigService cvConfigService;
  @Inject private NextGenService nextGenService;
  @Inject private Injector injector;

  @Override
  public List<DSConfig> list(String accountId, String connectorIdentifier, String productName) {
    List<CVConfig> cvConfigs = cvConfigService.list(accountId, connectorIdentifier, productName);
    if (cvConfigs.isEmpty()) {
      return Collections.emptyList();
    }
    DataSourceType dataSourceType = cvConfigs.get(0).getType();
    CVConfigTransformer<? extends CVConfig, ? extends DSConfig> cvConfigTransformer =
        injector.getInstance(Key.get(CVConfigTransformer.class, Names.named(dataSourceType.name())));
    Map<String, List<CVConfig>> groupById =
        cvConfigs.stream().collect(Collectors.groupingBy(CVConfig::getGroupId, Collectors.toList()));
    return groupById.values().stream().map(group -> cvConfigTransformer.transform(group)).collect(Collectors.toList());
  }

  @Override
  public void upsert(DSConfig dsConfig) {
    List<CVConfig> saved = cvConfigService.list(dsConfig.getAccountId(), dsConfig.getConnectorIdentifier(),
        dsConfig.getProductName(), dsConfig.getIdentifier());
    CVConfigUpdateResult cvConfigUpdateResult = dsConfig.getCVConfigUpdateResult(saved);
    cvConfigUpdateResult.getDeleted().forEach(cvConfig -> cvConfigService.delete(cvConfig.getUuid()));
    cvConfigService.update(cvConfigUpdateResult.getUpdated());

    cvConfigService.save(cvConfigUpdateResult.getAdded());
  }

  @Override
  public void delete(String accountId, String connectorIdentifier, String productName, String identifier) {
    cvConfigService.deleteByGroupId(accountId, connectorIdentifier, productName, identifier);
  }

  @Override
  public List<MonitoringSourceDTO> listMonitoringSources(
      String accountId, String orgIdentifier, String projectIdentifier, int limit, int offset) {
    List<String> monitoringSourceIdsForGivenLimitAndOffset =
        cvConfigService.getMonitoringSourceIds(accountId, orgIdentifier, projectIdentifier, limit, offset);
    List<CVConfig> cvConfigs = cvConfigService.listByMonitoringSources(
        accountId, orgIdentifier, projectIdentifier, monitoringSourceIdsForGivenLimitAndOffset);
    List<MonitoringSourceDTO> monitoringSourceDTOS = groupDSConfigsByMonitoringSources(cvConfigs);
    monitoringSourceDTOS.sort(Comparator.comparing(MonitoringSourceDTO::getImportedAt).reversed());
    return monitoringSourceDTOS;
  }

  private List<MonitoringSourceDTO> groupDSConfigsByMonitoringSources(List<CVConfig> cvConfigs) {
    if (isEmpty(cvConfigs)) {
      return Collections.emptyList();
    }
    Map<String, List<CVConfig>> groupByMonitoringSource =
        cvConfigs.stream().collect(Collectors.groupingBy(CVConfig::getMonitoringSourceIdentifier, Collectors.toList()));
    return groupByMonitoringSource.values()
        .stream()
        .map(listOfConfigs -> createMonitoringSourceDTO(listOfConfigs))
        .collect(Collectors.toList());
  }

  private MonitoringSourceDTO createMonitoringSourceDTO(List<CVConfig> cvConfigsGroupedByMonitoringSource) {
    Preconditions.checkState(isNotEmpty(cvConfigsGroupedByMonitoringSource), "The number of configs in the group is 0");
    MonitoringSourceDTOBuilder monitoringSourceDTOBuilder = MonitoringSourceDTO.builder();
    populateCommonFieldsOfMonitoringSource(cvConfigsGroupedByMonitoringSource, monitoringSourceDTOBuilder);
    CVConfig firstCVConfigForReference = cvConfigsGroupedByMonitoringSource.get(0);
    int numberOfEnvironments = nextGenService.getEnvironmentCount(firstCVConfigForReference.getAccountId(),
        firstCVConfigForReference.getOrgIdentifier(), firstCVConfigForReference.getProjectIdentifier());
    monitoringSourceDTOBuilder.importStatus(
        createImportStatus(cvConfigsGroupedByMonitoringSource, numberOfEnvironments));
    return monitoringSourceDTOBuilder.build();
  }

  private void populateCommonFieldsOfMonitoringSource(
      List<CVConfig> cvConfigsGroupedByMonitoringSource, MonitoringSourceDTOBuilder monitoringSourceDTOBuilder) {
    CVConfig firstCVConfig = cvConfigsGroupedByMonitoringSource.get(0);
    monitoringSourceDTOBuilder.monitoringSourceIdentifier(firstCVConfig.getMonitoringSourceIdentifier());
    monitoringSourceDTOBuilder.monitoringSourceName(firstCVConfig.getMonitoringSourceName());
    long importedAtTime = cvConfigsGroupedByMonitoringSource.stream()
                              .min(Comparator.comparing(CVConfig::getCreatedAt))
                              .get()
                              .getCreatedAt();
    Set<String> servicesList =
        cvConfigsGroupedByMonitoringSource.stream().map(CVConfig::getServiceIdentifier).collect(Collectors.toSet());
    monitoringSourceDTOBuilder.importedAt(importedAtTime);
    monitoringSourceDTOBuilder.numberOfServices(isNotEmpty(servicesList) ? servicesList.size() : 0);
    monitoringSourceDTOBuilder.type(firstCVConfig.getType());
  }

  private MonitoringSourceImportStatus createImportStatus(
      List<CVConfig> cvConfigsGroupedByMonitoringSource, int totalNumberOfEnvironments) {
    Preconditions.checkState(isNotEmpty(cvConfigsGroupedByMonitoringSource), "The number of configs in the group is 0");
    CVConfig firstCVConfig = cvConfigsGroupedByMonitoringSource.get(0);
    MonitoringSourceImportStatusCreator monitoringSourceImportStatusCreator = injector.getInstance(
        Key.get(MonitoringSourceImportStatusCreator.class, Names.named(firstCVConfig.getType().name())));
    return monitoringSourceImportStatusCreator.createMonitoringSourceImportStatus(
        cvConfigsGroupedByMonitoringSource, totalNumberOfEnvironments);
  }
}

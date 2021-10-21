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
import io.harness.cvng.core.services.api.MonitoringSourcePerpetualTaskService;
import io.harness.exception.DuplicateFieldException;
import io.harness.ng.beans.PageResponse;
import io.harness.utils.PageUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
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
  @Inject private MonitoringSourcePerpetualTaskService monitoringSourcePerpetualTaskService;

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
        cvConfigs.stream().collect(Collectors.groupingBy(CVConfig::getIdentifier, Collectors.toList()));
    return groupById.values().stream().map(group -> cvConfigTransformer.transform(group)).collect(Collectors.toList());
  }

  @Override
  public void create(DSConfig dsConfig) {
    List<CVConfig> existingMapping = cvConfigService.getExistingMappedConfigs(dsConfig.getAccountId(),
        dsConfig.getOrgIdentifier(), dsConfig.getProjectIdentifier(), dsConfig.getIdentifier(), dsConfig.getType());
    dsConfig.validate(existingMapping);
    List<CVConfig> saved = cvConfigService.list(dsConfig.getAccountId(), dsConfig.getOrgIdentifier(),
        dsConfig.getProjectIdentifier(), dsConfig.getIdentifier());
    if (saved != null && saved.size() > 0) {
      throw new DuplicateFieldException(
          String.format("DSConfig  with identifier %s and orgIdentifier %s and projectIdentifier %s is already present",
              dsConfig.getIdentifier(), dsConfig.getOrgIdentifier(), dsConfig.getProjectIdentifier()));
    }
    CVConfigUpdateResult cvConfigUpdateResult = dsConfig.getCVConfigUpdateResult(saved);
    cvConfigService.save(cvConfigUpdateResult.getAdded());
    monitoringSourcePerpetualTaskService.createTask(dsConfig.getAccountId(), dsConfig.getOrgIdentifier(),
        dsConfig.getProjectIdentifier(), dsConfig.getConnectorIdentifier(), dsConfig.getIdentifier(), false);
  }

  @Override
  public void update(String identifier, DSConfig dsConfig) {
    List<CVConfig> existingMapping = cvConfigService.getExistingMappedConfigs(dsConfig.getAccountId(),
        dsConfig.getOrgIdentifier(), dsConfig.getProjectIdentifier(), identifier, dsConfig.getType());
    dsConfig.validate(existingMapping);
    List<CVConfig> saved = cvConfigService.list(
        dsConfig.getAccountId(), dsConfig.getOrgIdentifier(), dsConfig.getProjectIdentifier(), identifier);
    CVConfigUpdateResult cvConfigUpdateResult = dsConfig.getCVConfigUpdateResult(saved);
    cvConfigUpdateResult.getDeleted().forEach(cvConfig -> cvConfigService.delete(cvConfig.getUuid()));
    cvConfigService.update(cvConfigUpdateResult.getUpdated());
    cvConfigService.save(cvConfigUpdateResult.getAdded());
  }

  @Override
  public void delete(
      String accountId, String orgIdentifier, String projectIdentifier, String monitoringSourceIdentifier) {
    monitoringSourcePerpetualTaskService.deleteTask(
        accountId, orgIdentifier, projectIdentifier, monitoringSourceIdentifier);
    cvConfigService.deleteByIdentifier(accountId, orgIdentifier, projectIdentifier, monitoringSourceIdentifier);
  }

  @Override
  public DSConfig getMonitoringSource(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    List<CVConfig> cvConfigs = cvConfigService.listByMonitoringSources(
        accountId, orgIdentifier, projectIdentifier, Lists.newArrayList(identifier));
    Preconditions.checkState(isNotEmpty(cvConfigs), "No configurations found with identifier %s", identifier);

    Map<String, List<CVConfig>> groupById =
        cvConfigs.stream().collect(Collectors.groupingBy(CVConfig::getIdentifier, Collectors.toList()));
    Preconditions.checkState(groupById.size() == 1, "%s groups found for identifier %s", groupById.size(), identifier);
    DataSourceType dataSourceType = cvConfigs.get(0).getType();
    CVConfigTransformer<? extends CVConfig, ? extends DSConfig> cvConfigTransformer =
        injector.getInstance(Key.get(CVConfigTransformer.class, Names.named(dataSourceType.name())));

    List<? extends DSConfig> dsConfigs =
        groupById.values().stream().map(group -> cvConfigTransformer.transform(group)).collect(Collectors.toList());
    Preconditions.checkState(dsConfigs.size() == 1, "%s configs found for identifier %s", dsConfigs.size(), identifier);
    return dsConfigs.get(0);
  }

  @Override
  public PageResponse<MonitoringSourceDTO> listMonitoringSources(
      String accountId, String orgIdentifier, String projectIdentifier, int limit, int offset, String filter) {
    List<String> filteredMonitoringSources =
        cvConfigService.getMonitoringSourceIds(accountId, orgIdentifier, projectIdentifier, filter);
    List<CVConfig> cvConfigs =
        cvConfigService.listByMonitoringSources(accountId, orgIdentifier, projectIdentifier, filteredMonitoringSources);
    List<MonitoringSourceDTO> monitoringSourceDTOS = groupDSConfigsByMonitoringSources(cvConfigs);
    monitoringSourceDTOS.sort(Comparator.comparing(MonitoringSourceDTO::getImportedAt).reversed());
    return PageUtils.offsetAndLimit(monitoringSourceDTOS, offset, limit);
  }

  @Override
  public List<String> getAvailableMonitoringSources(String accountId, String orgIdentifier, String projectIdentifier) {
    return cvConfigService.getMonitoringSourceIds(accountId, orgIdentifier, projectIdentifier, null);
  }

  private List<MonitoringSourceDTO> groupDSConfigsByMonitoringSources(List<CVConfig> cvConfigs) {
    if (isEmpty(cvConfigs)) {
      return Collections.emptyList();
    }
    Map<String, List<CVConfig>> groupByMonitoringSource =
        cvConfigs.stream().collect(Collectors.groupingBy(CVConfig::getIdentifier, Collectors.toList()));
    return groupByMonitoringSource.values()
        .stream()
        .map(listOfConfigs -> createMonitoringSourceDTO(listOfConfigs))
        .collect(Collectors.toList());
  }

  private MonitoringSourceDTO createMonitoringSourceDTO(List<CVConfig> cvConfigsGroupedByMonitoringSource) {
    Preconditions.checkState(isNotEmpty(cvConfigsGroupedByMonitoringSource), "The number of configs in the group is 0");
    MonitoringSourceDTOBuilder monitoringSourceDTOBuilder = MonitoringSourceDTO.builder();
    populateCommonFieldsOfMonitoringSource(cvConfigsGroupedByMonitoringSource, monitoringSourceDTOBuilder);
    return monitoringSourceDTOBuilder.build();
  }

  private void populateCommonFieldsOfMonitoringSource(
      List<CVConfig> cvConfigsGroupedByMonitoringSource, MonitoringSourceDTOBuilder monitoringSourceDTOBuilder) {
    CVConfig firstCVConfig = cvConfigsGroupedByMonitoringSource.get(0);
    monitoringSourceDTOBuilder.monitoringSourceIdentifier(firstCVConfig.getIdentifier());
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

  @Override
  public MonitoringSourceImportStatus getMonitoringSourceImportStatus(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    List<CVConfig> cvConfigs = cvConfigService.listByMonitoringSources(
        accountId, orgIdentifier, projectIdentifier, Lists.newArrayList(identifier));
    Preconditions.checkState(isNotEmpty(cvConfigs), "The number of configs in the group is 0");
    CVConfig firstCVConfigForReference = cvConfigs.get(0);
    int numberOfEnvironments = nextGenService.getEnvironmentCount(firstCVConfigForReference.getAccountId(),
        firstCVConfigForReference.getOrgIdentifier(), firstCVConfigForReference.getProjectIdentifier());
    return createImportStatus(cvConfigs, numberOfEnvironments);
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

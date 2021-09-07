package io.harness.cvng.core.services.impl.monitoredService;

import io.harness.cvng.core.beans.monitoredService.HealthSource;
import io.harness.cvng.core.beans.monitoredService.HealthSource.CVConfigUpdateResult;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.HealthSourceDTO;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.core.services.api.MonitoringSourcePerpetualTaskService;
import io.harness.cvng.core.services.api.monitoredService.HealthSourceService;
import io.harness.exception.DuplicateFieldException;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HealthSourceServiceImpl implements HealthSourceService {
  @Inject Injector injector;
  @Inject private CVConfigService cvConfigService;
  @Inject private MonitoringSourcePerpetualTaskService monitoringSourcePerpetualTaskService;
  @Inject private MetricPackService metricPackService;

  @Override
  public void create(String accountId, String orgIdentifier, String projectIdentifier, String environmentRef,
      String serviceRef, String nameSpaceIdentifier, Set<HealthSource> healthSources, boolean enabled) {
    healthSources.forEach(healthSource -> {
      CVConfigUpdateResult cvConfigUpdateResult = healthSource.getSpec().getCVConfigUpdateResult(accountId,
          orgIdentifier, projectIdentifier, environmentRef, serviceRef,
          HealthSourceService.getNameSpacedIdentifier(nameSpaceIdentifier, healthSource.getIdentifier()),
          healthSource.getName(), Collections.emptyList(), metricPackService);
      cvConfigUpdateResult.getAdded().forEach(cvConfig -> cvConfig.setEnabled(enabled));
      cvConfigService.save(cvConfigUpdateResult.getAdded());
      // Creating MonitoringSourcePerpetualTasks for now irrespective of enable/disable status.
      // We need to rethink how these tasks are created, batched and managed together.
      monitoringSourcePerpetualTaskService.createTask(accountId, orgIdentifier, projectIdentifier,
          healthSource.getSpec().getConnectorRef(),
          HealthSourceService.getNameSpacedIdentifier(nameSpaceIdentifier, healthSource.getIdentifier()));
    });
  }

  @Override
  public void checkIfAlreadyPresent(String accountId, String orgIdentifier, String projectIdentifier,
      String nameSpaceIdentifier, Set<HealthSource> healthSources) {
    healthSources.forEach(healthSource -> {
      List<CVConfig> saved = cvConfigService.list(accountId, orgIdentifier, projectIdentifier,
          HealthSourceService.getNameSpacedIdentifier(nameSpaceIdentifier, healthSource.getIdentifier()));
      if (saved != null && saved.size() > 0) {
        throw new DuplicateFieldException(String.format(
            "Already Existing configs for Monitored Service  with identifier %s and orgIdentifier %s and projectIdentifier %s",
            HealthSourceService.getNameSpacedIdentifier(nameSpaceIdentifier, healthSource.getIdentifier()),
            orgIdentifier, projectIdentifier));
      }
    });
  }

  @Override
  public Set<HealthSource> get(String accountId, String orgIdentifier, String projectIdentifier,
      String nameSpaceIdentifier, List<String> identifiers) {
    Set<HealthSource> healthSources = new HashSet<>();
    identifiers.forEach(identifier
        -> healthSources.add(
            transformCVConfigs(accountId, orgIdentifier, projectIdentifier, nameSpaceIdentifier, identifier)));
    return healthSources;
  }

  @Override
  public void delete(String accountId, String orgIdentifier, String projectIdentifier, String nameSpaceIdentifier,
      List<String> identifiers) {
    identifiers.forEach(identifier
        -> cvConfigService.deleteByIdentifier(accountId, orgIdentifier, projectIdentifier,
            HealthSourceService.getNameSpacedIdentifier(nameSpaceIdentifier, identifier)));
  }

  @Override
  public void update(String accountId, String orgIdentifier, String projectIdentifier, String environmentRef,
      String serviceRef, String nameSpaceIdentifier, Set<HealthSource> healthSources) {
    healthSources.forEach(healthSource -> {
      List<CVConfig> saved = cvConfigService.list(accountId, orgIdentifier, projectIdentifier,
          HealthSourceService.getNameSpacedIdentifier(nameSpaceIdentifier, healthSource.getIdentifier()));
      CVConfigUpdateResult cvConfigUpdateResult = healthSource.getSpec().getCVConfigUpdateResult(accountId,
          orgIdentifier, projectIdentifier, environmentRef, serviceRef,
          HealthSourceService.getNameSpacedIdentifier(nameSpaceIdentifier, healthSource.getIdentifier()),
          healthSource.getName(), saved, metricPackService);
      cvConfigUpdateResult.getDeleted().forEach(cvConfig -> cvConfigService.delete(cvConfig.getUuid()));
      cvConfigService.update(cvConfigUpdateResult.getUpdated());
      cvConfigService.save(cvConfigUpdateResult.getAdded());
    });
  }

  @Override
  public void setHealthMonitoringFlag(String accountId, String orgIdentifier, String projectIdentifier,
      String namespace, List<String> healthSourceIdentifiers, boolean enable) {
    cvConfigService.setHealthMonitoringFlag(accountId, orgIdentifier, projectIdentifier,
        healthSourceIdentifiers.stream()
            .map(healthSourceIdentifier
                -> HealthSourceService.getNameSpacedIdentifier(namespace, healthSourceIdentifier))
            .collect(Collectors.toList()),
        enable);
  }

  private HealthSource transformCVConfigs(
      String accountId, String orgIdentifier, String projectIdentifier, String nameSpaceIdentifier, String identifier) {
    List<CVConfig> cvConfigs = cvConfigService.list(accountId, orgIdentifier, projectIdentifier,
        HealthSourceService.getNameSpacedIdentifier(nameSpaceIdentifier, identifier));
    Preconditions.checkState(!cvConfigs.isEmpty(),
        String.format("CVConfigs are not present for identifier %s, orgIdentifier %s and projectIdentifier %s",
            HealthSourceService.getNameSpacedIdentifier(nameSpaceIdentifier, identifier), orgIdentifier,
            projectIdentifier));

    HealthSource healthSource = HealthSourceDTO.toHealthSource(cvConfigs.get(0), injector);
    healthSource.setIdentifier(identifier);
    return healthSource;
  }
}
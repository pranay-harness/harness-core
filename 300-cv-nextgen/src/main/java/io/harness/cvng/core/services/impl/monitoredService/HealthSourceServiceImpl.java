package io.harness.cvng.core.services.impl.monitoredService;

import static io.harness.cvng.beans.MonitoredServiceDataSourceType.dataSourceTypeMonitoredServiceDataSourceTypeMap;

import io.harness.cvng.core.beans.monitoredService.HealthSource;
import io.harness.cvng.core.beans.monitoredService.HealthSource.CVConfigUpdateResult;
import io.harness.cvng.core.beans.monitoredService.HealthSourceSpec;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.MonitoringSourcePerpetualTaskService;
import io.harness.cvng.core.services.api.monitoredService.HealthSourceService;
import io.harness.cvng.core.utils.monitoredService.CVConfigToHealthSourceTransformer;
import io.harness.exception.DuplicateFieldException;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HealthSourceServiceImpl implements HealthSourceService {
  @Inject Injector injector;
  @Inject private CVConfigService cvConfigService;
  @Inject private MonitoringSourcePerpetualTaskService monitoringSourcePerpetualTaskService;

  @Override
  public void create(String accountId, String orgIdentifier, String projectIdentifier, String environmentRef,
      String serviceRef, Set<HealthSource> healthSources) {
    healthSources.forEach(healthSourceInfo -> {
      CVConfigUpdateResult cvConfigUpdateResult = healthSourceInfo.getSpec().getCVConfigUpdateResult(accountId,
          orgIdentifier, projectIdentifier, environmentRef, serviceRef, healthSourceInfo.getIdentifier(),
          healthSourceInfo.getName(), Collections.emptyList());
      cvConfigService.save(cvConfigUpdateResult.getAdded());
      monitoringSourcePerpetualTaskService.createTask(accountId, orgIdentifier, projectIdentifier,
          healthSourceInfo.getSpec().getConnectorRef(), healthSourceInfo.getIdentifier());
    });
  }

  @Override
  public void checkIfAlreadyPresent(
      String accountId, String orgIdentifier, String projectIdentifier, Set<HealthSource> healthSources) {
    healthSources.forEach(healthSourceInfo -> {
      List<CVConfig> saved =
          cvConfigService.list(accountId, orgIdentifier, projectIdentifier, healthSourceInfo.getIdentifier());
      if (saved != null && saved.size() > 0) {
        throw new DuplicateFieldException(String.format(
            "Already Existing configs for Monitored Service  with identifier %s and orgIdentifier %s and projectIdentifier %s",
            healthSourceInfo.getIdentifier(), orgIdentifier, projectIdentifier));
      }
    });
  }

  @Override
  public Set<HealthSource> get(
      String accountId, String orgIdentifier, String projectIdentifier, List<String> identifiers) {
    Set<HealthSource> healthSources = new HashSet<>();
    identifiers.forEach(
        identifier -> healthSources.add(transformCVConfigs(accountId, orgIdentifier, projectIdentifier, identifier)));
    return healthSources;
  }

  private HealthSource transformCVConfigs(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    List<CVConfig> cvConfigs = cvConfigService.list(accountId, orgIdentifier, projectIdentifier, identifier);
    Preconditions.checkNotNull(cvConfigs,
        String.format("CVConfigs are not present for identifier %s, orgIdentifier %s and projectIdentifier %s",
            identifier, orgIdentifier, projectIdentifier));

    CVConfigToHealthSourceTransformer<CVConfig, HealthSourceSpec> cvConfigToHealthSourceTransformer =
        injector.getInstance(
            Key.get(CVConfigToHealthSourceTransformer.class, Names.named(cvConfigs.get(0).getType().name())));

    return HealthSource.builder()
        .name(cvConfigs.get(0).getMonitoringSourceName())
        .type(dataSourceTypeMonitoredServiceDataSourceTypeMap.get(cvConfigs.get(0).getType()))
        .identifier(cvConfigs.get(0).getIdentifier())
        .spec(cvConfigToHealthSourceTransformer.transformToHealthSourceConfig(cvConfigs))
        .build();
  }
}
package io.harness.cvng.core.beans.monitoredService.healthSouceSpec;

import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.beans.monitoredService.HealthSource;
import io.harness.cvng.core.beans.monitoredService.MetricPackDTO;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.services.api.MetricPackService;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppDynamicsHealthSourceSpec extends HealthSourceSpec {
  @NotNull String feature;
  @NotEmpty String applicationName;
  @NotEmpty String tierName;
  @NotNull @NotEmpty @Valid Set<MetricPackDTO> metricPacks;

  @Override
  public HealthSource.CVConfigUpdateResult getCVConfigUpdateResult(String accountId, String orgIdentifier,
      String projectIdentifier, String environmentRef, String serviceRef, String identifier, String name,
      List<CVConfig> existingCVConfigs, MetricPackService metricPackService) {
    List<AppDynamicsCVConfig> cvConfigsFromThisObj = toCVConfigs(
        accountId, orgIdentifier, projectIdentifier, environmentRef, serviceRef, identifier, name, metricPackService);
    Map<Key, AppDynamicsCVConfig> existingConfigMap = new HashMap<>();
    List<AppDynamicsCVConfig> existingAppDCVConfig = (List<AppDynamicsCVConfig>) (List<?>) existingCVConfigs;
    for (AppDynamicsCVConfig appDynamicsCVConfig : existingAppDCVConfig) {
      existingConfigMap.put(getKeyFromCVConfig(appDynamicsCVConfig), appDynamicsCVConfig);
    }
    Map<Key, AppDynamicsCVConfig> currentCVConfigsMap = new HashMap<>();
    for (AppDynamicsCVConfig appDynamicsCVConfig : cvConfigsFromThisObj) {
      currentCVConfigsMap.put(getKeyFromCVConfig(appDynamicsCVConfig), appDynamicsCVConfig);
    }
    Set<Key> deleted = Sets.difference(existingConfigMap.keySet(), currentCVConfigsMap.keySet());
    Set<Key> added = Sets.difference(currentCVConfigsMap.keySet(), existingConfigMap.keySet());
    Set<Key> updated = Sets.intersection(existingConfigMap.keySet(), currentCVConfigsMap.keySet());
    List<CVConfig> updatedConfigs =
        updated.stream().map(key -> currentCVConfigsMap.get(key)).collect(Collectors.toList());
    List<CVConfig> updatedConfigWithUuid =
        updated.stream().map(key -> existingConfigMap.get(key)).collect(Collectors.toList());
    for (int i = 0; i < updatedConfigs.size(); i++) {
      updatedConfigs.get(i).setUuid(updatedConfigWithUuid.get(i).getUuid());
    }
    return HealthSource.CVConfigUpdateResult.builder()
        .deleted(deleted.stream().map(key -> existingConfigMap.get(key)).collect(Collectors.toList()))
        .updated(updatedConfigs)
        .added(added.stream().map(key -> currentCVConfigsMap.get(key)).collect(Collectors.toList()))
        .build();
  }

  @Override
  public DataSourceType getType() {
    return DataSourceType.APP_DYNAMICS;
  }

  private List<AppDynamicsCVConfig> toCVConfigs(String accountId, String orgIdentifier, String projectIdentifier,
      String environmentRef, String serviceRef, String identifier, String name, MetricPackService metricPackService) {
    List<AppDynamicsCVConfig> cvConfigs = new ArrayList<>();
    metricPacks.forEach(metricPack -> {
      AppDynamicsCVConfig appDynamicsCVConfig = AppDynamicsCVConfig.builder()
                                                    .accountId(accountId)
                                                    .orgIdentifier(orgIdentifier)
                                                    .projectIdentifier(projectIdentifier)
                                                    .identifier(identifier)
                                                    .connectorIdentifier(getConnectorRef())
                                                    .monitoringSourceName(name)
                                                    .productName(feature)
                                                    .applicationName(applicationName)
                                                    .tierName(tierName)
                                                    .envIdentifier(environmentRef)
                                                    .serviceIdentifier(serviceRef)
                                                    .metricPack(metricPack.toMetricPack(accountId, orgIdentifier,
                                                        projectIdentifier, getType(), metricPackService))
                                                    .category(metricPack.getIdentifier())
                                                    .build();
      cvConfigs.add(appDynamicsCVConfig);
    });
    return cvConfigs;
  }

  private Key getKeyFromCVConfig(AppDynamicsCVConfig appDynamicsCVConfig) {
    return Key.builder()
        .appName(appDynamicsCVConfig.getApplicationName())
        .metricPack(appDynamicsCVConfig.getMetricPack())
        .serviceIdentifier(appDynamicsCVConfig.getServiceIdentifier())
        .tierName(appDynamicsCVConfig.getTierName())
        .build();
  }

  @Value
  @Builder
  private static class Key {
    String appName;
    String tierName;
    String serviceIdentifier;
    MetricPack metricPack;
  }
}

package io.harness.cvng.core.beans.monitoredService;

import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MetricPack;

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
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppDynamicsHealthSourceSpec extends HealthSourceSpec {
  @NotNull String connectorRef;
  @NotNull String feature;
  @NotNull String appdApplicationName;
  @NotNull String appdTierName;
  @NotNull @NotEmpty @Valid Set<MetricPackDTO> metricPacks;

  @Override
  public HealthSource.CVConfigUpdateResult getCVConfigUpdateResult(String accountId, String orgIdentifier,
      String projectIdentifier, String environmentRef, String serviceRef, String identifier, String name,
      List<CVConfig> existingCVConfigs) {
    List<AppDynamicsCVConfig> cvConfigsFromThisObj =
        toCVConfigs(accountId, orgIdentifier, projectIdentifier, environmentRef, serviceRef, identifier, name);
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

  private List<AppDynamicsCVConfig> toCVConfigs(String accountId, String orgIdentifier, String projectIdentifier,
      String environmentRef, String serviceRef, String identifier, String name) {
    List<AppDynamicsCVConfig> cvConfigs = new ArrayList<>();
    metricPacks.forEach(metricPack -> {
      AppDynamicsCVConfig appDynamicsCVConfig = new AppDynamicsCVConfig();
      fillCommonFields(
          appDynamicsCVConfig, accountId, orgIdentifier, projectIdentifier, identifier, connectorRef, name, feature);
      appDynamicsCVConfig.setApplicationName(appdApplicationName);
      appDynamicsCVConfig.setEnvIdentifier(environmentRef);
      appDynamicsCVConfig.setTierName(appdTierName);
      appDynamicsCVConfig.setServiceIdentifier(serviceRef);
      appDynamicsCVConfig.setMetricPack(
          metricPack.toMetricPack(accountId, orgIdentifier, projectIdentifier, metricPack));
      appDynamicsCVConfig.setCategory(metricPack.getCategory());
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

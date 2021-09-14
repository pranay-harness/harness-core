/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans;

import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MetricPack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Value;

@Data
@JsonTypeName("APP_DYNAMICS")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppDynamicsDSConfig extends DSConfig {
  List<AppdynamicsAppConfig> appConfigs;

  @Override
  public DataSourceType getType() {
    return DataSourceType.APP_DYNAMICS;
  }

  @Override
  public CVConfigUpdateResult getCVConfigUpdateResult(List<CVConfig> existingCVConfigs) {
    List<AppDynamicsCVConfig> cvConfigsFromThisObj = toCVConfigs();
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
    return CVConfigUpdateResult.builder()
        .deleted(deleted.stream().map(key -> existingConfigMap.get(key)).collect(Collectors.toList()))
        .updated(updatedConfigs)
        .added(added.stream().map(key -> currentCVConfigsMap.get(key)).collect(Collectors.toList()))
        .build();
  }

  @Override
  public void validate(List<CVConfig> existingMapping) {
    existingMapping.forEach(appDynamicsCVConfig
        -> getAppConfigs().forEach(
            appdynamicsConfig -> appdynamicsConfig.getServiceMappings().forEach(serviceMapping -> {
              String applicationName = ((AppDynamicsCVConfig) appDynamicsCVConfig).getApplicationName();
              String tierName = ((AppDynamicsCVConfig) appDynamicsCVConfig).getTierName();
              Preconditions.checkState(
                  !(appDynamicsCVConfig.getEnvIdentifier().equals(appdynamicsConfig.getEnvIdentifier())
                      && applicationName.equals(appdynamicsConfig.getApplicationName())
                      && tierName.equals(serviceMapping.getTierName())),
                  "appdynamics app/tier %s/%s has already been onboarded for env %s and service %s", applicationName,
                  tierName, appDynamicsCVConfig.getEnvIdentifier(), tierName);
            })));
  }

  @Data
  @Builder
  public static class AppdynamicsAppConfig {
    String applicationName;
    String envIdentifier;
    Set<MetricPack> metricPacks;
    Set<ServiceMapping> serviceMappings;
  }

  @Value
  @Builder
  public static class ServiceMapping {
    String serviceIdentifier;
    String tierName;
  }

  private List<AppDynamicsCVConfig> toCVConfigs() {
    List<AppDynamicsCVConfig> cvConfigs = new ArrayList<>();
    appConfigs.forEach(
        appConfig -> appConfig.serviceMappings.forEach(serviceMapping -> appConfig.metricPacks.forEach(metricPack -> {
          AppDynamicsCVConfig appDynamicsCVConfig = new AppDynamicsCVConfig();
          fillCommonFields(appDynamicsCVConfig);
          appDynamicsCVConfig.setApplicationName(appConfig.applicationName);
          appDynamicsCVConfig.setEnvIdentifier(appConfig.envIdentifier);
          appDynamicsCVConfig.setTierName(serviceMapping.getTierName());
          appDynamicsCVConfig.setServiceIdentifier(serviceMapping.serviceIdentifier);
          appDynamicsCVConfig.setMetricPack(metricPack);
          appDynamicsCVConfig.setCategory(metricPack.getCategory());
          cvConfigs.add(appDynamicsCVConfig);
        })));
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

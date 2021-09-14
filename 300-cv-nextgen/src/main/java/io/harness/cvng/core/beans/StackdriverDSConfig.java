/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans;

import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.StackdriverCVConfig;

import com.fasterxml.jackson.annotation.JsonTypeName;
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
@JsonTypeName("STACKDRIVER")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StackdriverDSConfig extends DSConfig {
  private List<StackdriverConfiguration> metricConfigurations;

  @Override
  public DataSourceType getType() {
    return DataSourceType.STACKDRIVER;
  }

  @Override
  public CVConfigUpdateResult getCVConfigUpdateResult(List<CVConfig> existingCVConfigs) {
    List<StackdriverCVConfig> cvConfigsFromThisObj = toCVConfigs();
    Map<Key, StackdriverCVConfig> existingConfigMap = new HashMap<>();

    List<StackdriverCVConfig> existingSDCVConfigs = (List<StackdriverCVConfig>) (List<?>) existingCVConfigs;

    for (StackdriverCVConfig stackdriverCVConfig : existingSDCVConfigs) {
      existingConfigMap.put(getKeyFromConfig(stackdriverCVConfig), stackdriverCVConfig);
    }

    Map<Key, StackdriverCVConfig> currentCVConfigsMap = new HashMap<>();
    for (StackdriverCVConfig stackdriverCVConfig : cvConfigsFromThisObj) {
      currentCVConfigsMap.put(getKeyFromConfig(stackdriverCVConfig), stackdriverCVConfig);
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
    // do nothing
  }

  private List<StackdriverCVConfig> toCVConfigs() {
    // group things under same service_env_category_dashboard into one config
    Map<Key, List<StackdriverDefinition>> keyToDefinitionMap = new HashMap<>();

    metricConfigurations.forEach(configuration -> {
      Key key = getKeyFromStackdriverConfiguration(configuration);
      if (!keyToDefinitionMap.containsKey(key)) {
        keyToDefinitionMap.put(key, new ArrayList<>());
      }
      keyToDefinitionMap.get(key).add(configuration.getMetricDefinition());
    });

    List<StackdriverCVConfig> cvConfigs = new ArrayList<>();

    keyToDefinitionMap.forEach((key, stackdriverDefinitions) -> {
      StackdriverCVConfig cvConfig = StackdriverCVConfig.builder().build();
      fillCommonFields(cvConfig);
      CVMonitoringCategory category = key.getCategory();
      cvConfig.setEnvIdentifier(key.getEnvIdentifier());
      cvConfig.setServiceIdentifier(key.getServiceIdentifier());
      cvConfig.fromStackdriverDefinitions(stackdriverDefinitions, category);
      cvConfig.setCategory(category);
      cvConfig.setDashboardName(key.getDashboardName());
      cvConfig.setDashboardPath(stackdriverDefinitions.get(0).getDashboardPath());
      cvConfigs.add(cvConfig);
    });

    return cvConfigs;
  }

  private Key getKeyFromConfig(StackdriverCVConfig cvConfig) {
    return Key.builder()
        .envIdentifier(cvConfig.getEnvIdentifier())
        .serviceIdentifier(cvConfig.getServiceIdentifier())
        .category(cvConfig.getMetricPack().getCategory())
        .dashboardName(cvConfig.getDashboardName())
        .build();
  }

  private Key getKeyFromStackdriverConfiguration(StackdriverConfiguration configuration) {
    return Key.builder()
        .envIdentifier(configuration.getEnvIdentifier())
        .dashboardName(configuration.getMetricDefinition().getDashboardName())
        .serviceIdentifier(configuration.getServiceIdentifier())
        .category(configuration.getMetricDefinition().getRiskProfile().getCategory())
        .build();
  }

  @Data
  @Builder
  public static class StackdriverConfiguration {
    private StackdriverDefinition metricDefinition;
    private String serviceIdentifier;
    private String envIdentifier;
  }

  @Value
  @Builder
  private static class Key {
    String envIdentifier;
    String serviceIdentifier;
    CVMonitoringCategory category;
    String dashboardName;
  }
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans;

import static io.harness.cvng.beans.CVMonitoringCategory.ERRORS;
import static io.harness.cvng.beans.DataSourceType.STACKDRIVER_LOG;

import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.stackdriver.StackdriverLogDefinition;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.StackdriverLogCVConfig;

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
@JsonTypeName("STACKDRIVER_LOG")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StackdriverLogDSConfig extends DSConfig {
  List<StackdriverLogConfiguration> logConfigurations;

  @Override
  public DataSourceType getType() {
    return STACKDRIVER_LOG;
  }

  @Override
  public CVConfigUpdateResult getCVConfigUpdateResult(List<CVConfig> existingCVConfigs) {
    Map<Key, StackdriverLogCVConfig> currentCVConfigsMap = toCVConfig();
    Map<Key, StackdriverLogCVConfig> existingConfigMap = new HashMap<>();
    existingCVConfigs.forEach(config -> {
      StackdriverLogCVConfig stackdriverLogCVConfig = (StackdriverLogCVConfig) config;
      existingConfigMap.put(getKeyFromConfig(stackdriverLogCVConfig), stackdriverLogCVConfig);
    });

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
    // Do nothing
  }

  private Map<Key, StackdriverLogCVConfig> toCVConfig() {
    List<StackdriverLogCVConfig> logCVConfigs = new ArrayList<>();
    logConfigurations.forEach(config -> logCVConfigs.add(toCVConfig(config)));
    Map<Key, StackdriverLogCVConfig> configMap = new HashMap<>();
    logCVConfigs.forEach(config -> configMap.put(getKeyFromConfig(config), config));
    return configMap;
  }

  private StackdriverLogCVConfig toCVConfig(StackdriverLogConfiguration logConfiguration) {
    StackdriverLogCVConfig stackdriverLogCVConfig = new StackdriverLogCVConfig();
    fillCommonFields(stackdriverLogCVConfig);
    stackdriverLogCVConfig.setQuery(logConfiguration.getLogDefinition().getQuery());
    stackdriverLogCVConfig.setQueryName(logConfiguration.getLogDefinition().getName());
    stackdriverLogCVConfig.setMessageIdentifier(logConfiguration.getLogDefinition().getMessageIdentifier());
    stackdriverLogCVConfig.setServiceInstanceIdentifier(
        logConfiguration.getLogDefinition().getServiceInstanceIdentifier());
    stackdriverLogCVConfig.setCategory(ERRORS);
    stackdriverLogCVConfig.setServiceIdentifier(logConfiguration.getServiceIdentifier());
    stackdriverLogCVConfig.setEnvIdentifier(logConfiguration.getEnvIdentifier());
    return stackdriverLogCVConfig;
  }

  @Data
  @Builder
  public static class StackdriverLogConfiguration {
    StackdriverLogDefinition logDefinition;
    String serviceIdentifier;
    String envIdentifier;
  }

  @Value
  @Builder
  private static class Key {
    String envIdentifier;
    String serviceIdentifier;
    String queryName;
  }

  private Key getKeyFromConfig(StackdriverLogCVConfig cvConfig) {
    return Key.builder()
        .envIdentifier(cvConfig.getEnvIdentifier())
        .serviceIdentifier(cvConfig.getServiceIdentifier())
        .queryName(cvConfig.getQueryName())
        .build();
  }
}

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
import io.harness.cvng.core.entities.PrometheusCVConfig;

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
@JsonTypeName("PROMETHEUS")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PrometheusDSConfig extends DSConfig {
  List<PrometheusMetricDefinition> metricDefinitions;

  @Override
  public DataSourceType getType() {
    return DataSourceType.PROMETHEUS;
  }

  @Override
  public CVConfigUpdateResult getCVConfigUpdateResult(List<CVConfig> existingCVConfigs) {
    List<PrometheusCVConfig> cvConfigsFromThisObj = toCVConfigs();
    Map<Key, PrometheusCVConfig> existingConfigMap = new HashMap<>();

    List<PrometheusCVConfig> existingSDCVConfigs = (List<PrometheusCVConfig>) (List<?>) existingCVConfigs;

    for (PrometheusCVConfig prometheusCVConfig : existingSDCVConfigs) {
      existingConfigMap.put(getKeyFromConfig(prometheusCVConfig), prometheusCVConfig);
    }

    Map<Key, PrometheusCVConfig> currentCVConfigsMap = new HashMap<>();
    for (PrometheusCVConfig prometheusCVConfig : cvConfigsFromThisObj) {
      currentCVConfigsMap.put(getKeyFromConfig(prometheusCVConfig), prometheusCVConfig);
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
  public void validate(List<CVConfig> existingMapping) {}

  private List<PrometheusCVConfig> toCVConfigs() {
    List<PrometheusCVConfig> cvConfigs = new ArrayList<>();
    // One cvConfig per service+environment+groupName+category.
    Map<Key, List<PrometheusMetricDefinition>> keyDefinitionMap = new HashMap<>();
    metricDefinitions.forEach(prometheusMetricDefinition -> {
      Key key = getKeyFromPrometheusMetricDefinition(prometheusMetricDefinition);
      if (!keyDefinitionMap.containsKey(key)) {
        keyDefinitionMap.put(key, new ArrayList<>());
      }
      keyDefinitionMap.get(key).add(prometheusMetricDefinition);
    });

    keyDefinitionMap.forEach((key, definitionList) -> {
      PrometheusCVConfig cvConfig = PrometheusCVConfig.builder().groupName(key.getGroupName()).build();
      fillCommonFields(cvConfig);

      CVMonitoringCategory category = key.getCategory();
      cvConfig.setEnvIdentifier(key.getEnvIdentifier());
      cvConfig.setServiceIdentifier(key.getServiceIdentifier());
      cvConfig.setCategory(category);
      cvConfig.populateFromMetricDefinitions(definitionList, category);
      cvConfigs.add(cvConfig);
    });

    return cvConfigs;
  }

  private Key getKeyFromPrometheusMetricDefinition(PrometheusMetricDefinition prometheusMetricDefinition) {
    return Key.builder()
        .envIdentifier(prometheusMetricDefinition.getEnvIdentifier())
        .serviceIdentifier(prometheusMetricDefinition.getServiceIdentifier())
        .category(prometheusMetricDefinition.getRiskProfile().getCategory())
        .groupName(prometheusMetricDefinition.getGroupName())
        .build();
  }

  private Key getKeyFromConfig(PrometheusCVConfig prometheusCVConfig) {
    return Key.builder()
        .envIdentifier(prometheusCVConfig.getEnvIdentifier())
        .serviceIdentifier(prometheusCVConfig.getServiceIdentifier())
        .groupName(prometheusCVConfig.getGroupName())
        .category(prometheusCVConfig.getCategory())
        .build();
  }

  @Value
  @Builder
  private static class Key {
    String envIdentifier;
    String serviceIdentifier;
    CVMonitoringCategory category;
    String groupName;
  }
}

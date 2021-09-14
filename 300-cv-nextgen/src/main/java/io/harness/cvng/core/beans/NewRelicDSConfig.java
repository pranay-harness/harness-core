/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans;

import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.NewRelicCVConfig;

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
@JsonTypeName("NEW_RELIC")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewRelicDSConfig extends DSConfig {
  List<NewRelicServiceConfig> newRelicServiceConfigList;
  @Override
  public DataSourceType getType() {
    return DataSourceType.NEW_RELIC;
  }

  @Override
  public CVConfigUpdateResult getCVConfigUpdateResult(List<CVConfig> existingCVConfigs) {
    Map<Key, NewRelicCVConfig> mapExistingConfigs = new HashMap<>();
    existingCVConfigs.forEach(cvConfig
        -> mapExistingConfigs.put(getKeyFromCVConfig((NewRelicCVConfig) cvConfig), (NewRelicCVConfig) cvConfig));

    Map<Key, NewRelicCVConfig> mapConfigsFromThisObj = new HashMap<>();
    List<NewRelicCVConfig> cvConfigList = toCVConfigs();
    cvConfigList.forEach(config -> mapConfigsFromThisObj.put(getKeyFromCVConfig(config), config));

    Set<Key> deleted = Sets.difference(mapExistingConfigs.keySet(), mapConfigsFromThisObj.keySet());
    Set<Key> added = Sets.difference(mapConfigsFromThisObj.keySet(), mapExistingConfigs.keySet());
    Set<Key> updated = Sets.intersection(mapExistingConfigs.keySet(), mapConfigsFromThisObj.keySet());

    List<CVConfig> deletedConfigs =
        deleted.stream().map(key -> mapExistingConfigs.get(key)).collect(Collectors.toList());
    List<CVConfig> addedConfigs =
        added.stream().map(key -> mapConfigsFromThisObj.get(key)).collect(Collectors.toList());

    List<CVConfig> updatedWithoutUuid =
        updated.stream().map(key -> mapConfigsFromThisObj.get(key)).collect(Collectors.toList());
    List<CVConfig> updatedWithUuid =
        updated.stream().map(key -> mapExistingConfigs.get(key)).collect(Collectors.toList());

    for (int i = 0; i < updatedWithoutUuid.size(); i++) {
      updatedWithoutUuid.get(i).setUuid(updatedWithUuid.get(i).getUuid());
    }

    return CVConfigUpdateResult.builder()
        .added(addedConfigs)
        .deleted(deletedConfigs)
        .updated(updatedWithoutUuid)
        .build();
  }

  @Override
  public void validate(List<CVConfig> existingMapping) {
    existingMapping.forEach(cvConfig -> {
      getNewRelicServiceConfigList().forEach(newRelicServiceConfig -> {
        NewRelicCVConfig newRelicCVConfig = (NewRelicCVConfig) cvConfig;
        Preconditions.checkState(
            !(newRelicCVConfig.getApplicationId() == newRelicServiceConfig.getApplicationId()
                && newRelicCVConfig.getApplicationName().equals(newRelicServiceConfig.getApplicationName())
                && newRelicCVConfig.getEnvIdentifier().equals(newRelicServiceConfig.getEnvIdentifier())
                && newRelicCVConfig.getServiceIdentifier().equals(newRelicServiceConfig.getServiceIdentifier())),
            "A config already exists with the same mapping for Application and service/environment");
      });
    });
  }

  private List<NewRelicCVConfig> toCVConfigs() {
    List<NewRelicCVConfig> cvConfigs = new ArrayList<>();
    newRelicServiceConfigList.forEach(serviceConfig -> serviceConfig.metricPacks.forEach(metricPack -> {
      NewRelicCVConfig cvConfig = new NewRelicCVConfig();
      fillCommonFields(cvConfig);
      cvConfig.setApplicationName(serviceConfig.applicationName);
      cvConfig.setEnvIdentifier(serviceConfig.envIdentifier);
      cvConfig.setApplicationId(serviceConfig.applicationId);
      cvConfig.setServiceIdentifier(serviceConfig.serviceIdentifier);
      cvConfig.setMetricPack(metricPack);
      cvConfig.setCategory(metricPack.getCategory());
      cvConfigs.add(cvConfig);
    }));
    return cvConfigs;
  }

  private Key getKeyFromCVConfig(NewRelicCVConfig cvConfig) {
    return Key.builder()
        .applicationId(cvConfig.getApplicationId())
        .applicationName(cvConfig.getApplicationName())
        .envIdentifier(cvConfig.getEnvIdentifier())
        .serviceIdentifier(cvConfig.getServiceIdentifier())
        .metricPack(cvConfig.getMetricPack())
        .build();
  }

  @Value
  @Builder
  private static class Key {
    private String applicationName;
    private long applicationId;
    private String envIdentifier;
    private String serviceIdentifier;
    MetricPack metricPack;
  }

  @Data
  @Builder
  public static class NewRelicServiceConfig {
    private String applicationName;
    private long applicationId;
    private String envIdentifier;
    private String serviceIdentifier;
    private Set<MetricPack> metricPacks;
  }
}

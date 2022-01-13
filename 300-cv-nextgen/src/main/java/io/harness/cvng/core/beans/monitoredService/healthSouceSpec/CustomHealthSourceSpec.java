/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.beans.monitoredService.healthSouceSpec;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.beans.CustomHealthMetricDefinition;
import io.harness.cvng.core.beans.RiskProfile;
import io.harness.cvng.core.beans.monitoredService.HealthSource;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.CustomHealthCVConfig;
import io.harness.cvng.core.entities.CustomHealthCVConfig.MetricDefinition;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.core.validators.UniqueIdentifierCheck;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;

@Data
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomHealthSourceSpec extends MetricHealthSourceSpec {
  @UniqueIdentifierCheck List<CustomHealthMetricDefinition> metricDefinitions = new ArrayList<>();

  @Data
  @Builder
  public static class Key {
    String groupName;
    String category;
  }

  @Override
  public DataSourceType getType() {
    return DataSourceType.CUSTOM_HEALTH;
  }

  @Override
  public HealthSource.CVConfigUpdateResult getCVConfigUpdateResult(String accountId, String orgIdentifier,
      String projectIdentifier, String environmentRef, String serviceRef, String identifier, String name,
      List<CVConfig> existingCVConfigs, MetricPackService metricPackService) {
    List<CustomHealthCVConfig> existingDBCVConfigs = (List<CustomHealthCVConfig>) (List<?>) existingCVConfigs;
    Map<String, CustomHealthCVConfig> existingConfigs = new HashMap<>();
    existingDBCVConfigs.forEach(
        config -> existingConfigs.put(getKey(config.getGroupName(), config.getCategory()), config));

    Map<String, CustomHealthCVConfig> currentCVConfigs =
        getCVConfigs(accountId, orgIdentifier, projectIdentifier, environmentRef, serviceRef, identifier, name);

    Set<String> deleted = Sets.difference(existingConfigs.keySet(), currentCVConfigs.keySet());
    Set<String> added = Sets.difference(currentCVConfigs.keySet(), existingConfigs.keySet());
    Set<String> updated = Sets.intersection(existingConfigs.keySet(), currentCVConfigs.keySet());

    List<CVConfig> updatedConfigs = updated.stream().map(currentCVConfigs::get).collect(Collectors.toList());
    List<CVConfig> updatedConfigWithUuid = updated.stream().map(existingConfigs::get).collect(Collectors.toList());
    for (int i = 0; i < updatedConfigs.size(); i++) {
      updatedConfigs.get(i).setUuid(updatedConfigWithUuid.get(i).getUuid());
    }
    return HealthSource.CVConfigUpdateResult.builder()
        .deleted(deleted.stream().map(existingConfigs::get).collect(Collectors.toList()))
        .updated(updatedConfigs)
        .added(added.stream().map(currentCVConfigs::get).collect(Collectors.toList()))
        .build();
  }

  public Map<String, CustomHealthCVConfig> getCVConfigs(String accountId, String orgIdentifier,
      String projectIdentifier, String environmentRef, String serviceRef, String identifier, String name) {
    Map<String, CustomHealthCVConfig> cvConfigMap = new HashMap<>();
    metricDefinitions.forEach(metricDefinition -> {
      String groupName = metricDefinition.getGroupName();
      RiskProfile riskProfile = metricDefinition.getRiskProfile();

      if (riskProfile == null || riskProfile.getCategory() == null) {
        return;
      }

      String cvConfigKey = getKey(groupName, riskProfile.getCategory());
      CustomHealthCVConfig existingCvConfig = cvConfigMap.get(cvConfigKey);
      List<MetricDefinition> cvConfigMetricDefinitions =
          existingCvConfig != null && isNotEmpty(existingCvConfig.getMetricDefinitions())
          ? existingCvConfig.getMetricDefinitions()
          : new ArrayList<>();

      MetricResponseMapping metricResponseMapping = metricDefinition.getMetricResponseMapping();
      cvConfigMetricDefinitions.add(MetricDefinition.builder()
                                        .metricName(metricDefinition.getMetricName())
                                        .method(metricDefinition.getMethod())
                                        .queryType(metricDefinition.getQueryType())
                                        .metricResponseMapping(metricResponseMapping)
                                        .requestBody(metricDefinition.getRequestBody())
                                        .startTime(metricDefinition.getStartTime())
                                        .endTime(metricDefinition.getEndTime())
                                        .urlPath(metricDefinition.getUrlPath())
                                        .riskProfile(metricDefinition.getRiskProfile())
                                        .analysis(metricDefinition.getAnalysis())
                                        .sli(metricDefinition.getSli())
                                        .build());

      CustomHealthCVConfig mappedCVConfig = CustomHealthCVConfig.builder()
                                                .groupName(groupName)
                                                .metricDefinitions(cvConfigMetricDefinitions)
                                                .accountId(accountId)
                                                .orgIdentifier(orgIdentifier)
                                                .projectIdentifier(projectIdentifier)
                                                .identifier(identifier)
                                                .serviceIdentifier(serviceRef)
                                                .category(riskProfile.getCategory())
                                                .envIdentifier(environmentRef)
                                                .connectorIdentifier(getConnectorRef())
                                                .monitoringSourceName(name)
                                                .build();
      mappedCVConfig.setMetricPack(
          mappedCVConfig.generateMetricPack(metricDefinition.getMetricName(), metricDefinition.getRiskProfile()));

      cvConfigMap.put(cvConfigKey, mappedCVConfig);
    });

    return cvConfigMap;
  }

  public List<CustomHealthMetricDefinition> getMetricDefinitions() {
    return metricDefinitions;
  }

  public String getKey(String groupName, @NotNull CVMonitoringCategory category) {
    return String.format("%s%s", groupName, category);
  }
}

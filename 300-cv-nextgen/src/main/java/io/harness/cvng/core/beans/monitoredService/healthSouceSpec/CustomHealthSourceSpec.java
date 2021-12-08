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
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomHealthSourceSpec extends HealthSourceSpec {
  List<CustomHealthMetricDefinition> metricDefinitions = new ArrayList<>();

  @Override
  public DataSourceType getType() {
    return DataSourceType.CUSTOM_HEALTH;
  }

  @Override
  public HealthSource.CVConfigUpdateResult getCVConfigUpdateResult(String accountId, String orgIdentifier,
      String projectIdentifier, String environmentRef, String serviceRef, String identifier, String name,
      List<CVConfig> existingCVConfigs, MetricPackService metricPackService) {
    List<CustomHealthCVConfig> existingDBCVConfigs = (List<CustomHealthCVConfig>) (List<?>) existingCVConfigs;
    Map<CustomHealthSourceSpec.Key, CustomHealthCVConfig> existingConfigs = new HashMap<>();
    existingDBCVConfigs.forEach(config
        -> existingConfigs.put(
            Key.builder().groupName(config.getGroupName()).category(config.getCategory()).build(), config));

    Map<CustomHealthSourceSpec.Key, CustomHealthCVConfig> currentCVConfigs = getCVConfigs();

    Set<CustomHealthSourceSpec.Key> deleted = Sets.difference(existingConfigs.keySet(), currentCVConfigs.keySet());
    Set<CustomHealthSourceSpec.Key> added = Sets.difference(currentCVConfigs.keySet(), existingConfigs.keySet());
    Set<CustomHealthSourceSpec.Key> updated = Sets.intersection(existingConfigs.keySet(), currentCVConfigs.keySet());

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

  public Map<CustomHealthSourceSpec.Key, CustomHealthCVConfig> getCVConfigs() {
    Map<CustomHealthSourceSpec.Key, CustomHealthCVConfig> cvConfigMap = new HashMap<>();
    metricDefinitions.forEach(metricDefinition -> {
      String groupName = metricDefinition.getGroupName();
      RiskProfile riskProfile = metricDefinition.getRiskProfile();

      if (riskProfile == null || riskProfile.getCategory() == null) {
        return;
      }

      Key cvConfigKey = Key.builder().groupName(groupName).category(riskProfile.getCategory()).build();
      CustomHealthCVConfig existingCvConfig = cvConfigMap.get(cvConfigKey);
      List<MetricDefinition> cvConfigMetricDefinitions =
          existingCvConfig != null && isNotEmpty(existingCvConfig.getMetricDefinitions())
          ? existingCvConfig.getMetricDefinitions()
          : new ArrayList<>();

      cvConfigMetricDefinitions.add(MetricDefinition.builder()
                                        .metricName(metricDefinition.getMetricName())
                                        .method(metricDefinition.getMethod())
                                        .queryType(metricDefinition.getQueryType())
                                        .metricValueFieldPathString(metricDefinition.getMetricValueFieldPathString())
                                        .requestBody(metricDefinition.getRequestBody())
                                        .timestampFormat(metricDefinition.getTimestampFormat())
                                        .urlPath(metricDefinition.getUrlPath())
                                        .riskProfile(metricDefinition.getRiskProfile())
                                        .analysis(metricDefinition.getAnalysis())
                                        .sli(metricDefinition.getSli())
                                        .timestampFieldPathString(metricDefinition.getTimestampFieldPathString())
                                        .serviceInstance(metricDefinition.getServiceInstance())
                                        .build());

      CustomHealthCVConfig mappedCVConfig =
          CustomHealthCVConfig.builder().groupName(groupName).metricDefinitions(cvConfigMetricDefinitions).build();
      cvConfigMap.put(cvConfigKey, mappedCVConfig);
    });

    return cvConfigMap;
  }

  @Value
  @Builder
  public static class Key {
    String groupName;
    CVMonitoringCategory category;
  }
}

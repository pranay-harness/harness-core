package io.harness.cvng.core.beans;

import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.StackdriverCVConfig;

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
@JsonTypeName("STACKDRIVER")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StackdriverDSConfig extends DSConfig {
  private Set<StackdriverDefinition> metricDefinitions;
  private String serviceIdentifier;
  private Set<MetricPack> metricPacks;

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
      Preconditions.checkArgument(stackdriverCVConfig.getEnvIdentifier().equals(this.getEnvIdentifier()),
          "Can not get update result for different envIdentifier names.");
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

  private List<StackdriverCVConfig> toCVConfigs() {
    // group things under same service_env_category_dashboard into one config
    Map<String, List<StackdriverDefinition>> categoryMap = new HashMap<>();
    metricDefinitions.forEach(metricDefinition -> {
      CVMonitoringCategory category = metricDefinition.getRiskProfile().getCategory();
      String key = category + "_" + metricDefinition.getDashboardName();
      if (!categoryMap.containsKey(key)) {
        categoryMap.put(key, new ArrayList<>());
      }
      categoryMap.get(key).add(metricDefinition);
    });

    Map<CVMonitoringCategory, MetricPack> categoryMetricPackMap = new HashMap<>();
    metricPacks.forEach(pack -> { categoryMetricPackMap.put(pack.getCategory(), pack); });
    List<StackdriverCVConfig> cvConfigs = new ArrayList<>();
    categoryMap.forEach((key, stackdriverDefinitions) -> {
      StackdriverCVConfig cvConfig = StackdriverCVConfig.builder().build();
      fillCommonFields(cvConfig);
      CVMonitoringCategory category = stackdriverDefinitions.get(0).getRiskProfile().getCategory();
      cvConfig.setServiceIdentifier(serviceIdentifier);
      cvConfig.fromStackdriverDefinitions(stackdriverDefinitions, categoryMetricPackMap.get(category));
      cvConfig.setCategory(category);
      cvConfigs.add(cvConfig);
    });

    return cvConfigs;
  }

  private Key getKeyFromConfig(StackdriverCVConfig cvConfig) {
    return Key.builder()
        .serviceIdentifier(cvConfig.getServiceIdentifier())
        .category(cvConfig.getMetricPack().getCategory())
        .dashboardName(cvConfig.getDashboardName())
        .build();
  }

  @Value
  @Builder
  private static class Key {
    String serviceIdentifier;
    CVMonitoringCategory category;
    String dashboardName;
  }
}

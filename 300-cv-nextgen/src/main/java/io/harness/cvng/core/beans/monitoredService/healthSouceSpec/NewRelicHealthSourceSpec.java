package io.harness.cvng.core.beans.monitoredService.healthSouceSpec;

import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.beans.monitoredService.HealthSource.CVConfigUpdateResult;
import io.harness.cvng.core.beans.monitoredService.MetricPackDTO;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.NewRelicCVConfig;
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
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewRelicHealthSourceSpec extends HealthSourceSpec {
  @NotEmpty String applicationName;
  @NotEmpty String applicationId;
  @NotNull String feature;
  @NotNull @NotEmpty @Valid Set<MetricPackDTO> metricPacks;

  @Override
  public CVConfigUpdateResult getCVConfigUpdateResult(String accountId, String orgIdentifier, String projectIdentifier,
      String environmentRef, String serviceRef, String identifier, String name, List<CVConfig> existingCVConfigs,
      MetricPackService metricPackService) {
    Map<Key, NewRelicCVConfig> mapExistingConfigs = new HashMap<>();
    existingCVConfigs.forEach(cvConfig
        -> mapExistingConfigs.put(getKeyFromCVConfig((NewRelicCVConfig) cvConfig), (NewRelicCVConfig) cvConfig));

    Map<Key, NewRelicCVConfig> mapConfigsFromThisObj = new HashMap<>();
    List<NewRelicCVConfig> cvConfigList = toCVConfigs(
        accountId, orgIdentifier, projectIdentifier, environmentRef, serviceRef, identifier, name, metricPackService);
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
  public String getConnectorRef() {
    return connectorRef;
  }

  @Override
  public DataSourceType getType() {
    return DataSourceType.NEW_RELIC;
  }

  private List<NewRelicCVConfig> toCVConfigs(String accountId, String orgIdentifier, String projectIdentifier,
      String environmentRef, String serviceRef, String identifier, String name, MetricPackService metricPackService) {
    List<NewRelicCVConfig> cvConfigs = new ArrayList<>();
    metricPacks.forEach(metricPack -> {
      NewRelicCVConfig newRelicCVConfig = NewRelicCVConfig.builder()
                                              .accountId(accountId)
                                              .orgIdentifier(orgIdentifier)
                                              .projectIdentifier(projectIdentifier)
                                              .identifier(identifier)
                                              .connectorIdentifier(getConnectorRef())
                                              .monitoringSourceName(name)
                                              .applicationName(applicationName)
                                              .applicationId(Long.valueOf(applicationId))
                                              .envIdentifier(environmentRef)
                                              .serviceIdentifier(serviceRef)
                                              .metricPack(metricPack.toMetricPack(accountId, orgIdentifier,
                                                  projectIdentifier, getType(), metricPackService))
                                              .category(metricPack.getIdentifier())
                                              .productName(feature)
                                              .build();
      cvConfigs.add(newRelicCVConfig);
    });
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

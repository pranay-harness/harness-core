package io.harness.cvng.core.beans;

import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.NewRelicCVConfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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
    return CVConfigUpdateResult.builder().added((List<CVConfig>) (List<?>) toCVConfigs()).build();
  }

  @Override
  public void validate(List<CVConfig> existingMapping) {
    // TODO: Recheck this logic.
    existingMapping.forEach(cvConfig -> {
      getNewRelicServiceConfigList().forEach(newRelicServiceConfig -> {
        NewRelicCVConfig newRelicCVConfig = (NewRelicCVConfig) cvConfig;
        Preconditions.checkState(
            !(newRelicCVConfig.getApplicationId().equals(newRelicServiceConfig.getApplicationId())
                && newRelicCVConfig.getApplicationName().equals(newRelicServiceConfig.getApplicationName())
                && newRelicCVConfig.getEnvIdentifier().equals(newRelicServiceConfig.getEnvIdentifier())
                && newRelicCVConfig.getServiceIdentifier().equals(newRelicServiceConfig.getServiceIdentifier())),
            "");
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

  @Data
  @Builder
  public static class NewRelicServiceConfig {
    private String applicationName;
    private String applicationId;
    private String envIdentifier;
    private String serviceIdentifier;
    private Set<MetricPack> metricPacks;
  }
}

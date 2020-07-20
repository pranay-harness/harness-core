package io.harness.cdng.service.beans;

import io.harness.data.structure.EmptyPredicate;
import io.harness.yaml.core.intfc.OverridesApplier;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Wither;

import javax.validation.constraints.NotNull;

@Data
@Builder
public class ServiceConfig implements OverridesApplier<ServiceConfig> {
  @Wither private ServiceUseFromStage useFromStage;
  @NotNull private String identifier;
  @Wither @NotNull private String name;
  @Wither private String description;
  private ServiceDefinition serviceDef;
  @Wither private StageOverridesConfig stageOverrides;

  public ServiceConfig applyUseFromStage(ServiceConfig serviceConfigToUseFrom) {
    return serviceConfigToUseFrom.withStageOverrides(stageOverrides).withUseFromStage(useFromStage);
  }

  @Override
  public ServiceConfig applyOverrides(ServiceConfig overrideConfig) {
    ServiceConfig resultantConfig = this;
    if (EmptyPredicate.isNotEmpty(overrideConfig.getName())) {
      resultantConfig = resultantConfig.withName(overrideConfig.getName());
    }
    if (EmptyPredicate.isNotEmpty(overrideConfig.getDescription())) {
      resultantConfig = resultantConfig.withDescription(overrideConfig.getDescription());
    }
    return resultantConfig;
  }
}

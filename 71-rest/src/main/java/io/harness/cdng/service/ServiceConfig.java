package io.harness.cdng.service;

import io.harness.cdng.service.beans.ServiceUseFromStage;
import io.harness.data.Outcome;
import io.harness.data.structure.EmptyPredicate;
import io.harness.yaml.core.intfc.OverridesApplier;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Wither;

import javax.validation.constraints.NotNull;

@Data
@Builder
public class ServiceConfig implements Outcome, OverridesApplier<ServiceConfig> {
  @NotNull private String identifier;
  @Wither @NotNull private String displayName;
  @Wither private String description;
  private ServiceSpec serviceSpec;
  @Wither private StageOverridesConfig stageOverrides;
  @Wither private ServiceUseFromStage useFromStage;

  public ServiceConfig applyUseFromStage(ServiceConfig serviceConfigToUseFrom) {
    return serviceConfigToUseFrom.withStageOverrides(stageOverrides).withUseFromStage(useFromStage);
  }

  @Override
  public ServiceConfig applyOverrides(ServiceConfig overrideConfig) {
    ServiceConfig resultantConfig = this;
    if (EmptyPredicate.isNotEmpty(overrideConfig.getDisplayName())) {
      resultantConfig = resultantConfig.withDisplayName(overrideConfig.getDisplayName());
    }
    if (EmptyPredicate.isNotEmpty(overrideConfig.getDescription())) {
      resultantConfig = resultantConfig.withDescription(overrideConfig.getDescription());
    }
    return resultantConfig;
  }
}

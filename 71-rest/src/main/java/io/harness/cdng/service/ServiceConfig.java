package io.harness.cdng.service;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cdng.service.beans.ServiceUseFromStage;
import io.harness.data.Outcome;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
public class ServiceConfig implements Outcome {
  @NotNull private String identifier;
  @NotNull private String displayName;
  private String description;
  private ServiceSpec serviceSpec;
  private StageOverridesConfig stageOverrides;
  private ServiceUseFromStage useFromStage;

  public ServiceConfig applyUseFromStage(ServiceConfig serviceConfigToUseFrom) {
    ServiceConfigBuilder builder = ServiceConfig.builder();
    if (useFromStage.getOverrides() != null) {
      builder
          .displayName(isNotEmpty(useFromStage.getOverrides().getDisplayName())
                  ? useFromStage.getOverrides().getDisplayName()
                  : serviceConfigToUseFrom.getDisplayName())
          .description(isNotEmpty(useFromStage.getOverrides().getDescription())
                  ? useFromStage.getOverrides().getDescription()
                  : serviceConfigToUseFrom.getDescription());
    }

    return builder.serviceSpec(serviceConfigToUseFrom.getServiceSpec()).stageOverrides(stageOverrides).build();
  }
}

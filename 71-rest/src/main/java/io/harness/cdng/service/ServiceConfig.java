package io.harness.cdng.service;

import io.harness.data.Outcome;
import io.harness.data.structure.EmptyPredicate;
import io.harness.yaml.core.UseFromStage;
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
  private OverrideConfig overrides;
  private UseFromStage useFromStage;

  /** Merge non null properties of given parameter on caller properties and return a new config. */
  public ServiceConfig mergeNonNullProperties(ServiceConfig serviceConfig) {
    return ServiceConfig.builder()
        .displayName(
            EmptyPredicate.isNotEmpty(serviceConfig.getDisplayName()) ? serviceConfig.getDisplayName() : displayName)
        .description(
            EmptyPredicate.isNotEmpty(serviceConfig.getDescription()) ? serviceConfig.getDescription() : description)
        .overrides(serviceConfig.getOverrides())
        .serviceSpec(serviceSpec)
        .build();
  }
}

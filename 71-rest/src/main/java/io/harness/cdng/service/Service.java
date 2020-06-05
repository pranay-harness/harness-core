package io.harness.cdng.service;

import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.data.Outcome;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
public class Service implements Outcome {
  @NotNull private String identifier;
  @NotNull private String name;
  private ArtifactListConfig artifacts;
}

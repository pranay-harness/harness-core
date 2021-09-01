package io.harness.cdng.artifact.steps;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@TypeAlias("artifactStepParameters")
public class ArtifactStepParameters implements StepParameters {
  String identifier;
  ArtifactSourceType type;
  ArtifactConfig spec;
  @Singular List<ArtifactConfig> overrideSets;
  ArtifactConfig stageOverride;
}

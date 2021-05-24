package io.harness.cdng.manifest.steps;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@TypeAlias("manifestStepParameters")
public class ManifestStepParameters implements StepParameters {
  String identifier;
  String type;
  ManifestAttributes spec;
  @Singular List<ManifestAttributes> overrideSets;
  ManifestAttributes stageOverride;
}

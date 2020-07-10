package io.harness.cdng.manifest.state;

import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ManifestStepParameters implements StepParameters {
  List<ManifestConfigWrapper> serviceSpecManifests;
  List<ManifestConfigWrapper> manifestOverrideSets;
  List<ManifestConfigWrapper> stageOverrideManifests;
}
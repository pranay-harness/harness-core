package io.harness.cdng.tasks.manifestFetch.step;

import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.state.io.StepParameters;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ManifestFetchParameters implements StepParameters {
  private List<ManifestAttributes> serviceSpecManifestAttributes;
  private List<ManifestAttributes> overridesManifestAttributes;
  private boolean fetchValuesOnly;
}

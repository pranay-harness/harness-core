package io.harness.cdng.manifest.yaml;

import io.harness.data.Outcome;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ManifestOutcome implements Outcome {
  List<ManifestAttributes> manifestAttributesForServiceSpec;
  List<ManifestAttributes> manifestAttributesForOverride;
  List<ManifestAttributes> manifestAttributesForOverrideSets;
}

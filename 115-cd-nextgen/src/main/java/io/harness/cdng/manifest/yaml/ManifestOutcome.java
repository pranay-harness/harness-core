package io.harness.cdng.manifest.yaml;

import io.harness.data.Outcome;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ManifestOutcome implements Outcome {
  @NonNull List<ManifestAttributes> manifestAttributes;
}

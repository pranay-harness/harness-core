package io.harness.cdng.manifest.yaml;

import io.harness.data.Outcome;

import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class ManifestOutcome implements Outcome {
  @NonNull List<ManifestAttributes> manifestAttributes;
}

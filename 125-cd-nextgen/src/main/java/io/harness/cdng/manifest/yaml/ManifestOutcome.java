package io.harness.cdng.manifest.yaml;

import io.harness.pms.sdk.core.data.Outcome;

import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("manifestOutcome")
public class ManifestOutcome implements Outcome {
  @NonNull List<ManifestAttributes> manifestAttributes;
}

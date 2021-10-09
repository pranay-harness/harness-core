package io.harness.cdng.tasks.manifestFetch.step;

import io.harness.annotation.RecasterAlias;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("manifestFetchParameters")
@RecasterAlias("io.harness.cdng.tasks.manifestFetch.step.ManifestFetchParameters")
public class ManifestFetchParameters implements StepParameters {
  private List<ManifestAttributes> serviceSpecManifestAttributes;
  private List<ManifestAttributes> overridesManifestAttributes;
  private boolean fetchValuesOnly;
}

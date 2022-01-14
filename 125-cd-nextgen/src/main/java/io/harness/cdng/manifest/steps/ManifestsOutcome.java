package io.harness.cdng.manifest.steps;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDC)
@JsonTypeName("ManifestsOutcome")
@TypeAlias("manifestsOutcome")
@RecasterAlias("io.harness.cdng.manifest.steps.ManifestsOutcome")
public class ManifestsOutcome extends HashMap<String, ManifestOutcome> implements Outcome {
  public ManifestsOutcome() {}

  public ManifestsOutcome(Map<String, ManifestOutcome> map) {
    super(map);
  }
}

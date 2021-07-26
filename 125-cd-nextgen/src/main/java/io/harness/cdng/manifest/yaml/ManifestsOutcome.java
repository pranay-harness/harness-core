package io.harness.cdng.manifest.yaml;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.manifest.ManifestOutcome;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("manifestsOutcome")
@JsonTypeName("manifestsOutcome")
@OwnedBy(CDC)
public class ManifestsOutcome implements Outcome {
  @NotEmpty List<io.harness.manifest.ManifestOutcome> manifestOutcomeList;
  @NotEmpty List<io.harness.manifest.ManifestOutcome> manifestOriginalList;
  @NotEmpty List<io.harness.manifest.ManifestOutcome> manifestStageOverridesList;
  @Singular Map<String, List<ManifestOutcome>> manifestOverrideSets;
}

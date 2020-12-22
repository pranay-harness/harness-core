package io.harness.cdng.manifest.yaml;

import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("manifestsOutcome")
@JsonTypeName("manifestsOutcome")
public class ManifestsOutcome implements Outcome {
  @NotEmpty List<ManifestOutcome> manifestOutcomeList;

  @Override
  public String getType() {
    return "manifestOutcome";
  }
}

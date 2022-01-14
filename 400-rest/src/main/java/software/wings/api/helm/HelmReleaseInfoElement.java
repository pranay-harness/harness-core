package software.wings.api.helm;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName("helmReleaseInfoElement")
@OwnedBy(CDP)
public class HelmReleaseInfoElement implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "helmReleaseInfoElement";

  private String releaseName;

  @Override
  public String getType() {
    return "helmReleaseInfoElement";
  }
}

package software.wings.api.helm;

import io.harness.pms.sdk.core.data.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName("helmReleaseInfoElement")
public class HelmReleaseInfoElement implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "helmReleaseInfoElement";

  private String releaseName;

  @Override
  public String getType() {
    return "helmReleaseInfoElement";
  }
}

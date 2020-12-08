package software.wings.api.helm;

import io.harness.pms.sdk.core.data.SweepingOutput;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HelmReleaseInfoElement implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "helmReleaseInfoElement";

  private String releaseName;
}

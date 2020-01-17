package software.wings.api;

import io.harness.beans.SweepingOutput;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TerraformApplyMarkerParam implements SweepingOutput {
  private String provisionerId;
  private boolean applyCompleted;
}
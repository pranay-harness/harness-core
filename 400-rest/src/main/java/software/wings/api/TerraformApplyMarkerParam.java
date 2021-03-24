package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName("terraformApplyMarkerParam")
@OwnedBy(CDP)
public class TerraformApplyMarkerParam implements SweepingOutput {
  private String provisionerId;
  private boolean applyCompleted;

  @Override
  public String getType() {
    return "terraformApplyMarkerParam";
  }
}

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

@TargetModule(HarnessModule._957_CG_BEANS)
@OwnedBy(CDC)
@Data
@Builder
public class WorkflowCreationFlags {
  private static final String ECS_BG_TYPE_DNS = "DNS";
  private static final String AWS_TRAFFIC_SHIFT_TYPE_ALB = "ELB";
  private String ecsBGType;
  private String awsTrafficShiftType;

  public boolean isEcsBgDnsType() {
    return ECS_BG_TYPE_DNS.equals(ecsBGType);
  }

  public boolean isAwsTrafficShiftAlbType() {
    return AWS_TRAFFIC_SHIFT_TYPE_ALB.equals(awsTrafficShiftType);
  }
}

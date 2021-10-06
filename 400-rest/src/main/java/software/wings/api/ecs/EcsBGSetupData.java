package software.wings.api.ecs;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDP)
@TargetModule(HarnessModule._957_CG_BEANS)
public class EcsBGSetupData {
  private boolean ecsBlueGreen;
  private String prodEcsListener;
  private String stageEcsListener;
  private String ecsBGTargetGroup1;
  private String ecsBGTargetGroup2;
  private boolean isUseSpecificListenerRuleArn;
  private String prodListenerRuleArn;
  private String stageListenerRuleArn;
  private String downsizedServiceName;
  private int downsizedServiceCount;

  // For Route 53 swap
  private boolean useRoute53Swap;
  private String parentRecordName;
  private String parentRecordHostedZoneId;
  private String oldServiceDiscoveryArn;
  private String newServiceDiscoveryArn;
}

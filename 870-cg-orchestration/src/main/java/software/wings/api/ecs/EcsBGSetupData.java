package software.wings.api.ecs;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
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

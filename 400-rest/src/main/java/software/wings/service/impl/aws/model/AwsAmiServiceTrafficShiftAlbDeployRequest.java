package software.wings.service.impl.aws.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static software.wings.service.impl.aws.model.AwsAmiRequest.AwsAmiRequestType.EXECUTE_AMI_SERVICE_TRAFFIC_SHIFT_ALB_DEPLOY;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class AwsAmiServiceTrafficShiftAlbDeployRequest extends AwsAmiRequest {
  private String accountId;
  private String appId;
  private String activityId;
  private String commandName;
  private String newAutoScalingGroupName;
  private String oldAutoScalingGroupName;
  private Integer oldAsgFinalDesiredCount;
  private Integer autoScalingSteadyStateTimeout;
  private int minInstances;
  private int maxInstances;
  private int desiredInstances;
  private AwsAmiPreDeploymentData preDeploymentData;
  private boolean rollback;
  private List<String> baseScalingPolicyJSONs;
  private List<String> infraMappingTargetGroupArns;

  @Builder
  public AwsAmiServiceTrafficShiftAlbDeployRequest(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String accountId, String appId, String activityId, String commandName,
      String newAutoScalingGroupName, String oldAutoScalingGroupName, Integer oldAsgFinalDesiredCount,
      Integer autoScalingSteadyStateTimeout, int minInstances, int maxInstances,
      AwsAmiPreDeploymentData preDeploymentData, boolean rollback, List<String> baseScalingPolicyJSONs,
      int desiredInstances, List<String> infraMappingTargetGroupArns, boolean amiInServiceHealthyStateFFEnabled) {
    super(awsConfig, encryptionDetails, EXECUTE_AMI_SERVICE_TRAFFIC_SHIFT_ALB_DEPLOY, region,
        amiInServiceHealthyStateFFEnabled);
    this.accountId = accountId;
    this.appId = appId;
    this.commandName = commandName;
    this.newAutoScalingGroupName = newAutoScalingGroupName;
    this.activityId = activityId;
    this.oldAsgFinalDesiredCount = oldAsgFinalDesiredCount;
    this.minInstances = minInstances;
    this.oldAutoScalingGroupName = oldAutoScalingGroupName;
    this.autoScalingSteadyStateTimeout = autoScalingSteadyStateTimeout;
    this.preDeploymentData = preDeploymentData;
    this.maxInstances = maxInstances;
    this.desiredInstances = desiredInstances;
    this.rollback = rollback;
    this.baseScalingPolicyJSONs = baseScalingPolicyJSONs;
    this.infraMappingTargetGroupArns = infraMappingTargetGroupArns;
  }
}

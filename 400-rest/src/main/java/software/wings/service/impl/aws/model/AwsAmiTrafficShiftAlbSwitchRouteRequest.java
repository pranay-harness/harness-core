package software.wings.service.impl.aws.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static software.wings.service.impl.aws.model.AwsAmiRequest.AwsAmiRequestType.EXECUTE_AMI_SERVICE_TRAFFIC_SHIFT_ALB;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.task.aws.LbDetailsForAlbTrafficShift;
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
public class AwsAmiTrafficShiftAlbSwitchRouteRequest extends AwsAmiRequest {
  private String accountId;
  private String appId;
  private String activityId;
  private String commandName;
  private String oldAsgName;
  private String newAsgName;
  private boolean downscaleOldAsg;
  boolean rollback;
  private int newAutoscalingGroupWeight;
  private Integer timeoutIntervalInMin;
  private AwsAmiPreDeploymentData preDeploymentData;
  private List<String> baseScalingPolicyJSONs;
  private List<LbDetailsForAlbTrafficShift> lbDetails;

  @Builder
  public AwsAmiTrafficShiftAlbSwitchRouteRequest(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String accountId, String appId, String activityId, String commandName, String oldAsgName,
      String newAsgName, AwsAmiPreDeploymentData preDeploymentData, boolean downscaleOldAsg, boolean rollback,
      List<String> baseScalingPolicyJSONs, Integer timeoutIntervalInMin, int newAutoscalingGroupWeight,
      List<LbDetailsForAlbTrafficShift> lbDetails) {
    super(awsConfig, encryptionDetails, EXECUTE_AMI_SERVICE_TRAFFIC_SHIFT_ALB, region);
    this.accountId = accountId;
    this.appId = appId;
    this.activityId = activityId;
    this.commandName = commandName;
    this.oldAsgName = oldAsgName;
    this.newAsgName = newAsgName;
    this.preDeploymentData = preDeploymentData;
    this.downscaleOldAsg = downscaleOldAsg;
    this.rollback = rollback;
    this.baseScalingPolicyJSONs = baseScalingPolicyJSONs;
    this.timeoutIntervalInMin = timeoutIntervalInMin;
    this.newAutoscalingGroupWeight = newAutoscalingGroupWeight;
    this.lbDetails = lbDetails;
  }
}

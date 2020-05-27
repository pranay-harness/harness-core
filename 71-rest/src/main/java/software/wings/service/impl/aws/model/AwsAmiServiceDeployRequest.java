package software.wings.service.impl.aws.model;

import static software.wings.service.impl.aws.model.AwsAmiRequest.AwsAmiRequestType.EXECUTE_AMI_SERVICE_DEPLOY;

import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsAmiServiceDeployRequest extends AwsAmiRequest {
  private String accountId;
  private String appId;
  private String activityId;
  private String commandName;
  private boolean resizeNewFirst;
  private String newAutoScalingGroupName;
  private Integer newAsgFinalDesiredCount;
  private String oldAutoScalingGroupName;
  private Integer oldAsgFinalDesiredCount;
  private List<AwsAmiResizeData> asgDesiredCounts;
  private Integer autoScalingSteadyStateTimeout;
  private int minInstances;
  private int maxInstances;
  private int desiredInstances;
  private AwsAmiPreDeploymentData preDeploymentData;
  private List<String> infraMappingClassisLbs;
  private List<String> infraMappingTargetGroupArns;
  private boolean rollback;
  private List<String> baseScalingPolicyJSONs;
  private List<String> existingInstanceIds;

  @Builder
  public AwsAmiServiceDeployRequest(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String accountId, String appId, String activityId, String commandName, boolean resizeNewFirst,
      String newAutoScalingGroupName, Integer newAsgFinalDesiredCount, String oldAutoScalingGroupName,
      Integer oldAsgFinalDesiredCount, Integer autoScalingSteadyStateTimeout, int minInstances,
      List<AwsAmiResizeData> asgDesiredCounts, int maxInstances, AwsAmiPreDeploymentData preDeploymentData,
      List<String> infraMappingClassisLbs, List<String> infraMappingTargetGroupArns, boolean rollback,
      List<String> baseScalingPolicyJSONs, int desiredInstances, List<String> existingInstanceIds) {
    super(awsConfig, encryptionDetails, EXECUTE_AMI_SERVICE_DEPLOY, region);
    this.accountId = accountId;
    this.appId = appId;
    this.activityId = activityId;
    this.commandName = commandName;
    this.resizeNewFirst = resizeNewFirst;
    this.newAutoScalingGroupName = newAutoScalingGroupName;
    this.newAsgFinalDesiredCount = newAsgFinalDesiredCount;
    this.oldAutoScalingGroupName = oldAutoScalingGroupName;
    this.oldAsgFinalDesiredCount = oldAsgFinalDesiredCount;
    this.autoScalingSteadyStateTimeout = autoScalingSteadyStateTimeout;
    this.minInstances = minInstances;
    this.asgDesiredCounts = asgDesiredCounts;
    this.maxInstances = maxInstances;
    this.preDeploymentData = preDeploymentData;
    this.infraMappingClassisLbs = infraMappingClassisLbs;
    this.infraMappingTargetGroupArns = infraMappingTargetGroupArns;
    this.rollback = rollback;
    this.baseScalingPolicyJSONs = baseScalingPolicyJSONs;
    this.desiredInstances = desiredInstances;
    this.existingInstanceIds = existingInstanceIds;
  }
}
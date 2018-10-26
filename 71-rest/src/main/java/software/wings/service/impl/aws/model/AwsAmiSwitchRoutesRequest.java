package software.wings.service.impl.aws.model;

import static software.wings.service.impl.aws.model.AwsAmiRequest.AwsAmiRequestType.EXECUTE_AMI_SWITCH_ROUTE;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsAmiSwitchRoutesRequest extends AwsAmiRequest {
  private String accountId;
  private String appId;
  private String activityId;
  private String commandName;
  private String oldAsgName;
  private List<String> primaryClassicLBs;
  private List<String> primaryTargetGroupARNs;
  private String newAsgName;
  private List<String> stageClassicLBs;
  private List<String> stageTargetGroupARNs;
  private int registrationTimeout;
  private boolean downscaleOldAsg;
  private AwsAmiPreDeploymentData preDeploymentData;
  boolean rollback;

  @Builder
  public AwsAmiSwitchRoutesRequest(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String accountId, String appId, String activityId, String commandName, String oldAsgName,
      List<String> primaryClassicLBs, List<String> primaryTargetGroupARNs, String newAsgName,
      List<String> stageClassicLBs, List<String> stageTargetGroupARNs, int registrationTimeout,
      AwsAmiPreDeploymentData preDeploymentData, boolean downscaleOldAsg, boolean rollback) {
    super(awsConfig, encryptionDetails, EXECUTE_AMI_SWITCH_ROUTE, region);
    this.accountId = accountId;
    this.appId = appId;
    this.activityId = activityId;
    this.commandName = commandName;
    this.oldAsgName = oldAsgName;
    this.primaryClassicLBs = primaryClassicLBs;
    this.primaryTargetGroupARNs = primaryTargetGroupARNs;
    this.newAsgName = newAsgName;
    this.stageClassicLBs = stageClassicLBs;
    this.stageTargetGroupARNs = stageTargetGroupARNs;
    this.registrationTimeout = registrationTimeout;
    this.preDeploymentData = preDeploymentData;
    this.downscaleOldAsg = downscaleOldAsg;
    this.rollback = rollback;
  }
}
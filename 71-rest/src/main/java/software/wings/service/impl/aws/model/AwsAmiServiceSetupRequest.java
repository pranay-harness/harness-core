package software.wings.service.impl.aws.model;

import static software.wings.service.impl.aws.model.AwsAmiRequest.AwsAmiRequestType.EXECUTE_AMI_SERVICE_SETUP;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsAmiServiceSetupRequest extends AwsAmiRequest {
  private String accountId;
  private String appId;
  private String activityId;
  private String commandName;
  private String infraMappingAsgName;
  private String infraMappingId;
  private List<String> infraMappingClassisLbs;
  private List<String> infraMappingTargetGroupArns;
  private String newAsgNamePrefix;
  private Integer maxInstances;
  private Integer autoScalingSteadyStateTimeout;
  private String artifactRevision;
  private String userData;
  private boolean blueGreen;

  @Builder
  public AwsAmiServiceSetupRequest(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String infraMappingAsgName, String infraMappingId, List<String> infraMappingClassisLbs,
      List<String> infraMappingTargetGroupArns, String newAsgNamePrefix, Integer maxInstances,
      Integer autoScalingSteadyStateTimeout, String artifactRevision, String userData, String accountId, String appId,
      String activityId, String commandName, boolean blueGreen) {
    super(awsConfig, encryptionDetails, EXECUTE_AMI_SERVICE_SETUP, region);
    this.accountId = accountId;
    this.appId = appId;
    this.activityId = activityId;
    this.commandName = commandName;
    this.infraMappingAsgName = infraMappingAsgName;
    this.infraMappingId = infraMappingId;
    this.infraMappingClassisLbs = infraMappingClassisLbs;
    this.infraMappingTargetGroupArns = infraMappingTargetGroupArns;
    this.newAsgNamePrefix = newAsgNamePrefix;
    this.maxInstances = maxInstances;
    this.autoScalingSteadyStateTimeout = autoScalingSteadyStateTimeout;
    this.artifactRevision = artifactRevision;
    this.userData = userData;
    this.blueGreen = blueGreen;
  }
}
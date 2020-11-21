package software.wings.service.impl.aws.model;

import static software.wings.service.impl.aws.model.AwsAmiRequest.AwsAmiRequestType.EXECUTE_AMI_SERVICE_TRAFFIC_SHIFT_ALB_SETUP;

import io.harness.delegate.task.aws.LbDetailsForAlbTrafficShift;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsAmiServiceTrafficShiftAlbSetupRequest extends AwsAmiRequest {
  private String accountId;
  private String appId;
  private String activityId;
  private String commandName;
  private String infraMappingAsgName;
  private String infraMappingId;
  private String newAsgNamePrefix;
  private Integer minInstances;
  private Integer maxInstances;
  private Integer desiredInstances;
  private Integer autoScalingSteadyStateTimeout;
  private String artifactRevision;
  private boolean useCurrentRunningCount;
  private List<LbDetailsForAlbTrafficShift> lbDetails;

  @Builder
  public AwsAmiServiceTrafficShiftAlbSetupRequest(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String infraMappingAsgName, String infraMappingId, String newAsgNamePrefix, Integer maxInstances,
      Integer autoScalingSteadyStateTimeout, String artifactRevision, String accountId, String appId, String activityId,
      String commandName, boolean useCurrentRunningCount, Integer desiredInstances, Integer minInstances,
      List<LbDetailsForAlbTrafficShift> lbDetails) {
    super(awsConfig, encryptionDetails, EXECUTE_AMI_SERVICE_TRAFFIC_SHIFT_ALB_SETUP, region);
    this.appId = appId;
    this.activityId = activityId;
    this.useCurrentRunningCount = useCurrentRunningCount;
    this.infraMappingAsgName = infraMappingAsgName;
    this.infraMappingId = infraMappingId;
    this.artifactRevision = artifactRevision;
    this.newAsgNamePrefix = newAsgNamePrefix;
    this.maxInstances = maxInstances;
    this.autoScalingSteadyStateTimeout = autoScalingSteadyStateTimeout;
    this.commandName = commandName;
    this.desiredInstances = desiredInstances;
    this.minInstances = minInstances;
    this.lbDetails = lbDetails;
    this.accountId = accountId;
  }
}

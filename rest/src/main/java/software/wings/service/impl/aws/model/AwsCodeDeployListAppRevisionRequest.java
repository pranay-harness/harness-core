package software.wings.service.impl.aws.model;

import static software.wings.service.impl.aws.model.AwsCodeDeployRequest.AwsCodeDeployRequestType.LIST_APP_REVISION;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsCodeDeployListAppRevisionRequest extends AwsCodeDeployRequest {
  private String appName;
  private String deploymentGroupName;

  @Builder
  public AwsCodeDeployListAppRevisionRequest(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String appName, String deploymentGroupName) {
    super(awsConfig, encryptionDetails, LIST_APP_REVISION, region);
    this.appName = appName;
    this.deploymentGroupName = deploymentGroupName;
  }
}
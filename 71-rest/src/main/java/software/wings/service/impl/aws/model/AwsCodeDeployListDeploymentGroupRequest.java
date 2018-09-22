package software.wings.service.impl.aws.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsCodeDeployListDeploymentGroupRequest extends AwsCodeDeployRequest {
  private String appName;

  @Builder
  public AwsCodeDeployListDeploymentGroupRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String appName) {
    super(awsConfig, encryptionDetails, AwsCodeDeployRequestType.LIST_DEPLOYMENT_GROUP, region);
    this.appName = appName;
  }
}
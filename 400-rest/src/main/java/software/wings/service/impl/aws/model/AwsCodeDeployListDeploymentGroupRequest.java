package software.wings.service.impl.aws.model;

import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

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

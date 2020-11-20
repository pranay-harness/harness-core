package software.wings.service.impl.aws.model;

import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsCodeDeployListDeploymentConfigRequest extends AwsCodeDeployRequest {
  @Builder
  public AwsCodeDeployListDeploymentConfigRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    super(awsConfig, encryptionDetails, AwsCodeDeployRequestType.LIST_DEPLOYMENT_CONFIGURATION, region);
  }
}

package software.wings.service.impl.aws.model;

import static software.wings.service.impl.aws.model.AwsCodeDeployRequest.AwsCodeDeployRequestType.LIST_DEPLOYMENT_INSTANCES;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsCodeDeployListDeploymentInstancesRequest extends AwsCodeDeployRequest {
  private String deploymentId;

  @Builder
  public AwsCodeDeployListDeploymentInstancesRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String deploymentId) {
    super(awsConfig, encryptionDetails, LIST_DEPLOYMENT_INSTANCES, region);
    this.deploymentId = deploymentId;
  }
}
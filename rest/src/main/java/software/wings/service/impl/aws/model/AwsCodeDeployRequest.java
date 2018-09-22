package software.wings.service.impl.aws.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsCodeDeployRequest extends AwsRequest {
  public enum AwsCodeDeployRequestType {
    LIST_APPLICATIONS,
    LIST_DEPLOYMENT_CONFIGURATION,
    LIST_DEPLOYMENT_GROUP,
    LIST_DEPLOYMENT_INSTANCES,
    LIST_APP_REVISION
  }

  @NotNull private AwsCodeDeployRequestType requestType;
  @NotNull private String region;

  public AwsCodeDeployRequest(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      AwsCodeDeployRequestType requestType, String region) {
    super(awsConfig, encryptionDetails);
    this.requestType = requestType;
    this.region = region;
  }
}
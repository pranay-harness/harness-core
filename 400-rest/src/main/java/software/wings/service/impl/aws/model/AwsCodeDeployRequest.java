package software.wings.service.impl.aws.model;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
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

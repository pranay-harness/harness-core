package software.wings.service.impl.aws.model;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(CDC)
@Data
@EqualsAndHashCode(callSuper = true)
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
public class AwsEcrRequest extends AwsRequest {
  public enum AwsEcrRequestType { GET_ECR_IMAGE_URL, GET_ECR_AUTH_TOKEN }
  @NotNull private AwsEcrRequestType requestType;
  @NotNull private String region;

  public AwsEcrRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, AwsEcrRequestType requestType, String region) {
    super(awsConfig, encryptionDetails);
    this.requestType = requestType;
    this.region = region;
  }
}

package software.wings.service.impl.aws.model;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(CDC)
@Data
@EqualsAndHashCode(callSuper = true)
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
public class AwsEcrGetAuthTokenRequest extends AwsEcrRequest {
  private String awsAccount;

  @Builder
  public AwsEcrGetAuthTokenRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String awsAccount) {
    super(awsConfig, encryptionDetails, AwsEcrRequestType.GET_ECR_AUTH_TOKEN, region);
    this.awsAccount = awsAccount;
  }
}

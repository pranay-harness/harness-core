package software.wings.service.impl.aws.model;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;

import java.util.List;

@OwnedBy(CDC)
@Data
@EqualsAndHashCode(callSuper = true)
public class AwsEcrGetAuthTokenRequest extends AwsEcrRequest {
  private String awsAccount;

  @Builder
  public AwsEcrGetAuthTokenRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String awsAccount) {
    super(awsConfig, encryptionDetails, AwsEcrRequestType.GET_ECR_AUTH_TOKEN, region);
    this.awsAccount = awsAccount;
  }
}
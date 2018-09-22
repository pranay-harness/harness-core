package software.wings.service.impl.aws.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

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
package software.wings.service.impl.aws.model;

import static software.wings.service.impl.aws.model.AwsEc2Request.AwsEc2RequestType.VALIDATE_CREDENTIALS;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsEc2ValidateCredentialsRequest extends AwsEc2Request {
  @Builder
  public AwsEc2ValidateCredentialsRequest(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    super(awsConfig, encryptionDetails, VALIDATE_CREDENTIALS);
  }
}
package software.wings.service.impl.aws.model;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;

import java.util.List;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Data
@EqualsAndHashCode(callSuper = true)
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
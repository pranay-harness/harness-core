package software.wings.service.impl.aws.model;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;

import java.util.List;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Data
@EqualsAndHashCode(callSuper = true)
public class AwsEcrGetImageUrlRequest extends AwsEcrRequest {
  @NotNull private String imageName;

  @Builder
  public AwsEcrGetImageUrlRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String imageName) {
    super(awsConfig, encryptionDetails, AwsEcrRequestType.GET_ECR_IMAGE_URL, region);
    this.imageName = imageName;
  }
}
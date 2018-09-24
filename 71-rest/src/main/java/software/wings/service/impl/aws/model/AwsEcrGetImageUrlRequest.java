package software.wings.service.impl.aws.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import javax.validation.constraints.NotNull;

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
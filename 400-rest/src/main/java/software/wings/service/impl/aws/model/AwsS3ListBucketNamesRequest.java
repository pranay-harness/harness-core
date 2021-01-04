package software.wings.service.impl.aws.model;

import static software.wings.service.impl.aws.model.AwsS3Request.AwsS3RequestType.LIST_BUCKET_NAMES;

import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsS3ListBucketNamesRequest extends AwsS3Request {
  @Builder
  public AwsS3ListBucketNamesRequest(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    super(awsConfig, encryptionDetails, LIST_BUCKET_NAMES);
  }
}

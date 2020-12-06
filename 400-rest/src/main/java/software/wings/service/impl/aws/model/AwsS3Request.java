package software.wings.service.impl.aws.model;

import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsS3Request extends AwsRequest implements TaskParameters {
  public enum AwsS3RequestType { LIST_BUCKET_NAMES }

  @NotNull private AwsS3RequestType requestType;

  public AwsS3Request(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, AwsS3RequestType requestType) {
    super(awsConfig, encryptionDetails);
    this.requestType = requestType;
  }
}

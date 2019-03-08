package software.wings.service.impl.aws.model;

import io.harness.delegate.task.TaskParameters;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsEcsRequest extends AwsRequest implements TaskParameters {
  public enum AwsEcsRequestType { LIST_CLUSTERS }

  @NotNull AwsEcsRequestType requestType;
  @NotNull String region;

  public AwsEcsRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, AwsEcsRequestType requestType, String region) {
    super(awsConfig, encryptionDetails);
    this.requestType = requestType;
    this.region = region;
  }
}
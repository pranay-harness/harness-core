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
public class AwsIamRequest extends AwsRequest implements TaskParameters {
  public enum AwsIamRequestType { LIST_IAM_ROLES, LIST_IAM_INSTANCE_ROLES }
  @NotNull private AwsIamRequestType requestType;

  public AwsIamRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, AwsIamRequestType requestType) {
    super(awsConfig, encryptionDetails);
    this.requestType = requestType;
  }
}

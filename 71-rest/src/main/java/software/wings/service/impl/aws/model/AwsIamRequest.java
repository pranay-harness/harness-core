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
public class AwsIamRequest extends AwsRequest implements TaskParameters {
  public enum AwsIamRequestType { LIST_IAM_ROLES, LIST_IAM_INSTANCE_ROLES }
  @NotNull private AwsIamRequestType requestType;

  public AwsIamRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, AwsIamRequestType requestType) {
    super(awsConfig, encryptionDetails);
    this.requestType = requestType;
  }
}
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
public class AwsAsgRequest extends AwsRequest implements TaskParameters {
  public enum AwsAsgRequestType { LIST_ALL_ASG_NAMES, LIST_ASG_INSTANCES, LIST_DESIRED_CAPACITIES, GET_RUNNING_COUNT }

  @NotNull private AwsAsgRequestType requestType;
  @NotNull private String region;

  public AwsAsgRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, AwsAsgRequestType requestType, String region) {
    super(awsConfig, encryptionDetails);
    this.requestType = requestType;
    this.region = region;
  }
}

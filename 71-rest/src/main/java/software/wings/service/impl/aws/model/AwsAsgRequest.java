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
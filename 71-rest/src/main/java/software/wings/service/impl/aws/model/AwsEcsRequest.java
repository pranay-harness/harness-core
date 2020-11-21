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
public class AwsEcsRequest extends AwsRequest implements TaskParameters {
  public enum AwsEcsRequestType { LIST_CLUSTERS, LIST_CLUSTER_SERVICES }

  @NotNull AwsEcsRequestType requestType;
  @NotNull String region;

  public AwsEcsRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, AwsEcsRequestType requestType, String region) {
    super(awsConfig, encryptionDetails);
    this.requestType = requestType;
    this.region = region;
  }
}

package software.wings.service.impl.aws.model;

import static software.wings.service.impl.aws.model.AwsAsgRequest.AwsAsgRequestType.LIST_ASG_INSTANCES;

import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsAsgListInstancesRequest extends AwsAsgRequest {
  private String autoScalingGroupName;

  @Builder
  public AwsAsgListInstancesRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName) {
    super(awsConfig, encryptionDetails, LIST_ASG_INSTANCES, region);
    this.autoScalingGroupName = autoScalingGroupName;
  }
}

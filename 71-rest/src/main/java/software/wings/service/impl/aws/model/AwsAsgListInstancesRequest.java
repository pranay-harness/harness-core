package software.wings.service.impl.aws.model;

import static software.wings.service.impl.aws.model.AwsAsgRequest.AwsAsgRequestType.LIST_ASG_INSTANCES;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

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
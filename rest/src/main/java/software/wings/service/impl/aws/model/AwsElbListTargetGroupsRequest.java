package software.wings.service.impl.aws.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsElbListTargetGroupsRequest extends AwsElbRequest {
  private String loadBalancerName;

  @Builder
  public AwsElbListTargetGroupsRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String loadBalancerName) {
    super(awsConfig, encryptionDetails, AwsElbRequestType.LIST_TARGET_GROUPS_FOR_ALBS, region);
    this.loadBalancerName = loadBalancerName;
  }
}
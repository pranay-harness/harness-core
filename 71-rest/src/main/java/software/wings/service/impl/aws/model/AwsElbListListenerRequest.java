package software.wings.service.impl.aws.model;

import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsElbListListenerRequest extends AwsElbRequest {
  private String loadBalancerName;

  @Builder
  public AwsElbListListenerRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String loadBalancerName) {
    super(awsConfig, encryptionDetails, AwsElbRequestType.LIST_LISTENER_FOR_ELB, region);
    this.loadBalancerName = loadBalancerName;
  }
}

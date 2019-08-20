package software.wings.service.impl.aws.model;

import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsElbListNetworkElbsRequest extends AwsElbRequest {
  @Builder
  public AwsElbListNetworkElbsRequest(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    super(awsConfig, encryptionDetails, AwsElbRequestType.LIST_NETWORK_LBS, region);
  }
}

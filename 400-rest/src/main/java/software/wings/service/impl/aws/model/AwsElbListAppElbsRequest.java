package software.wings.service.impl.aws.model;

import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsElbListAppElbsRequest extends AwsElbRequest {
  @Builder
  public AwsElbListAppElbsRequest(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    super(awsConfig, encryptionDetails, AwsElbRequestType.LIST_APPLICATION_LBS, region);
  }
}

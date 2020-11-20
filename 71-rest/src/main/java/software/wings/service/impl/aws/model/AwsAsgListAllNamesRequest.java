package software.wings.service.impl.aws.model;

import static software.wings.service.impl.aws.model.AwsAsgRequest.AwsAsgRequestType.LIST_ALL_ASG_NAMES;

import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsAsgListAllNamesRequest extends AwsAsgRequest {
  @Builder
  public AwsAsgListAllNamesRequest(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    super(awsConfig, encryptionDetails, LIST_ALL_ASG_NAMES, region);
  }
}

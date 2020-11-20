package software.wings.service.impl.aws.model;

import static software.wings.service.impl.aws.model.AwsAsgRequest.AwsAsgRequestType.GET_RUNNING_COUNT;

import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsAsgGetRunningCountRequest extends AwsAsgRequest {
  private String infraMappingId;

  @Builder
  public AwsAsgGetRunningCountRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String infraMappingId) {
    super(awsConfig, encryptionDetails, GET_RUNNING_COUNT, region);
    this.infraMappingId = infraMappingId;
  }
}

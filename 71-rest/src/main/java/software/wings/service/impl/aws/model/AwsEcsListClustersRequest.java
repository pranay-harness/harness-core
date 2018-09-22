package software.wings.service.impl.aws.model;

import static software.wings.service.impl.aws.model.AwsEcsRequest.AwsEcsRequestType.LIST_CLUSTERS;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsEcsListClustersRequest extends AwsEcsRequest {
  @Builder
  public AwsEcsListClustersRequest(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    super(awsConfig, encryptionDetails, LIST_CLUSTERS, region);
  }
}
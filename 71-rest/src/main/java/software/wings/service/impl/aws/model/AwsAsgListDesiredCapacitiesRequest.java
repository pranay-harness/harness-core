package software.wings.service.impl.aws.model;

import static software.wings.service.impl.aws.model.AwsAsgRequest.AwsAsgRequestType.LIST_DESIRED_CAPACITIES;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsAsgListDesiredCapacitiesRequest extends AwsAsgRequest {
  private List<String> asgs;

  @Builder
  public AwsAsgListDesiredCapacitiesRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<String> asgs) {
    super(awsConfig, encryptionDetails, LIST_DESIRED_CAPACITIES, region);
    this.asgs = asgs;
  }
}
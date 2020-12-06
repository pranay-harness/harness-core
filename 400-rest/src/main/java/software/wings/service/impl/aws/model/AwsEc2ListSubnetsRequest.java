package software.wings.service.impl.aws.model;

import static software.wings.service.impl.aws.model.AwsEc2Request.AwsEc2RequestType.LIST_SUBNETS;

import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsEc2ListSubnetsRequest extends AwsEc2Request {
  private String region;
  private List<String> vpcIds;

  @Builder
  public AwsEc2ListSubnetsRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<String> vpcIds) {
    super(awsConfig, encryptionDetails, LIST_SUBNETS);
    this.region = region;
    this.vpcIds = vpcIds;
  }
}

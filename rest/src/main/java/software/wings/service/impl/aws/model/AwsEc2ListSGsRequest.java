package software.wings.service.impl.aws.model;

import static software.wings.service.impl.aws.model.AwsEc2Request.AwsEc2RequestType.LIST_SGS;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsEc2ListSGsRequest extends AwsEc2Request {
  private String region;
  private List<String> vpcIds;

  @Builder
  public AwsEc2ListSGsRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<String> vpcIds) {
    super(awsConfig, encryptionDetails, LIST_SGS);
    this.region = region;
    this.vpcIds = vpcIds;
  }
}
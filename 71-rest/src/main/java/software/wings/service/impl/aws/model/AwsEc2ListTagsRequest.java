package software.wings.service.impl.aws.model;

import static software.wings.service.impl.aws.model.AwsEc2Request.AwsEc2RequestType.LIST_TAGS;

import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsEc2ListTagsRequest extends AwsEc2Request {
  private String region;
  private String resourceType;

  @Builder
  public AwsEc2ListTagsRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String resourceType) {
    super(awsConfig, encryptionDetails, LIST_TAGS);
    this.region = region;
    this.resourceType = resourceType;
  }
}

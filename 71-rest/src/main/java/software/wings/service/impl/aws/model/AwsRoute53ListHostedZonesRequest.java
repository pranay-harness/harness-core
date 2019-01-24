package software.wings.service.impl.aws.model;

import static software.wings.service.impl.aws.model.AwsRoute53Request.AwsRoute53RequestType.LIST_HOSTED_ZONES;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsRoute53ListHostedZonesRequest extends AwsRoute53Request {
  private String region;

  @Builder
  public AwsRoute53ListHostedZonesRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    super(awsConfig, encryptionDetails, LIST_HOSTED_ZONES);
    this.region = region;
  }
}
package software.wings.service.impl.aws.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsEc2Request extends AwsRequest {
  public enum AwsEc2RequestType {
    VALIDATE_CREDENTIALS,
    LIST_REGIONS,
    LIST_VPCS,
    LIST_SUBNETS,
    LIST_SGS,
    LIST_TAGS,
    LIST_INSTANCES
  }

  @NotNull private AwsEc2RequestType requestType;

  public AwsEc2Request(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, AwsEc2RequestType requestType) {
    super(awsConfig, encryptionDetails);
    this.requestType = requestType;
  }
}
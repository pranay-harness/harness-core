package software.wings.service.impl.aws.model;

import static software.wings.service.impl.aws.model.AwsEc2Request.AwsEc2RequestType.LIST_REGIONS;

import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsEc2ListRegionsRequest extends AwsEc2Request {
  @Builder
  public AwsEc2ListRegionsRequest(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    super(awsConfig, encryptionDetails, LIST_REGIONS);
  }
}

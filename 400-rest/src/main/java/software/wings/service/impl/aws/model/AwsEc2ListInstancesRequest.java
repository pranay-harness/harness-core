package software.wings.service.impl.aws.model;

import static software.wings.service.impl.aws.model.AwsEc2Request.AwsEc2RequestType.LIST_INSTANCES;

import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;

import com.amazonaws.services.ec2.model.Filter;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsEc2ListInstancesRequest extends AwsEc2Request {
  private String region;
  private List<Filter> filters;

  @Builder
  public AwsEc2ListInstancesRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<Filter> filters) {
    super(awsConfig, encryptionDetails, LIST_INSTANCES);
    this.region = region;
    this.filters = filters;
  }
}

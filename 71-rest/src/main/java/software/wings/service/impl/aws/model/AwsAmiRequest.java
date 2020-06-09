package software.wings.service.impl.aws.model;

import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsAmiRequest extends AwsRequest {
  public enum AwsAmiRequestType {
    EXECUTE_AMI_SERVICE_SETUP,
    EXECUTE_AMI_SERVICE_DEPLOY,
    EXECUTE_AMI_SWITCH_ROUTE,
    EXECUTE_AMI_SERVICE_TRAFFIC_SHIFT_ALB_SETUP,
    EXECUTE_AMI_SERVICE_TRAFFIC_SHIFT_ALB_DEPLOY,
    EXECUTE_AMI_SERVICE_TRAFFIC_SHIFT_ALB
  }

  @NotNull private AwsAmiRequestType requestType;
  @NotNull private String region;

  public AwsAmiRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, AwsAmiRequestType requestType, String region) {
    super(awsConfig, encryptionDetails);
    this.requestType = requestType;
    this.region = region;
  }
}
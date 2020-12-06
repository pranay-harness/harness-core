package software.wings.service.impl.aws.model;

import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsElbRequest extends AwsRequest {
  public enum AwsElbRequestType {
    LIST_CLASSIC_ELBS,
    LIST_APPLICATION_LBS,
    LIST_TARGET_GROUPS_FOR_ALBS,
    LIST_NETWORK_LBS,
    LIST_ELB_LBS,
    LIST_LISTENER_FOR_ELB
  }
  @NotNull private AwsElbRequestType requestType;
  @NotNull private String region;

  public AwsElbRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, AwsElbRequestType requestType, String region) {
    super(awsConfig, encryptionDetails);
    this.requestType = requestType;
    this.region = region;
  }
}

package software.wings.service.impl.aws.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsElbRequest extends AwsRequest {
  public enum AwsElbRequestType {
    LIST_CLASSIC_ELBS,
    LIST_APPLICATION_LBS,
    LIST_TARGET_GROUPS_FOR_ALBS,
    LIST_NETWORK_LBS,
    LIST_ELB_LBS
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
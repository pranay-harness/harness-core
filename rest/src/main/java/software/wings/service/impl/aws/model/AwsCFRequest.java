package software.wings.service.impl.aws.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsCFRequest extends AwsRequest {
  public enum AwsCFRequestType { GET_TEMPLATE_PARAMETERS }

  @NotNull private String region;
  @NotNull private AwsCFRequestType requestType;

  public AwsCFRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, AwsCFRequestType requestType, String region) {
    super(awsConfig, encryptionDetails);
    this.requestType = requestType;
    this.region = region;
  }
}
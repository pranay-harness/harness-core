package software.wings.service.impl.aws.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsRoute53Request extends AwsRequest {
  public enum AwsRoute53RequestType { LIST_HOSTED_ZONES }

  @NotNull private AwsRoute53RequestType requestType;

  public AwsRoute53Request(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, AwsRoute53RequestType requestType) {
    super(awsConfig, encryptionDetails);
    this.requestType = requestType;
  }
}
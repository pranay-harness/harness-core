package software.wings.service.impl.aws.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsLambdaRequest extends AwsRequest {
  public enum AwsLambdaRequestType { EXECUTE_LAMBA_WF, EXECUTE_LAMBDA_FUNCTION }

  @NotNull private AwsLambdaRequestType requestType;
  @NotNull private String region;

  public AwsLambdaRequest(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      AwsLambdaRequestType requestType, String region) {
    super(awsConfig, encryptionDetails);
    this.requestType = requestType;
    this.region = region;
  }
}
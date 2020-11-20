package software.wings.service.impl.aws.model;

import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsLambdaRequest extends AwsRequest {
  public enum AwsLambdaRequestType {
    EXECUTE_LAMBDA_WF,
    EXECUTE_LAMBDA_FUNCTION,
    LIST_LAMBDA_FUNCTION,
    LAMBDA_FUNCTION_DETAILS
  }

  @NotNull private AwsLambdaRequestType requestType;
  @NotNull private String region;

  public AwsLambdaRequest(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      AwsLambdaRequestType requestType, String region) {
    super(awsConfig, encryptionDetails);
    this.requestType = requestType;
    this.region = region;
  }
}

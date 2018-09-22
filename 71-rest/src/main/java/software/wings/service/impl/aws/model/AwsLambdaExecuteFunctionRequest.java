package software.wings.service.impl.aws.model;

import static software.wings.service.impl.aws.model.AwsLambdaRequest.AwsLambdaRequestType.EXECUTE_LAMBDA_FUNCTION;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsLambdaExecuteFunctionRequest extends AwsLambdaRequest {
  private String functionName;
  private String qualifier;
  private String payload;

  @Builder
  public AwsLambdaExecuteFunctionRequest(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String functionName, String qualifier, String logType, String payload) {
    super(awsConfig, encryptionDetails, EXECUTE_LAMBDA_FUNCTION, region);
    this.functionName = functionName;
    this.qualifier = qualifier;
    this.payload = payload;
  }
}
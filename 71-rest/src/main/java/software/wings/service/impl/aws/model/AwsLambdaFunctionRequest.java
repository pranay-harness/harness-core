package software.wings.service.impl.aws.model;

import static software.wings.service.impl.aws.model.AwsLambdaRequest.AwsLambdaRequestType.LIST_LAMBDA_FUNCTION;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

/**
 * Created by Pranjal on 01/29/2019
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AwsLambdaFunctionRequest extends AwsLambdaRequest {
  @Builder
  public AwsLambdaFunctionRequest(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    super(awsConfig, encryptionDetails, LIST_LAMBDA_FUNCTION, region);
  }
}

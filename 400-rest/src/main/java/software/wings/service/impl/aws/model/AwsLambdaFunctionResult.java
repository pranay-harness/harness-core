package software.wings.service.impl.aws.model;

import software.wings.api.AwsLambdaContextElement.FunctionMeta;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsLambdaFunctionResult {
  private boolean success;
  private String errorMessage;
  private FunctionMeta functionMeta;
}

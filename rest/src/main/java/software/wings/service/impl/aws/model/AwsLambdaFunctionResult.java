package software.wings.service.impl.aws.model;

import lombok.Builder;
import lombok.Data;
import software.wings.api.AwsLambdaContextElement.FunctionMeta;

@Data
@Builder
public class AwsLambdaFunctionResult {
  private boolean success;
  private String errorMessage;
  private FunctionMeta functionMeta;
}
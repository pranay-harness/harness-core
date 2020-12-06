package software.wings.beans;

import software.wings.api.AwsLambdaContextElement.FunctionMeta;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsLambdaExecutionSummary {
  private FunctionMeta functionMeta;
  private boolean success;
}

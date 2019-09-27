package software.wings.beans;

import lombok.Builder;
import lombok.Data;
import software.wings.api.AwsLambdaContextElement.FunctionMeta;

@Data
@Builder
public class AwsLambdaExecutionSummary {
  private FunctionMeta functionMeta;
  private boolean success;
}

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import software.wings.api.AwsLambdaContextElement.FunctionMeta;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
public class AwsLambdaExecutionSummary {
  private FunctionMeta functionMeta;
  private boolean success;
}

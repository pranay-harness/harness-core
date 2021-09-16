package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.api.AwsLambdaContextElement.FunctionMeta;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
@TargetModule(HarnessModule._957_CG_BEANS)
public class AwsLambdaExecutionSummary {
  private FunctionMeta functionMeta;
  private boolean success;
}

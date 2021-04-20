package software.wings.service.impl.aws.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class AwsLambdaFunctionParams {
  private String key;
  private String bucket;
  private String functionName;
  private String handler;
  private String runtime;
  private Integer memory;
  private Integer timeout;
  private Map<String, String> functionTags;
}

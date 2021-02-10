package software.wings.service.impl.aws.model;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
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

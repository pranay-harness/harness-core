package software.wings.helpers.ext.cloudformation.response;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.logging.CommandExecutionStatus;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
public class CloudFormationCreateStackResponse extends CloudFormationCommandResponse {
  String stackId;
  Map<String, Object> cloudFormationOutputMap;
  ExistingStackInfo existingStackInfo;
  CloudFormationRollbackInfo rollbackInfo;

  @Builder
  public CloudFormationCreateStackResponse(CommandExecutionStatus commandExecutionStatus, String output,
      Map<String, Object> cloudFormationOutputMap, String stackId, ExistingStackInfo existingStackInfo,
      CloudFormationRollbackInfo rollbackInfo) {
    super(commandExecutionStatus, output);
    this.stackId = stackId;
    this.cloudFormationOutputMap = cloudFormationOutputMap;
    this.existingStackInfo = existingStackInfo;
    this.rollbackInfo = rollbackInfo;
  }
}

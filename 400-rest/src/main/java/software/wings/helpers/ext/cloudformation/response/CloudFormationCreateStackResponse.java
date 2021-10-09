package software.wings.helpers.ext.cloudformation.response;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.logging.CommandExecutionStatus;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class CloudFormationCreateStackResponse extends CloudFormationCommandResponse {
  String stackId;
  Map<String, Object> cloudFormationOutputMap;
  ExistingStackInfo existingStackInfo;
  String stackStatus;
  CloudFormationRollbackInfo rollbackInfo;

  @Builder
  public CloudFormationCreateStackResponse(CommandExecutionStatus commandExecutionStatus, String output,
      Map<String, Object> cloudFormationOutputMap, String stackId, ExistingStackInfo existingStackInfo,
      CloudFormationRollbackInfo rollbackInfo, String stackStatus) {
    super(commandExecutionStatus, output);
    this.stackId = stackId;
    this.cloudFormationOutputMap = cloudFormationOutputMap;
    this.existingStackInfo = existingStackInfo;
    this.rollbackInfo = rollbackInfo;
    this.stackStatus = stackStatus;
  }
}

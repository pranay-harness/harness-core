package software.wings.helpers.ext.cloudformation.response;

import io.harness.logging.CommandExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
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

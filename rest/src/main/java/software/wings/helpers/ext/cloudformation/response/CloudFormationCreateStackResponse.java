package software.wings.helpers.ext.cloudformation.response;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
public class CloudFormationCreateStackResponse extends CloudFormationCommandResponse {
  String stackId;
  Map<String, Object> cloudFormationOutputMap;
  ExistingStackInfo existingStackInfo;

  @Builder
  public CloudFormationCreateStackResponse(CommandExecutionStatus commandExecutionStatus, String output,
      Map<String, Object> cloudFormationOutputMap, String stackId, ExistingStackInfo existingStackInfo) {
    super(commandExecutionStatus, output);
    this.stackId = stackId;
    this.cloudFormationOutputMap = cloudFormationOutputMap;
    this.existingStackInfo = existingStackInfo;
  }
}
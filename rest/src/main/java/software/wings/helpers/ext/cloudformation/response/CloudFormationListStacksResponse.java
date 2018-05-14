package software.wings.helpers.ext.cloudformation.response;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class CloudFormationListStacksResponse extends CloudFormationCommandResponse {
  List<StackSummaryInfo> stackSummaryInfos;

  @Builder
  public CloudFormationListStacksResponse(
      CommandExecutionStatus commandExecutionStatus, String output, List<StackSummaryInfo> stackSummaryInfos) {
    super(commandExecutionStatus, output);
    this.stackSummaryInfos = stackSummaryInfos;
  }
}
package software.wings.helpers.ext.cloudformation.response;

import io.harness.logging.CommandExecutionStatus;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

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

package software.wings.helpers.ext.cloudformation.response;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.logging.CommandExecutionStatus;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
public class CloudFormationListStacksResponse extends CloudFormationCommandResponse {
  List<StackSummaryInfo> stackSummaryInfos;

  @Builder
  public CloudFormationListStacksResponse(
      CommandExecutionStatus commandExecutionStatus, String output, List<StackSummaryInfo> stackSummaryInfos) {
    super(commandExecutionStatus, output);
    this.stackSummaryInfos = stackSummaryInfos;
  }
}

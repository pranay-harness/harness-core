package software.wings.helpers.ext.cloudformation.response;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
public class StackSummaryInfo {
  private String stackId;
  private String stackName;
  private String stackStatus;
  private String stackStatusReason;
}

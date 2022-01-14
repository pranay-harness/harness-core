package software.wings.helpers.ext.ecs.response;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.logging.CommandExecutionStatus;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
public class EcsBGRoute53DNSWeightUpdateResponse extends EcsCommandResponse {
  @Builder
  public EcsBGRoute53DNSWeightUpdateResponse(
      CommandExecutionStatus commandExecutionStatus, String output, boolean timeoutFailure) {
    super(commandExecutionStatus, output, timeoutFailure);
  }
}

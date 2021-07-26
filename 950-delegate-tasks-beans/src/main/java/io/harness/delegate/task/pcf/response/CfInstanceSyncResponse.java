package io.harness.delegate.task.pcf.response;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.task.pcf.CfCommandResponse;
import io.harness.logging.CommandExecutionStatus;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class CfInstanceSyncResponse extends CfCommandResponse {
  private String name;
  private String guid;
  private String organization;
  private String space;
  private List<String> instanceIndices;

  @Builder
  public CfInstanceSyncResponse(CommandExecutionStatus commandExecutionStatus, String output, String name, String guid,
      List<String> instanceIndicesx, String organization, String space) {
    super(commandExecutionStatus, output);
    this.name = name;
    this.guid = guid;
    this.instanceIndices = instanceIndicesx;
    this.organization = organization;
    this.space = space;
  }
}

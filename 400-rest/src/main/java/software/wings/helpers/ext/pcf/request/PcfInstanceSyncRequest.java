package software.wings.helpers.ext.pcf.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.pcf.model.CfCliVersion;

import software.wings.beans.PcfConfig;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class PcfInstanceSyncRequest extends PcfCommandRequest {
  private String pcfApplicationName;

  @Builder
  public PcfInstanceSyncRequest(String accountId, String appId, String commandName, String activityId,
      PcfCommandType pcfCommandType, String organization, String space, PcfConfig pcfConfig, String workflowExecutionId,
      Integer timeoutIntervalInMin, String pcfApplicationName, boolean useCLIForPcfAppCreation, boolean limitPcfThreads,
      boolean ignorePcfConnectionContextCache, CfCliVersion cfCliVersion) {
    super(accountId, appId, commandName, activityId, pcfCommandType, organization, space, pcfConfig,
        workflowExecutionId, timeoutIntervalInMin, useCLIForPcfAppCreation, false, false, limitPcfThreads,
        ignorePcfConnectionContextCache, cfCliVersion);
    this.pcfApplicationName = pcfApplicationName;
  }
}

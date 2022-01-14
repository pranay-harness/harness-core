package io.harness.delegate.task.pcf.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.pcf.CfInternalConfig;
import io.harness.delegate.task.pcf.CfCommandRequest;
import io.harness.pcf.model.CfCliVersion;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@OwnedBy(CDP)
public class CfInstanceSyncRequest extends CfCommandRequest {
  private String pcfApplicationName;

  @Builder
  public CfInstanceSyncRequest(String accountId, String appId, String commandName, String activityId,
      PcfCommandType pcfCommandType, String organization, String space, CfInternalConfig pcfConfig,
      String workflowExecutionId, Integer timeoutIntervalInMin, String pcfApplicationName,
      boolean useCLIForPcfAppCreation, boolean limitPcfThreads, boolean ignorePcfConnectionContextCache,
      CfCliVersion cfCliVersion) {
    super(accountId, appId, commandName, activityId, pcfCommandType, organization, space, pcfConfig,
        workflowExecutionId, timeoutIntervalInMin, useCLIForPcfAppCreation, false, false, limitPcfThreads,
        ignorePcfConnectionContextCache, cfCliVersion);
    this.pcfApplicationName = pcfApplicationName;
  }
}

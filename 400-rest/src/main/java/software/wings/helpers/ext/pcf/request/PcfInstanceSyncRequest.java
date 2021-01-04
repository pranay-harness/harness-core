package software.wings.helpers.ext.pcf.request;

import software.wings.beans.PcfConfig;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class PcfInstanceSyncRequest extends PcfCommandRequest {
  private String pcfApplicationName;

  @Builder
  public PcfInstanceSyncRequest(String accountId, String appId, String commandName, String activityId,
      PcfCommandType pcfCommandType, String organization, String space, PcfConfig pcfConfig, String workflowExecutionId,
      Integer timeoutIntervalInMin, String pcfApplicationName, boolean useCLIForPcfAppCreation, boolean limitPcfThreads,
      boolean ignorePcfConnectionContextCache) {
    super(accountId, appId, commandName, activityId, pcfCommandType, organization, space, pcfConfig,
        workflowExecutionId, timeoutIntervalInMin, useCLIForPcfAppCreation, false, false, limitPcfThreads,
        ignorePcfConnectionContextCache);
    this.pcfApplicationName = pcfApplicationName;
  }
}

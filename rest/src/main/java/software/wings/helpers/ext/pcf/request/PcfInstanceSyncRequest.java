package software.wings.helpers.ext.pcf.request;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.PcfConfig;

@Data
public class PcfInstanceSyncRequest extends PcfCommandRequest {
  private String pcfApplicationName;

  @Builder
  public PcfInstanceSyncRequest(String accountId, String appId, String commandName, String activityId,
      PcfCommandType pcfCommandType, String organization, String space, PcfConfig pcfConfig, String workflowExecutionId,
      Integer timeoutIntervalInMin, String pcfApplicationName) {
    super(accountId, appId, commandName, activityId, pcfCommandType, organization, space, pcfConfig,
        workflowExecutionId, timeoutIntervalInMin, false);
    this.pcfApplicationName = pcfApplicationName;
  }
}

package software.wings.helpers.ext.pcf.request;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.PcfConfig;

@Data
@EqualsAndHashCode(callSuper = false)
public class PcfInstanceSyncRequest extends PcfCommandRequest {
  private String pcfApplicationName;

  @Builder
  public PcfInstanceSyncRequest(String accountId, String appId, String commandName, String activityId,
      PcfCommandType pcfCommandType, String organization, String space, PcfConfig pcfConfig, String workflowExecutionId,
      Integer timeoutIntervalInMin, String pcfApplicationName) {
    super(accountId, appId, commandName, activityId, pcfCommandType, organization, space, pcfConfig,
        workflowExecutionId, timeoutIntervalInMin);
    this.pcfApplicationName = pcfApplicationName;
  }
}

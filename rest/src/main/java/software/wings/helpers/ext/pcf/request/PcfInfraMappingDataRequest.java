package software.wings.helpers.ext.pcf.request;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.PcfConfig;

@Data
@EqualsAndHashCode(callSuper = false)
public class PcfInfraMappingDataRequest extends PcfCommandRequest {
  private PcfConfig pcfConfig;

  @Builder
  public PcfInfraMappingDataRequest(String accountId, String appId, String commandName, String activityId,
      PcfCommandType pcfCommandType, String organization, String space, PcfConfig pcfConfig, String workflowExecutionId,
      Integer timeoutIntervalInMin) {
    super(accountId, appId, commandName, activityId, pcfCommandType, organization, space, pcfConfig,
        workflowExecutionId, timeoutIntervalInMin);
    this.pcfConfig = pcfConfig;
  }
}

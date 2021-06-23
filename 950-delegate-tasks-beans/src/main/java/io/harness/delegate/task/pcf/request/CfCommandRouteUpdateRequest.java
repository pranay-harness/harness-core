package io.harness.delegate.task.pcf.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.pcf.CfInternalConfig;
import io.harness.delegate.beans.pcf.CfRouteUpdateRequestConfigData;
import io.harness.delegate.task.pcf.CfCommandRequest;
import io.harness.pcf.model.CfCliVersion;

import lombok.Builder;
import lombok.Data;

/**
 * This class contains all required data for PCFCommandTask.SETUP to perform setup task
 */
@Data
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class CfCommandRouteUpdateRequest extends CfCommandRequest {
  private CfRouteUpdateRequestConfigData pcfRouteUpdateConfigData;

  @Builder
  public CfCommandRouteUpdateRequest(String accountId, String appId, String commandName, String activityId,
      PcfCommandType pcfCommandType, String organization, String space, CfInternalConfig pcfConfig,
      String workflowExecutionId, Integer timeoutIntervalInMin, CfRouteUpdateRequestConfigData pcfRouteUpdateConfigData,
      boolean useCfCLI, boolean useAppAutoscalar, boolean enforceSslValidation, boolean limitPcfThreads,
      boolean ignorePcfConnectionContextCache, CfCliVersion cfCliVersion) {
    super(accountId, appId, commandName, activityId, pcfCommandType, organization, space, pcfConfig,
        workflowExecutionId, timeoutIntervalInMin, useCfCLI, enforceSslValidation, useAppAutoscalar, limitPcfThreads,
        ignorePcfConnectionContextCache, cfCliVersion);
    this.pcfRouteUpdateConfigData = pcfRouteUpdateConfigData;
  }
}

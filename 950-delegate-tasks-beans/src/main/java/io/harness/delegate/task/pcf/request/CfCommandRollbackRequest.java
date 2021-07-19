package io.harness.delegate.task.pcf.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.pcf.CfAppSetupTimeDetails;
import io.harness.delegate.beans.pcf.CfInternalConfig;
import io.harness.delegate.beans.pcf.CfServiceData;
import io.harness.delegate.beans.pcf.ResizeStrategy;
import io.harness.delegate.task.pcf.CfCommandRequest;
import io.harness.pcf.model.CfCliVersion;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * This class contains all required data for PCFCommandTask.DEPLOY to perform setup task
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class CfCommandRollbackRequest extends CfCommandRequest {
  private List<CfServiceData> instanceData;
  private List<String> routeMaps;
  private List<String> tempRouteMaps;
  private ResizeStrategy resizeStrategy;
  private List<CfAppSetupTimeDetails> appsToBeDownSized;
  private CfAppSetupTimeDetails newApplicationDetails;
  private boolean isStandardBlueGreenWorkflow;
  private boolean versioningChanged;
  private boolean nonVersioning;
  private String cfAppNamePrefix;
  private CfAppSetupTimeDetails existingInActiveApplicationDetails;
  private Integer activeAppRevision;

  @Builder
  public CfCommandRollbackRequest(String accountId, String appId, String commandName, String activityId,
      PcfCommandType pcfCommandType, String organization, String space, CfInternalConfig pcfConfig,
      String workflowExecutionId, List<CfServiceData> instanceData, ResizeStrategy resizeStrategy,
      List<String> routeMaps, List<String> tempRouteMaps, Integer timeoutIntervalInMin,
      List<CfAppSetupTimeDetails> appsToBeDownSized, CfAppSetupTimeDetails newApplicationDetails,
      boolean isStandardBlueGreenWorkflow, boolean useCfCLI, boolean useAppAutoscalar, boolean enforceSslValidation,
      boolean limitPcfThreads, boolean ignorePcfConnectionContextCache, CfCliVersion cfCliVersion,
      boolean versioningChanged, boolean nonVersioning, String cfAppNamePrefix,
      CfAppSetupTimeDetails existingInActiveApplicationDetails, Integer activeAppRevision) {
    super(accountId, appId, commandName, activityId, pcfCommandType, organization, space, pcfConfig,
        workflowExecutionId, timeoutIntervalInMin, useCfCLI, enforceSslValidation, useAppAutoscalar, limitPcfThreads,
        ignorePcfConnectionContextCache, cfCliVersion);
    this.instanceData = instanceData;
    this.resizeStrategy = resizeStrategy;
    this.routeMaps = routeMaps;
    this.tempRouteMaps = tempRouteMaps;
    this.appsToBeDownSized = appsToBeDownSized;
    this.newApplicationDetails = newApplicationDetails;
    this.isStandardBlueGreenWorkflow = isStandardBlueGreenWorkflow;
    this.versioningChanged = versioningChanged;
    this.nonVersioning = nonVersioning;
    this.cfAppNamePrefix = cfAppNamePrefix;
    this.existingInActiveApplicationDetails = existingInActiveApplicationDetails;
    this.activeAppRevision = activeAppRevision;
  }
}

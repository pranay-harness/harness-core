package software.wings.sm.states.pcf;

import software.wings.api.pcf.PcfDeployContextElement;
import software.wings.api.pcf.PcfDeployStateExecutionData;
import software.wings.api.pcf.PcfSetupContextElement;
import software.wings.beans.Application;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.PcfConfig;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest.PcfCommandType;
import software.wings.helpers.ext.pcf.request.PcfCommandRollbackRequest;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;

import java.util.ArrayList;

public class PcfRollbackState extends PcfDeployState {
  /**
   * Instantiates a new state.
   *
   * @param name the name
   */
  public PcfRollbackState(String name) {
    super(name, StateType.PCF_ROLLBACK.name());
  }

  @Override
  public PcfCommandRequest getPcfCommandRequest(ExecutionContext context, Application application, String activityId,
      PcfSetupContextElement pcfSetupContextElement, PcfConfig pcfConfig, Integer updateCount,
      Integer downsizeUpdateCount, PcfDeployStateExecutionData stateExecutionData,
      PcfInfrastructureMapping infrastructureMapping) {
    PcfDeployContextElement pcfDeployContextElement = context.getContextElement(ContextElementType.PCF_SERVICE_DEPLOY);

    // Just revert previousCount and desiredCount values for Rollback
    // Deploy sends emptyInstanceData and PcfCommandTask figured out which apps to be resized,
    // in case of rollback, we send InstanceData mentioning apps and their reset counts
    StringBuilder updateDetails = new StringBuilder();
    if (pcfDeployContextElement.getInstanceData() != null) {
      pcfDeployContextElement.getInstanceData().forEach(pcfServiceData -> {
        Integer temp = pcfServiceData.getDesiredCount();
        pcfServiceData.setDesiredCount(pcfServiceData.getPreviousCount());
        pcfServiceData.setPreviousCount(temp);
        updateDetails.append(new StringBuilder()
                                 .append("App Name: ")
                                 .append(pcfServiceData.getName())
                                 .append(", DesiredCount: ")
                                 .append(pcfServiceData.getDesiredCount())
                                 .append("}\n")
                                 .toString());
      });
    } else {
      pcfDeployContextElement.setInstanceData(new ArrayList<>());
    }

    stateExecutionData.setUpdateDetails(updateDetails.toString());
    stateExecutionData.setActivityId(activityId);

    return PcfCommandRollbackRequest.builder()
        .activityId(activityId)
        .commandName(PCF_RESIZE_COMMAND)
        .workflowExecutionId(context.getWorkflowExecutionId())
        .organization(pcfSetupContextElement.getPcfCommandRequest().getOrganization())
        .space(pcfSetupContextElement.getPcfCommandRequest().getSpace())
        .resizeStrategy(pcfSetupContextElement.getResizeStrategy())
        .routeMaps(infrastructureMapping.getRouteMaps())
        .tempRouteMaps(infrastructureMapping.getTempRouteMap())
        .pcfConfig(pcfConfig)
        .pcfCommandType(PcfCommandType.ROLLBACK)
        .instanceData(pcfDeployContextElement.getInstanceData())
        .appId(application.getUuid())
        .accountId(application.getAccountId())
        .timeoutIntervalInMin(pcfSetupContextElement.getTimeoutIntervalInMinutes())
        .appsToBeDownSized(pcfSetupContextElement.getAppDetailsToBeDownsized())
        .newApplicationDetails(pcfSetupContextElement.getNewPcfApplicationDetails())
        .isStandardBlueGreenWorkflow(pcfSetupContextElement.isStandardBlueGreenWorkflow())
        .build();
  }

  @Override
  public Integer getInstanceCount() {
    return 0;
  }

  @Override
  public InstanceUnitType getInstanceUnitType() {
    return null;
  }

  @Override
  public Integer getDownsizeInstanceCount() {
    return null;
  }

  @Override
  public InstanceUnitType getDownsizeInstanceUnitType() {
    return null;
  }

  @Override
  public Integer getUpsizeUpdateCount(PcfSetupContextElement pcfSetupContextElement) {
    return -1;
  }

  @Override
  public Integer getDownsizeUpdateCount(Integer updateCount, PcfSetupContextElement pcfSetupContextElement) {
    return -1;
  }
}

package software.wings.sm.states.pcf;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.Collections.emptyMap;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.StringUtils;
import org.simpleframework.xml.Transient;
import software.wings.api.pcf.DeploySweepingOutputPcf;
import software.wings.api.pcf.PcfDeployStateExecutionData;
import software.wings.api.pcf.PcfServiceData;
import software.wings.api.pcf.SetupSweepingOutputPcf;
import software.wings.beans.Application;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.PcfConfig;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest.PcfCommandType;
import software.wings.helpers.ext.pcf.request.PcfCommandRollbackRequest;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PcfRollbackState extends PcfDeployState {
  @Inject @Transient private SweepingOutputService sweepingOutputService;
  @Inject @Transient private PcfStateHelper pcfStateHelper;
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
      SetupSweepingOutputPcf setupSweepingOutputPcf, PcfConfig pcfConfig, Integer updateCount,
      Integer downsizeUpdateCount, PcfDeployStateExecutionData stateExecutionData,
      PcfInfrastructureMapping infrastructureMapping) {
    DeploySweepingOutputPcf deploySweepingOutputPcf =
        sweepingOutputService.findSweepingOutput(context.prepareSweepingOutputInquiryBuilder()
                                                     .name(pcfStateHelper.obtainDeploySweepingOutputName(context, true))
                                                     .build());

    // Just revert previousCount and desiredCount values for Rollback
    // Deploy sends emptyInstanceData and PcfCommandTask figured out which apps to be resized,
    // in case of rollback, we send InstanceData mentioning apps and their reset counts
    StringBuilder updateDetails = new StringBuilder();
    List<PcfServiceData> instanceData = new ArrayList<>();
    if (deploySweepingOutputPcf != null && deploySweepingOutputPcf.getInstanceData() != null) {
      deploySweepingOutputPcf.getInstanceData().forEach(pcfServiceData -> {
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

        instanceData.add(pcfServiceData);
      });
    }

    stateExecutionData.setUpdateDetails(updateDetails.toString());
    stateExecutionData.setActivityId(activityId);

    return PcfCommandRollbackRequest.builder()
        .activityId(activityId)
        .commandName(PCF_RESIZE_COMMAND)
        .workflowExecutionId(context.getWorkflowExecutionId())
        .organization(getOrgFromContext(setupSweepingOutputPcf))
        .space(fetchSpaceFromContext(setupSweepingOutputPcf))
        .resizeStrategy(setupSweepingOutputPcf.getResizeStrategy())
        .routeMaps(infrastructureMapping.getRouteMaps())
        .tempRouteMaps(infrastructureMapping.getTempRouteMap())
        .pcfConfig(pcfConfig)
        .pcfCommandType(PcfCommandType.ROLLBACK)
        .instanceData(instanceData)
        .appId(application.getUuid())
        .accountId(application.getAccountId())
        .timeoutIntervalInMin(firstNonNull(setupSweepingOutputPcf.getTimeoutIntervalInMinutes(), Integer.valueOf(5)))
        .appsToBeDownSized(setupSweepingOutputPcf.getAppDetailsToBeDownsized())
        .newApplicationDetails(setupSweepingOutputPcf.getNewPcfApplicationDetails())
        .isStandardBlueGreenWorkflow(setupSweepingOutputPcf.isStandardBlueGreenWorkflow())
        .enforceSslValidation(setupSweepingOutputPcf.isEnforceSslValidation())
        .useAppAutoscalar(setupSweepingOutputPcf.isUseAppAutoscalar())
        .useCfCLI(true)
        .build();
  }

  private String fetchSpaceFromContext(SetupSweepingOutputPcf setupSweepingOutputPcf) {
    if (setupSweepingOutputPcf == null || setupSweepingOutputPcf.getPcfCommandRequest() == null) {
      return StringUtils.EMPTY;
    }

    return setupSweepingOutputPcf.getPcfCommandRequest().getSpace();
  }

  private String getOrgFromContext(SetupSweepingOutputPcf setupSweepingOutputPcf) {
    if (setupSweepingOutputPcf == null || setupSweepingOutputPcf.getPcfCommandRequest() == null) {
      return StringUtils.EMPTY;
    }

    return setupSweepingOutputPcf.getPcfCommandRequest().getOrganization();
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
  public Integer getUpsizeUpdateCount(SetupSweepingOutputPcf setupSweepingOutputPcf) {
    return -1;
  }

  @Override
  public Integer getDownsizeUpdateCount(SetupSweepingOutputPcf setupSweepingOutputPcf) {
    return -1;
  }

  @Override
  public Map<String, String> validateFields() {
    return emptyMap();
  }
}

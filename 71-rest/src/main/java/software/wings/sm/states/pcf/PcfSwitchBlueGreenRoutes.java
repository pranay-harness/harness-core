package software.wings.sm.states.pcf;

import static com.google.common.base.MoreObjects.firstNonNull;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.SweepingOutputInstance;
import io.harness.beans.SweepingOutputInstance.Scope;
import io.harness.context.ContextElementType;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.pcf.PcfRouteUpdateStateExecutionData;
import software.wings.api.pcf.SetupSweepingOutputPcf;
import software.wings.api.pcf.SwapRouteRollbackSweepingOutputPcf;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.PcfConfig;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.helpers.ext.pcf.request.PcfRouteUpdateRequestConfigData;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PcfSwitchBlueGreenRoutes extends State {
  @Inject private transient AppService appService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient SecretManager secretManager;
  @Inject private transient SettingsService settingsService;
  @Inject private transient ActivityService activityService;
  @Inject private transient PcfStateHelper pcfStateHelper;
  @Inject private transient SweepingOutputService sweepingOutputService;
  @Inject protected transient FeatureFlagService featureFlagService;

  public static final String PCF_BG_SWAP_ROUTE_COMMAND = "PCF BG Swap Route";
  static final String PCF_BG_SKIP_SWAP_ROUTE_MESG = "Skipping route swapping";

  @Attributes(title = "Downsize Old Applications") private boolean downsizeOldApps;

  public boolean isDownsizeOldApps() {
    return downsizeOldApps;
  }

  public void setDownsizeOldApps(boolean downsizeOldApps) {
    this.downsizeOldApps = downsizeOldApps;
  }

  /**
   * Instantiates a new state.
   *
   * @param name      the name
   */
  public PcfSwitchBlueGreenRoutes(String name) {
    super(name, StateType.PCF_BG_MAP_ROUTE.name());
  }

  public PcfSwitchBlueGreenRoutes(String name, String stateType) {
    super(name, stateType);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      return executeInternal(context);
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  protected ExecutionResponse executeInternal(ExecutionContext context) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Application app = appService.get(context.getAppId());
    Environment env = workflowStandardParams.getEnv();
    notNullCheck("Environment does not exist", env, USER);

    PcfInfrastructureMapping pcfInfrastructureMapping =
        (PcfInfrastructureMapping) infrastructureMappingService.get(app.getUuid(), context.fetchInfraMappingId());

    SetupSweepingOutputPcf setupSweepingOutputPcf = pcfStateHelper.findSetupSweepingOutputPcf(context, isRollback());
    pcfStateHelper.populatePcfVariables(context, setupSweepingOutputPcf);
    PcfRouteUpdateRequestConfigData requestConfigData = getPcfRouteUpdateRequestConfigData(setupSweepingOutputPcf);
    Activity activity = createActivity(context);

    if (isRollback()) {
      if (pcfStateHelper.isRollBackNotNeeded(setupSweepingOutputPcf)) {
        return pcfStateHelper.handleRollbackSkipped(
            context.getAppId(), activity.getUuid(), PCF_BG_SWAP_ROUTE_COMMAND, PCF_BG_SKIP_SWAP_ROUTE_MESG);
      }
      SweepingOutputInstance sweepingOutputInstance =
          sweepingOutputService.find(context.prepareSweepingOutputInquiryBuilder()
                                         .name(pcfStateHelper.obtainSwapRouteSweepingOutputName(context, true))
                                         .build());
      if (sweepingOutputInstance != null) {
        SwapRouteRollbackSweepingOutputPcf swapRouteRollbackSweepingOutputPcf =
            (SwapRouteRollbackSweepingOutputPcf) sweepingOutputInstance.getValue();
        // it means no update route happened.
        if (swapRouteRollbackSweepingOutputPcf.getPcfRouteUpdateRequestConfigData() != null) {
          downsizeOldApps =
              swapRouteRollbackSweepingOutputPcf.getPcfRouteUpdateRequestConfigData().isDownsizeOldApplication();
        }
      }
      requestConfigData.setSkipRollback(sweepingOutputInstance == null);
    }

    SettingAttribute settingAttribute = settingsService.get(pcfInfrastructureMapping.getComputeProviderSettingId());
    PcfConfig pcfConfig = (PcfConfig) settingAttribute.getValue();

    List<EncryptedDataDetail> encryptedDataDetails = secretManager.getEncryptionDetails(
        (EncryptableSetting) pcfConfig, context.getAppId(), context.getWorkflowExecutionId());

    // update value as for rollback, we need to readt it from SweepingOutput
    requestConfigData.setDownsizeOldApplication(downsizeOldApps);
    return pcfStateHelper.queueDelegateTaskForRouteUpdate(
        PcfRouteUpdateQueueRequestData.builder()
            .pcfConfig(pcfConfig)
            .app(app)
            .pcfInfrastructureMapping(pcfInfrastructureMapping)
            .activityId(activity.getUuid())
            .envId(env.getUuid())
            .timeoutIntervalInMinutes(firstNonNull(setupSweepingOutputPcf.getTimeoutIntervalInMinutes(), 5))
            .commandName(PCF_BG_SWAP_ROUTE_COMMAND)
            .requestConfigData(requestConfigData)
            .encryptedDataDetails(encryptedDataDetails)
            .downsizeOldApps(downsizeOldApps)
            .useCfCli(true)
            .build(),
        setupSweepingOutputPcf);
  }

  private PcfRouteUpdateRequestConfigData getPcfRouteUpdateRequestConfigData(
      SetupSweepingOutputPcf setupSweepingOutputPcf) {
    List<String> existingAppNames;

    if (setupSweepingOutputPcf != null
        && EmptyPredicate.isNotEmpty(setupSweepingOutputPcf.getAppDetailsToBeDownsized())) {
      existingAppNames = setupSweepingOutputPcf.getAppDetailsToBeDownsized()
                             .stream()
                             .map(PcfAppSetupTimeDetails::getApplicationName)
                             .collect(toList());
    } else {
      existingAppNames = emptyList();
    }

    return PcfRouteUpdateRequestConfigData.builder()
        .newApplicatiaonName(getNewApplicationName(setupSweepingOutputPcf))
        .existingApplicationDetails(
            setupSweepingOutputPcf != null ? setupSweepingOutputPcf.getAppDetailsToBeDownsized() : null)
        .existingApplicationNames(existingAppNames)
        .tempRoutes(setupSweepingOutputPcf != null ? setupSweepingOutputPcf.getTempRouteMap() : emptyList())
        .finalRoutes(setupSweepingOutputPcf != null ? setupSweepingOutputPcf.getRouteMaps() : emptyList())
        .isRollback(isRollback())
        .isStandardBlueGreen(true)
        .downsizeOldApplication(downsizeOldApps)
        .build();
  }

  @VisibleForTesting
  String getNewApplicationName(SetupSweepingOutputPcf setupSweepingOutputPcf) {
    String name = EMPTY;
    if (setupSweepingOutputPcf != null && setupSweepingOutputPcf.getNewPcfApplicationDetails() != null) {
      name = setupSweepingOutputPcf.getNewPcfApplicationDetails().getApplicationName();
    }

    return name;
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      String activityId = response.keySet().iterator().next();
      PcfCommandExecutionResponse executionResponse = (PcfCommandExecutionResponse) response.values().iterator().next();
      ExecutionStatus executionStatus = executionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS
          ? ExecutionStatus.SUCCESS
          : ExecutionStatus.FAILED;
      activityService.updateStatus(activityId, context.getAppId(), executionStatus);

      // update PcfDeployStateExecutionData,
      PcfRouteUpdateStateExecutionData stateExecutionData =
          (PcfRouteUpdateStateExecutionData) context.getStateExecutionData();
      stateExecutionData.setStatus(executionStatus);
      stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());
      if (!isRollback()) {
        sweepingOutputService.save(
            context.prepareSweepingOutputBuilder(Scope.WORKFLOW)
                .name(pcfStateHelper.obtainSwapRouteSweepingOutputName(context, false))
                .value(SwapRouteRollbackSweepingOutputPcf.builder()
                           .pcfRouteUpdateRequestConfigData(stateExecutionData.getPcfRouteUpdateRequestConfigData())
                           .build())
                .build());
      }

      return ExecutionResponse.builder()
          .executionStatus(executionStatus)
          .errorMessage(executionResponse.getErrorMessage())
          .stateExecutionData(stateExecutionData)
          .build();

    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  protected Activity createActivity(ExecutionContext executionContext) {
    return pcfStateHelper.createActivity(executionContext, PCF_BG_SWAP_ROUTE_COMMAND, getStateType(),
        CommandUnitType.PCF_BG_SWAP_ROUTE, activityService);
  }
}

package software.wings.sm.states.pcf;

import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.data.structure.EmptyPredicate;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.api.PhaseElement;
import software.wings.api.pcf.PcfRouteUpdateStateExecutionData;
import software.wings.api.pcf.PcfSetupContextElement;
import software.wings.api.pcf.PcfSwapRouteRollbackContextElement;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.PcfConfig;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.common.Constants;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.pcf.request.PcfRouteUpdateRequestConfigData;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PcfSwitchBlueGreenRoutes extends State {
  @Inject private transient AppService appService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient DelegateService delegateService;
  @Inject private transient SecretManager secretManager;
  @Inject private transient SettingsService settingsService;
  @Inject private transient ActivityService activityService;
  @Inject private transient PcfStateHelper pcfStateHelper;
  @Inject @Transient protected transient LogService logService;

  public static final String PCF_BG_SWAP_ROUTE_COMMAND = "PCF BG Swap Route";

  private static final Logger logger = LoggerFactory.getLogger(PcfSwitchBlueGreenRoutes.class);

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
      throw new InvalidRequestException(Misc.getMessage(e), e);
    }
  }

  @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
  protected ExecutionResponse executeInternal(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Application app = appService.get(context.getAppId());
    Environment env = workflowStandardParams.getEnv();

    PcfInfrastructureMapping pcfInfrastructureMapping =
        (PcfInfrastructureMapping) infrastructureMappingService.get(app.getUuid(), phaseElement.getInfraMappingId());

    PcfSetupContextElement pcfSetupContextElement =
        context.<PcfSetupContextElement>getContextElementList(ContextElementType.PCF_SERVICE_SETUP)
            .stream()
            .filter(cse -> phaseElement.getInfraMappingId().equals(cse.getInfraMappingId()))
            .findFirst()
            .orElse(PcfSetupContextElement.builder().build());

    Activity activity = createActivity(context);
    SettingAttribute settingAttribute = settingsService.get(pcfInfrastructureMapping.getComputeProviderSettingId());
    PcfConfig pcfConfig = (PcfConfig) settingAttribute.getValue();

    List<EncryptedDataDetail> encryptedDataDetails = secretManager.getEncryptionDetails(
        (Encryptable) pcfConfig, context.getAppId(), context.getWorkflowExecutionId());

    PcfRouteUpdateRequestConfigData requestConfigData = null;
    if (isRollback()) {
      PcfSwapRouteRollbackContextElement pcfSwapRouteRollbackContextElement =
          context.getContextElement(ContextElementType.PCF_ROUTE_SWAP_ROLLBACK);
      requestConfigData = pcfSwapRouteRollbackContextElement.getPcfRouteUpdateRequestConfigData();
      requestConfigData.setRollback(true);
    } else {
      requestConfigData = getPcfRouteUpdateRequestConfigData(pcfSetupContextElement);
    }

    return pcfStateHelper.queueDelegateTaskForRouteUpdate(app, pcfConfig, delegateService, pcfInfrastructureMapping,
        activity.getUuid(), env.getUuid(), pcfSetupContextElement.getTimeoutIntervalInMinutes(),
        PCF_BG_SWAP_ROUTE_COMMAND, requestConfigData, encryptedDataDetails);
  }

  private PcfRouteUpdateRequestConfigData getPcfRouteUpdateRequestConfigData(
      PcfSetupContextElement pcfSetupContextElement) {
    List<String> existingAppNames;

    if (EmptyPredicate.isNotEmpty(pcfSetupContextElement.getAppsToBeDownsized())) {
      existingAppNames =
          pcfSetupContextElement.getAppsToBeDownsized().stream().map(app -> app.getApplicationName()).collect(toList());
    } else {
      existingAppNames = Collections.EMPTY_LIST;
    }

    return PcfRouteUpdateRequestConfigData.builder()
        .newApplicatiaonName(pcfSetupContextElement.getNewPcfApplicationDetails().getApplicationName())
        .existingApplicationDetails(pcfSetupContextElement.getAppsToBeDownsized())
        .existingApplicationNames(existingAppNames)
        .tempRoutes(pcfSetupContextElement.getTempRouteMap())
        .finalRoutes(pcfSetupContextElement.getRouteMaps())
        .isRollback(isRollback())
        .isStandardBlueGreen(true)
        .downsizeOldApplication(downsizeOldApps)
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    try {
      String activityId = response.keySet().iterator().next();
      PcfCommandExecutionResponse executionResponse = (PcfCommandExecutionResponse) response.values().iterator().next();
      ExecutionStatus executionStatus =
          executionResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS) ? ExecutionStatus.SUCCESS
                                                                                               : ExecutionStatus.FAILED;
      activityService.updateStatus(activityId, context.getAppId(), executionStatus);

      // update PcfDeployStateExecutionData,
      PcfRouteUpdateStateExecutionData stateExecutionData =
          (PcfRouteUpdateStateExecutionData) context.getStateExecutionData();
      stateExecutionData.setStatus(executionStatus);
      stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());

      return ExecutionResponse.Builder.anExecutionResponse()
          .withExecutionStatus(executionStatus)
          .withErrorMessage(executionResponse.getErrorMessage())
          .withStateExecutionData(stateExecutionData)
          .build();

    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(Misc.getMessage(e), e);
    }
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  protected Activity createActivity(ExecutionContext executionContext) {
    return pcfStateHelper.createActivity(executionContext, PCF_BG_SWAP_ROUTE_COMMAND, getStateType(),
        CommandUnitType.PCF_BG_SWAP_ROUTE, activityService);
  }
}

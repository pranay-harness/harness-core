package software.wings.sm.states.pcf;

import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import org.mongodb.morphia.annotations.Transient;
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
import software.wings.helpers.ext.pcf.response.PcfDeployCommandResponse;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MapRouteState extends State {
  public static final String PCF_APP_NAME = "pcfAppName";
  public static final String INFRA_ROUTE = "infra.route";

  @Inject private transient AppService appService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient SecretManager secretManager;
  @Inject private transient SettingsService settingsService;
  @Inject private transient ActivityService activityService;
  @Inject private transient PcfStateHelper pcfStateHelper;
  @Inject private transient SweepingOutputService sweepingOutputService;
  @Inject @Transient protected transient LogService logService;

  public static final String PCF_MAP_ROUTE_COMMAND = "PCF Map Route";

  @DefaultValue("${" + PCF_APP_NAME + "}") @Attributes(title = "PCF App Name") private String pcfAppName;

  @DefaultValue("${" + WorkflowServiceHelper.INFRA_ROUTE_PCF + "}")
  @Attributes(title = "Map Route")
  private String route;

  public String getPcfAppName() {
    return pcfAppName;
  }

  public void setPcfAppName(String pcfAppName) {
    this.pcfAppName = pcfAppName;
  }

  public String getRoute() {
    return route;
  }

  public void setRoute(String route) {
    this.route = route;
  }

  /**
   * Instantiates a new state.
   *
   * @param name      the name
   */
  public MapRouteState(String name) {
    super(name, StateType.PCF_MAP_ROUTE.name());
  }

  public MapRouteState(String name, String stateType) {
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
    Application application = appService.get(context.getAppId());
    Environment environment = workflowStandardParams.getEnv();
    notNullCheck("Environment does not exist", environment, USER);

    PcfInfrastructureMapping infrastructureMapping = (PcfInfrastructureMapping) infrastructureMappingService.get(
        application.getUuid(), context.fetchInfraMappingId());

    SetupSweepingOutputPcf setupSweepingOutputPcf = pcfStateHelper.findSetupSweepingOutputPcf(context, isRollback());
    pcfStateHelper.populatePcfVariables(context, setupSweepingOutputPcf);

    Activity activity = createActivity(context);
    SettingAttribute settingAttribute = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    PcfConfig pcfConfig = (PcfConfig) settingAttribute.getValue();

    List<EncryptedDataDetail> encryptedDetails = secretManager.getEncryptionDetails(
        (EncryptableSetting) pcfConfig, context.getAppId(), context.getWorkflowExecutionId());

    PcfRouteUpdateRequestConfigData requestConfigData = null;
    if (isRollback()) {
      SwapRouteRollbackSweepingOutputPcf swapRouteRollbackSweepingOutputPcf =
          (SwapRouteRollbackSweepingOutputPcf) sweepingOutputService.findSweepingOutput(
              context.prepareSweepingOutputInquiryBuilder()
                  .name(pcfStateHelper.obtainSwapRouteSweepingOutputName(context, true))
                  .build());
      requestConfigData = swapRouteRollbackSweepingOutputPcf.getPcfRouteUpdateRequestConfigData();
      requestConfigData.setRollback(true);
      requestConfigData.setMapRoutesOperation(!requestConfigData.isMapRoutesOperation());
    } else {
      requestConfigData = getPcfRouteUpdateRequestConfigData(setupSweepingOutputPcf, infrastructureMapping);
    }

    return pcfStateHelper.queueDelegateTaskForRouteUpdate(
        PcfRouteUpdateQueueRequestData.builder()
            .app(application)
            .pcfConfig(pcfConfig)
            .pcfInfrastructureMapping(infrastructureMapping)
            .activityId(activity.getUuid())
            .envId(environment.getUuid())
            .timeoutIntervalInMinutes(setupSweepingOutputPcf.getTimeoutIntervalInMinutes())
            .commandName(PCF_MAP_ROUTE_COMMAND)
            .requestConfigData(requestConfigData)
            .encryptedDataDetails(encryptedDetails)
            .build(),
        setupSweepingOutputPcf);
  }

  private PcfRouteUpdateRequestConfigData getPcfRouteUpdateRequestConfigData(
      SetupSweepingOutputPcf setupSweepingOutputPcf, PcfInfrastructureMapping infrastructureMapping) {
    return PcfRouteUpdateRequestConfigData.builder()
        .existingApplicationNames(getApplicationNamesTobeUpdated(setupSweepingOutputPcf))
        .finalRoutes(getRoutes(setupSweepingOutputPcf))
        .isRollback(false)
        .isStandardBlueGreen(false)
        .isMapRoutesOperation(checkIfMapRouteOperation())
        .build();
  }

  private List<String> getRoutes(SetupSweepingOutputPcf setupSweepingOutputPcf) {
    // determine which routes to map
    boolean isOriginalRoute = false;
    String infraRouteConst = "${" + INFRA_ROUTE + "}";
    String infraRouteConstLegacy = "${" + WorkflowServiceHelper.INFRA_ROUTE_PCF + "}";
    if (route == null || infraRouteConst.equalsIgnoreCase(route.trim())
        || infraRouteConstLegacy.equalsIgnoreCase(route.trim())) {
      isOriginalRoute = true;
    }

    List<String> routes =
        isOriginalRoute ? setupSweepingOutputPcf.getRouteMaps() : setupSweepingOutputPcf.getTempRouteMap();
    if (routes == null) {
      routes = Collections.EMPTY_LIST;
    }

    return routes;
  }

  private List<String> getApplicationNamesTobeUpdated(SetupSweepingOutputPcf setupSweepingOutputPcf) {
    String appConst = "${" + PCF_APP_NAME + "}";
    boolean isNewApplication = pcfAppName == null || appConst.equalsIgnoreCase(pcfAppName.trim());

    List<String> appNames = new ArrayList<>();

    if (isNewApplication) {
      appNames.add(setupSweepingOutputPcf.getNewPcfApplicationDetails().getApplicationName());
    } else {
      appNames.addAll(setupSweepingOutputPcf.getAppDetailsToBeDownsized() == null
              ? Collections.EMPTY_LIST
              : setupSweepingOutputPcf.getAppDetailsToBeDownsized()
                    .stream()
                    .map(PcfAppSetupTimeDetails::getApplicationName)
                    .collect(toList()));
    }

    return appNames;
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

  protected ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, ResponseData> response) {
    String activityId = response.keySet().iterator().next();
    PcfCommandExecutionResponse executionResponse = (PcfCommandExecutionResponse) response.values().iterator().next();
    ExecutionStatus executionStatus = executionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS
        ? ExecutionStatus.SUCCESS
        : ExecutionStatus.FAILED;
    activityService.updateStatus(activityId, context.getAppId(), executionStatus);

    PcfDeployCommandResponse pcfDeployCommandResponse =
        (PcfDeployCommandResponse) executionResponse.getPcfCommandResponse();

    if (pcfDeployCommandResponse.getInstanceDataUpdated() == null) {
      pcfDeployCommandResponse.setInstanceDataUpdated(new ArrayList<>());
    }

    // update PcfDeployStateExecutionData,
    PcfRouteUpdateStateExecutionData stateExecutionData =
        (PcfRouteUpdateStateExecutionData) context.getStateExecutionData();
    stateExecutionData.setStatus(executionStatus);
    stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());

    return ExecutionResponse.builder()
        .executionStatus(executionStatus)
        .errorMessage(executionResponse.getErrorMessage())
        .stateExecutionData(stateExecutionData)
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  protected Activity createActivity(ExecutionContext executionContext) {
    return pcfStateHelper.createActivity(
        executionContext, PCF_MAP_ROUTE_COMMAND, getStateType(), CommandUnitType.PCF_MAP_ROUTE, activityService);
  }

  public boolean checkIfMapRouteOperation() {
    return true;
  }
}

package software.wings.sm.states.pcf;

import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.api.PhaseElement;
import software.wings.api.pcf.PcfRouteUpdateStateExecutionData;
import software.wings.api.pcf.PcfSetupContextElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.PcfConfig;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.common.Constants;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest.PcfCommandType;
import software.wings.helpers.ext.pcf.request.PcfCommandRouteUpdateRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfDeployCommandResponse;
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
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MapRouteState extends State {
  @Inject private transient AppService appService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient DelegateService delegateService;
  @Inject private transient SecretManager secretManager;
  @Inject private transient SettingsService settingsService;
  @Inject private transient ActivityService activityService;
  @Inject @Transient protected transient LogService logService;

  public static final String PCF_MAP_ROUTE_COMMAND = "PCF Map Route";

  private static final Logger logger = LoggerFactory.getLogger(MapRouteState.class);

  @DefaultValue("${" + Constants.PCF_APP_NAME + "}") @Attributes(title = "PCF App Name") private String pcfAppName;

  @DefaultValue("${" + Constants.INFRA_ROUTE + "}") @Attributes(title = "Map Route") private String route;

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

    // route.equalsIgnoreCase("${" + Constants.INFRA_ROUTE + "}");

    Activity activity = createActivity(context);
    SettingAttribute settingAttribute = settingsService.get(pcfInfrastructureMapping.getComputeProviderSettingId());
    PcfConfig pcfConfig = (PcfConfig) settingAttribute.getValue();

    List<EncryptedDataDetail> encryptedDataDetails = secretManager.getEncryptionDetails(
        (Encryptable) pcfConfig, context.getAppId(), context.getWorkflowExecutionId());

    PcfCommandRequest pcfCommandRequest =
        PcfCommandRouteUpdateRequest.builder()
            .pcfCommandType(PcfCommandType.UPDATE_ROUTE)
            .commandName(PCF_MAP_ROUTE_COMMAND)
            .appId(app.getUuid())
            .accountId(app.getAccountId())
            .activityId(activity.getUuid())
            .pcfConfig(pcfConfig)
            .organization(pcfInfrastructureMapping.getOrganization())
            .space(pcfInfrastructureMapping.getSpace())
            .routeMaps(getRoutes(pcfInfrastructureMapping))
            .appsToBeUpdated(getApplicationNamesTobeUpdated(pcfSetupContextElement))
            .timeoutIntervalInMin(pcfSetupContextElement.getTimeoutIntervalInMinutes())
            .isMapRoutesOperation(checkIfMapRouteOperation())
            .build();

    PcfRouteUpdateStateExecutionData stateExecutionData =
        PcfRouteUpdateStateExecutionData.builder()
            .activityId(activity.getUuid())
            .accountId(app.getAccountId())
            .appId(app.getUuid())
            .pcfCommandRequest(pcfCommandRequest)
            .commandName(PCF_MAP_ROUTE_COMMAND)
            .routeMaps(pcfInfrastructureMapping.getRouteMaps())
            .appnames(((PcfCommandRouteUpdateRequest) pcfCommandRequest).getAppsToBeUpdated())
            .build();

    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(app.getAccountId())
                                    .withAppId(app.getUuid())
                                    .withTaskType(TaskType.PCF_COMMAND_TASK)
                                    .withWaitId(activity.getUuid())
                                    .withParameters(new Object[] {pcfCommandRequest, encryptedDataDetails})
                                    .withEnvId(env.getUuid())
                                    .withTimeout(TimeUnit.MINUTES.toMillis(10))
                                    .withInfrastructureMappingId(pcfInfrastructureMapping.getUuid())
                                    .build();

    String delegateTaskId = delegateService.queueTask(delegateTask);

    return ExecutionResponse.Builder.anExecutionResponse()
        .withCorrelationIds(Arrays.asList(activity.getUuid()))
        .withStateExecutionData(stateExecutionData)
        .withAsync(true)
        .build();
  }

  private List<String> getRoutes(PcfInfrastructureMapping pcfInfrastructureMapping) {
    // determine which routes to map
    String infraRouteConst = "${" + Constants.INFRA_ROUTE + "}";
    boolean isOriginalRoute = route == null || infraRouteConst.equalsIgnoreCase(route.trim());

    List<String> routes =
        isOriginalRoute ? pcfInfrastructureMapping.getRouteMaps() : pcfInfrastructureMapping.getTempRouteMap();
    if (routes == null) {
      routes = Collections.EMPTY_LIST;
    }

    return routes;
  }

  protected boolean checkIfMapRouteOperation() {
    return true;
  }

  private List<String> getApplicationNamesTobeUpdated(PcfSetupContextElement pcfSetupContextElement) {
    String appConst = "${" + Constants.PCF_APP_NAME + "}";
    boolean isNewApplication = pcfAppName == null || appConst.equalsIgnoreCase(pcfAppName.trim());

    List<String> appNames = new ArrayList<>();

    if (isNewApplication) {
      appNames.add(pcfSetupContextElement.getNewPcfApplicationName());
    } else {
      appNames.addAll(pcfSetupContextElement.getAppsToBeDownsized() == null
              ? Collections.EMPTY_LIST
              : pcfSetupContextElement.getAppsToBeDownsized());
    }

    return appNames;
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

  protected ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, NotifyResponseData> response) {
    String activityId = response.keySet().iterator().next();
    PcfCommandExecutionResponse executionResponse = (PcfCommandExecutionResponse) response.values().iterator().next();
    ExecutionStatus executionStatus =
        executionResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS) ? ExecutionStatus.SUCCESS
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

    return ExecutionResponse.Builder.anExecutionResponse()
        .withExecutionStatus(executionStatus)
        .withErrorMessage(executionResponse.getErrorMessage())
        .withStateExecutionData(stateExecutionData)
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  protected Activity createActivity(ExecutionContext executionContext) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();

    ActivityBuilder activityBuilder = Activity.builder()
                                          .applicationName(app.getName())
                                          .appId(app.getUuid())
                                          .commandName(PCF_MAP_ROUTE_COMMAND)
                                          .type(Type.Command)
                                          .workflowType(executionContext.getWorkflowType())
                                          .workflowExecutionName(executionContext.getWorkflowExecutionName())
                                          .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
                                          .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
                                          .commandType(getStateType())
                                          .workflowExecutionId(executionContext.getWorkflowExecutionId())
                                          .workflowId(executionContext.getWorkflowId())
                                          .commandUnits(Collections.emptyList())
                                          .serviceVariables(Maps.newHashMap())
                                          .status(ExecutionStatus.RUNNING)
                                          .commandUnitType(CommandUnitType.PCF_MAP_ROUTE)
                                          .environmentId(env.getUuid())
                                          .environmentName(env.getName())
                                          .environmentType(env.getEnvironmentType());
    return activityService.save(activityBuilder.build());
  }
}

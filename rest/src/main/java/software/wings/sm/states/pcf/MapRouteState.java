package software.wings.sm.states.pcf;

import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
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
import software.wings.helpers.ext.pcf.request.PcfRouteUpdateRequestConfigData;
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
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MapRouteState extends State {
  @Inject private transient AppService appService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient DelegateService delegateService;
  @Inject private transient SecretManager secretManager;
  @Inject private transient SettingsService settingsService;
  @Inject private transient ActivityService activityService;
  @Inject private transient PcfStateHelper pcfStateHelper;
  @Inject @Transient protected transient LogService logService;

  public static final String PCF_MAP_ROUTE_COMMAND = "PCF Map Route";

  private static final Logger logger = LoggerFactory.getLogger(MapRouteState.class);

  @DefaultValue("${" + Constants.PCF_APP_NAME + "}") @Attributes(title = "PCF App Name") private String pcfAppName;

  @DefaultValue("${" + Constants.INFRA_ROUTE_PCF + "}") @Attributes(title = "Map Route") private String route;

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
    Application application = appService.get(context.getAppId());
    Environment environment = workflowStandardParams.getEnv();

    PcfInfrastructureMapping infrastructureMapping = (PcfInfrastructureMapping) infrastructureMappingService.get(
        application.getUuid(), phaseElement.getInfraMappingId());

    PcfSetupContextElement pcfSetupContextElement =
        context.<PcfSetupContextElement>getContextElementList(ContextElementType.PCF_SERVICE_SETUP)
            .stream()
            .filter(cse -> phaseElement.getInfraMappingId().equals(cse.getInfraMappingId()))
            .findFirst()
            .orElse(PcfSetupContextElement.builder().build());

    Activity activity = createActivity(context);
    SettingAttribute settingAttribute = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    PcfConfig pcfConfig = (PcfConfig) settingAttribute.getValue();

    List<EncryptedDataDetail> encryptedDetails = secretManager.getEncryptionDetails(
        (Encryptable) pcfConfig, context.getAppId(), context.getWorkflowExecutionId());

    PcfRouteUpdateRequestConfigData requestConfigData = null;
    if (isRollback()) {
      PcfSwapRouteRollbackContextElement pcfSwapRouteRollbackContextElement =
          context.getContextElement(ContextElementType.PCF_ROUTE_SWAP_ROLLBACK);
      requestConfigData = pcfSwapRouteRollbackContextElement.getPcfRouteUpdateRequestConfigData();
      requestConfigData.setRollback(true);
      requestConfigData.setMapRoutesOperation(!requestConfigData.isMapRoutesOperation());
    } else {
      requestConfigData = getPcfRouteUpdateRequestConfigData(pcfSetupContextElement, infrastructureMapping);
    }

    return pcfStateHelper.queueDelegateTaskForRouteUpdate(application, pcfConfig, delegateService,
        infrastructureMapping, activity.getUuid(), environment.getUuid(),
        pcfSetupContextElement.getTimeoutIntervalInMinutes(), PCF_MAP_ROUTE_COMMAND, requestConfigData,
        encryptedDetails);
  }

  private PcfRouteUpdateRequestConfigData getPcfRouteUpdateRequestConfigData(
      PcfSetupContextElement pcfSetupContextElement, PcfInfrastructureMapping infrastructureMapping) {
    return PcfRouteUpdateRequestConfigData.builder()
        .existingApplicationNames(getApplicationNamesTobeUpdated(pcfSetupContextElement))
        .finalRoutes(getRoutes(pcfSetupContextElement))
        .isRollback(false)
        .isStandardBlueGreen(false)
        .isMapRoutesOperation(checkIfMapRouteOperation())
        .build();
  }

  private List<String> getRoutes(PcfSetupContextElement pcfSetupContextElement) {
    // determine which routes to map
    boolean isOriginalRoute = false;
    String infraRouteConst = "${" + Constants.INFRA_ROUTE + "}";
    String infraRouteConstLegacy = "${" + Constants.INFRA_ROUTE_PCF + "}";
    if (route == null || infraRouteConst.equalsIgnoreCase(route.trim())
        || infraRouteConstLegacy.equalsIgnoreCase(route.trim())) {
      isOriginalRoute = true;
    }

    List<String> routes =
        isOriginalRoute ? pcfSetupContextElement.getRouteMaps() : pcfSetupContextElement.getTempRouteMap();
    if (routes == null) {
      routes = Collections.EMPTY_LIST;
    }

    return routes;
  }

  private List<String> getApplicationNamesTobeUpdated(PcfSetupContextElement pcfSetupContextElement) {
    String appConst = "${" + Constants.PCF_APP_NAME + "}";
    boolean isNewApplication = pcfAppName == null || appConst.equalsIgnoreCase(pcfAppName.trim());

    List<String> appNames = new ArrayList<>();

    if (isNewApplication) {
      appNames.add(pcfSetupContextElement.getNewPcfApplicationDetails().getApplicationName());
    } else {
      appNames.addAll(pcfSetupContextElement.getAppsToBeDownsized() == null
              ? Collections.EMPTY_LIST
              : pcfSetupContextElement.getAppsToBeDownsized()
                    .stream()
                    .map(app -> app.getApplicationName())
                    .collect(toList()));
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
    return pcfStateHelper.createActivity(
        executionContext, PCF_MAP_ROUTE_COMMAND, getStateType(), CommandUnitType.PCF_MAP_ROUTE, activityService);
  }

  public boolean checkIfMapRouteOperation() {
    return true;
  }
}

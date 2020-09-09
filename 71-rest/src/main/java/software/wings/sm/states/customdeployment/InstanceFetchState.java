package software.wings.sm.states.customdeployment;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.logging.CommandExecutionStatus.RUNNING;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.LogColor.Red;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogHelper.doneColoring;
import static software.wings.beans.command.CommandUnitDetails.CommandUnitType.CUSTOM_DEPLOYMENT_FETCH_INSTANCES;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;
import static software.wings.sm.states.customdeployment.InstanceElementMapperUtils.getHostnameFieldName;

import com.google.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jayway.jsonpath.InvalidJsonException;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.SweepingOutputInstance.Scope;
import io.harness.data.algorithm.HashGenerator;
import io.harness.data.structure.EmptyPredicate;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.TaskData;
import io.harness.deployment.InstanceDetails;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.tasks.Cd1SetupFields;
import io.harness.tasks.ResponseData;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import software.wings.api.InfraMappingElement;
import software.wings.api.InstanceElement;
import software.wings.api.InstanceElementListParam;
import software.wings.api.customdeployment.InstanceFetchStateExecutionData;
import software.wings.api.instancedetails.InstanceInfoVariables;
import software.wings.api.shellscript.provision.ShellScriptProvisionExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Log.Builder;
import software.wings.beans.LogColor;
import software.wings.beans.TaskType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.FetchInstancesCommandUnit;
import software.wings.beans.shellscript.provisioner.ShellScriptProvisionParameters;
import software.wings.beans.template.deploymenttype.CustomDeploymentTypeTemplate;
import software.wings.expression.ManagerPreviewExpressionEvaluator;
import software.wings.service.impl.ActivityHelperService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.customdeployment.CustomDeploymentTypeService;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionResponse.ExecutionResponseBuilder;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.State;
import software.wings.sm.StateExecutionContext;
import software.wings.sm.StateType;
import software.wings.sm.states.ManagerExecutionLogCallback;
import software.wings.sm.states.utils.StateTimeoutUtils;
import software.wings.stencils.DefaultValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class InstanceFetchState extends State {
  public static final String OUTPUT_PATH_KEY = "INSTANCE_OUTPUT_PATH";
  public static final String FETCH_INSTANCE_COMMAND_UNIT = "Execute Instance Fetch Script";
  public static final String MAP_INSTANCES = "Map Instances";

  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private CustomDeploymentTypeService customDeploymentTypeService;
  @Inject private DelegateService delegateService;
  @Inject private ActivityHelperService activityHelperService;
  @Inject private SweepingOutputService sweepingOutputService;
  @Inject private LogService logService;

  @Getter @Setter @DefaultValue("10") String stateTimeoutInMinutes;
  @Getter @Setter private List<String> tags;

  static Function<String, InstanceElement> instanceElementMapper = hostName
      -> anInstanceElement()
             .uuid(UUIDGenerator.generateUuid())
             .hostName(hostName)
             .displayName(hostName)
             .newInstance(true)
             .build();

  static Function<String, InstanceDetails> instanceDetailsMapper = hostName
      -> InstanceDetails.builder()
             .newInstance(true)
             .hostName(hostName)
             .physicalHost(InstanceDetails.PHYSICAL_HOST.builder().instanceId(hostName).build())
             .build();

  public InstanceFetchState(String name) {
    super(name, StateType.CUSTOM_DEPLOYMENT_FETCH_INSTANCES.name());
  }

  @Override
  public Integer getTimeoutMillis(ExecutionContext context) {
    return StateTimeoutUtils.getTimeoutMillisFromMinutes(
        Integer.valueOf(context.renderExpression(stateTimeoutInMinutes)));
  }

  /**
   * Execute.
   *
   * @param context the context
   * @return the execution response
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    final String accountId = context.getAccountId();
    final String appId = context.getAppId();
    final String activityId = createAndSaveActivity(context);
    final String infraMappingId = context.fetchInfraMappingId();
    final InfraMappingElement infraMappingElement = context.fetchInfraMappingElement();
    final InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, infraMappingId);
    final CustomDeploymentTypeTemplate deploymentTypeTemplate = customDeploymentTypeService.fetchDeploymentTemplate(
        accountId, infrastructureMapping.getCustomDeploymentTemplateId(),
        infrastructureMapping.getDeploymentTypeTemplateVersion());

    String safeRenderedScriptString = getRenderedScriptExceptSecrets(
        deploymentTypeTemplate.getFetchInstanceScript(), infraMappingElement.getCustom().getVars());

    final ManagerExecutionLogCallback logCallback =
        buildLogcallBack(appId, activityId, CUSTOM_DEPLOYMENT_FETCH_INSTANCES.getName());

    logCallback.saveExecutionLog("Dispatching script to delegate for fetching instances", LogLevel.INFO, RUNNING);
    logCallback.saveExecutionLog(safeRenderedScriptString);

    ShellScriptProvisionParameters taskParameters = ShellScriptProvisionParameters.builder()
                                                        .accountId(accountId)
                                                        .appId(appId)
                                                        .activityId(activityId)
                                                        .scriptBody(safeRenderedScriptString)
                                                        .textVariables(infraMappingElement.getCustom().getVars())
                                                        .commandUnit(CUSTOM_DEPLOYMENT_FETCH_INSTANCES.getName())
                                                        .outputPathKey(OUTPUT_PATH_KEY)
                                                        .workflowExecutionId(context.getWorkflowExecutionId())
                                                        .build();

    final long timeout =
        ObjectUtils.defaultIfNull(Long.valueOf(getTimeoutMillis(context)), TaskData.DEFAULT_ASYNC_CALL_TIMEOUT);

    int expressionFunctorToken = HashGenerator.generateIntegerHash();
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(accountId)
                                    .description("Fetch Instances")
                                    .waitId(activityId)
                                    .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, appId)
                                    .tags(getRenderedTags(context))
                                    .data(TaskData.builder()
                                              .async(true)
                                              .parameters(new Object[] {taskParameters})
                                              .taskType(TaskType.SHELL_SCRIPT_PROVISION_TASK.name())
                                              .timeout(timeout)
                                              .expressionFunctorToken(expressionFunctorToken)
                                              .build())
                                    .build();

    renderDelegateTask(context, delegateTask,
        StateExecutionContext.builder()
            .stateExecutionData(context.getStateExecutionData())
            .adoptDelegateDecryption(true)
            .expressionFunctorToken(expressionFunctorToken)
            .build());

    delegateService.queueTask(delegateTask);

    appendDelegateTaskDetails(context, delegateTask);

    return ExecutionResponse.builder()
        .async(true)
        .correlationId(activityId)
        .stateExecutionData(InstanceFetchStateExecutionData.builder()
                                .activityId(activityId)
                                .hostObjectArrayPath(deploymentTypeTemplate.getHostObjectArrayPath())
                                .hostAttributes(deploymentTypeTemplate.getHostAttributes())
                                .instanceFetchScript(safeRenderedScriptString)
                                .build())
        .build();
  }

  private ManagerExecutionLogCallback buildLogcallBack(String appId, String activityId, String commandUnitName) {
    Builder logBuilder = aLog()
                             .appId(appId)
                             .activityId(activityId)
                             .logLevel(LogLevel.INFO)
                             .commandUnitName(commandUnitName)
                             .executionResult(RUNNING);

    return new ManagerExecutionLogCallback(logService, logBuilder, activityId);
  }

  /**
   * Handle abort event.
   *
   * @param context the context
   */
  @Override
  public void handleAbortEvent(ExecutionContext context) {
    // nothing to handle
  }

  /**
   * Callback for handing responses from states that this state was waiting on.
   *
   * @param context  Context of execution.
   * @param response map of responses this state was waiting on.
   * @return Response from handling this state.
   */
  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    final Entry<String, ResponseData> responseEntry = response.entrySet().iterator().next();
    ShellScriptProvisionExecutionData executionData = (ShellScriptProvisionExecutionData) responseEntry.getValue();

    final InstanceFetchStateExecutionData stateExecutionData = context.getStateExecutionData();
    final ManagerExecutionLogCallback logCallback =
        buildLogcallBack(context.getAppId(), stateExecutionData.getActivityId(), MAP_INSTANCES);
    stateExecutionData.setStatus(executionData.getExecutionStatus());

    List<InstanceElement> instanceElements = new ArrayList<>();
    List<InstanceDetails> instanceDetails = new ArrayList<>();
    if (FAILED == executionData.getExecutionStatus()) {
      activityHelperService.updateStatus(stateExecutionData.getActivityId(), context.getAppId(), FAILED);
      return ExecutionResponse.builder()
          .executionStatus(FAILED)
          .stateExecutionData(stateExecutionData)
          .errorMessage(executionData.getErrorMsg())
          .build();
    } else if (SUCCESS == executionData.getExecutionStatus()) {
      String output = executionData.getOutput();
      try {
        instanceElements = InstanceElementMapperUtils.mapJsonToInstanceElements(stateExecutionData.getHostAttributes(),
            stateExecutionData.getHostObjectArrayPath(), output, instanceElementMapper);
        instanceDetails = InstanceElementMapperUtils.mapJsonToInstanceElements(stateExecutionData.getHostAttributes(),
            stateExecutionData.getHostObjectArrayPath(), output, instanceDetailsMapper);
        validateInstanceElements(instanceElements, output, stateExecutionData, logCallback);
      } catch (Exception ex) {
        logCallback.saveExecutionLog(
            doneColoring(color(ex.getMessage(), Red)), LogLevel.ERROR, CommandExecutionStatus.FAILURE);
        return handleException(ex, "Error occurred while mapping script output Json to instances");
      }
      stateExecutionData.setScriptOutput(output);
    }

    stateExecutionData.setNewInstanceStatusSummaries(
        buildInstanceStatusSummaries(instanceElements, executionData.getExecutionStatus()));
    activityHelperService.updateStatus(
        stateExecutionData.getActivityId(), context.getAppId(), executionData.getExecutionStatus());

    String messageToLog = format("%n Found %d hosts {%s}", instanceDetails.size(),
        instanceDetails.stream().map(InstanceDetails::getHostName).collect(Collectors.toList()));
    logCallback.saveExecutionLog(
        doneColoring(color(messageToLog, LogColor.Green)), LogLevel.INFO, CommandExecutionStatus.SUCCESS);

    saveInstanceInfoToSweepingOutput(context, instanceElements, instanceDetails);

    InstanceElementListParam instanceElementListParam =
        InstanceElementListParam.builder().instanceElements(instanceElements).build();
    return ExecutionResponse.builder()
        .executionStatus(executionData.getExecutionStatus())
        .stateExecutionData(stateExecutionData)
        .contextElement(instanceElementListParam)
        .notifyElement(instanceElementListParam)
        .build();
  }

  private void validateInstanceElements(List<InstanceElement> instanceElements, String output,
      InstanceFetchStateExecutionData stateExecutionData, LogCallback logCallback) throws JsonProcessingException {
    final boolean elementWithoutHostnameExists =
        instanceElements.stream().map(InstanceElement::getHostName).anyMatch(StringUtils::isBlank);
    if (elementWithoutHostnameExists) {
      logCallback.saveExecutionLog(
          InstanceElementMapperUtils.prettyJson(output, stateExecutionData.getHostObjectArrayPath()), LogLevel.ERROR);
      throw new InvalidRequestException(format("Could not find \"%s\" field from Json Array",
                                            getHostnameFieldName(stateExecutionData.getHostAttributes())),
          WingsException.USER);
    }
  }

  private ExecutionResponse handleException(Throwable t, String defaultErrorMessage) {
    final ExecutionResponseBuilder responseBuilder = ExecutionResponse.builder().executionStatus(FAILED);
    StringBuilder errorMessage = new StringBuilder("Reason: ");
    if (t instanceof InvalidJsonException) {
      errorMessage.append(org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage(t));
    } else if (t instanceof WingsException) {
      errorMessage.append(ExceptionUtils.getMessage(t));
    } else {
      errorMessage.append(defaultErrorMessage).append("\n").append(t.getMessage());
    }
    logger.error(errorMessage.toString(), t);
    return responseBuilder.errorMessage(errorMessage.toString()).build();
  }

  private String createAndSaveActivity(ExecutionContext executionContext) {
    List<CommandUnit> commandUnits = asList(
        new FetchInstancesCommandUnit(FETCH_INSTANCE_COMMAND_UNIT), new FetchInstancesCommandUnit(MAP_INSTANCES));
    return activityHelperService
        .createAndSaveActivity(executionContext, Activity.Type.Command, getName(),
            CUSTOM_DEPLOYMENT_FETCH_INSTANCES.getName(), commandUnits)
        .getUuid();
  }

  private String getRenderedScriptExceptSecrets(String script, Map<String, String> contextMap) {
    ManagerPreviewExpressionEvaluator previewExpressionEvaluator =
        ManagerPreviewExpressionEvaluator.evaluatorWithSecretExpressionFormat();
    return previewExpressionEvaluator.substitute(script, Collections.<String, Object>unmodifiableMap(contextMap));
  }

  void saveInstanceInfoToSweepingOutput(
      ExecutionContext context, List<InstanceElement> instanceElements, List<InstanceDetails> instanceDetails) {
    sweepingOutputService.save(context.prepareSweepingOutputBuilder(Scope.WORKFLOW)
                                   .name(context.appendStateExecutionId(InstanceInfoVariables.SWEEPING_OUTPUT_NAME))
                                   .value(InstanceInfoVariables.builder()
                                              .instanceElements(instanceElements)
                                              .instanceDetails(instanceDetails)
                                              .skipVerification(isEmpty(instanceDetails))
                                              .build())
                                   .build());
  }

  private List<InstanceStatusSummary> buildInstanceStatusSummaries(
      List<InstanceElement> instanceElementList, ExecutionStatus executionStatus) {
    return instanceElementList.stream()
        .map(instanceElement
            -> anInstanceStatusSummary().withInstanceElement(instanceElement).withStatus(executionStatus).build())
        .collect(Collectors.toList());
  }

  private List<String> getRenderedTags(ExecutionContext context) {
    if (EmptyPredicate.isNotEmpty(tags)) {
      return tags.stream()
          .map(context::renderExpression)
          .filter(StringUtils::isNotBlank)
          .map(StringUtils::trim)
          .distinct()
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }
}

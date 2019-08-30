package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static software.wings.api.CommandStateExecutionData.Builder.aCommandStateExecutionData;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.TaskType.AWS_LAMBDA_TASK;
import static software.wings.service.intfc.ServiceTemplateService.EncryptedFieldComputeMode.OBTAIN_VALUE;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.TriggeredBy;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Getter;
import lombok.Setter;
import software.wings.api.AwsLambdaContextElement;
import software.wings.api.AwsLambdaContextElement.FunctionMeta;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsLambdaInfraStructureMapping;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.Environment;
import software.wings.beans.LambdaSpecification;
import software.wings.beans.LambdaSpecification.FunctionSpecification;
import software.wings.beans.Log.Builder;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Tag;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandUnit;
import software.wings.common.Constants;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.aws.model.AwsLambdaExecuteWfRequest;
import software.wings.service.impl.aws.model.AwsLambdaExecuteWfRequest.AwsLambdaExecuteWfRequestBuilder;
import software.wings.service.impl.aws.model.AwsLambdaExecuteWfResponse;
import software.wings.service.impl.aws.model.AwsLambdaFunctionParams;
import software.wings.service.impl.aws.model.AwsLambdaFunctionParams.AwsLambdaFunctionParamsBuilder;
import software.wings.service.impl.aws.model.AwsLambdaFunctionResult;
import software.wings.service.impl.aws.model.AwsLambdaVpcConfig;
import software.wings.service.impl.aws.model.AwsLambdaVpcConfig.AwsLambdaVpcConfigBuilder;
import software.wings.service.impl.servicetemplates.ServiceTemplateHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.utils.LambdaConvention;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AwsLambdaState extends State {
  @Inject protected transient SettingsService settingsService;
  @Inject protected transient ServiceResourceService serviceResourceService;
  @Inject protected transient ServiceTemplateService serviceTemplateService;
  @Inject protected transient ActivityService activityService;
  @Inject protected transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient DelegateService delegateService;
  @Inject private transient AwsHelperService awsHelperService;
  @Inject private transient SecretManager secretManager;
  @Inject private transient LogService logService;
  @Inject private transient ArtifactStreamService artifactStreamService;
  @Inject private transient EncryptionService encryptionService;
  @Inject private transient ServiceTemplateHelper serviceTemplateHelper;
  @Attributes(title = "Command")
  @DefaultValue(Constants.AWS_LAMBDA_COMMAND_NAME)
  private String commandName = Constants.AWS_LAMBDA_COMMAND_NAME;

  @Attributes(title = "Lambda Function Alias", required = true)
  @DefaultValue("${env.name}")
  @SchemaIgnore
  private List<String> aliases = new ArrayList<>();

  @Getter @Setter private List<Tag> tags;

  private static Pattern wildCharPattern = Pattern.compile("[_+*/\\\\ &$|\"']");

  /**
   * Instantiates a new Aws lambda state.
   *
   * @param name the name
   */
  public AwsLambdaState(String name) {
    super(name, StateType.AWS_LAMBDA_STATE.name());
  }

  /**
   * Instantiates a new Aws lambda state.
   *
   * @param name the name
   * @param type the type
   */
  protected AwsLambdaState(String name, String type) {
    super(name, type);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      return executeInternal(context);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      return handleAsyncResponseInternal(context, response);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  private ExecutionResponse handleAsyncResponseInternal(ExecutionContext context, Map<String, ResponseData> response) {
    String activityId = response.keySet().iterator().next();

    AwsLambdaExecuteWfResponse wfResponse = (AwsLambdaExecuteWfResponse) response.values().iterator().next();
    activityService.updateStatus(activityId, context.getAppId(), wfResponse.getExecutionStatus());

    CommandStateExecutionData stateExecutionData = (CommandStateExecutionData) context.getStateExecutionData();
    stateExecutionData.setDelegateMetaInfo(wfResponse.getDelegateMetaInfo());

    List<FunctionMeta> functionMetas =
        wfResponse.getFunctionResults().stream().map(AwsLambdaFunctionResult::getFunctionMeta).collect(toList());
    AwsConfig awsConfig = wfResponse.getAwsConfig();
    String region = wfResponse.getRegion();
    stateExecutionData.setAliases(aliases);
    stateExecutionData.setTags(tags);
    AwsLambdaContextElement awsLambdaContextElement = AwsLambdaContextElement.Builder.anAwsLambdaContextElement()
                                                          .withAwsConfig(awsConfig)
                                                          .withAliases(aliases)
                                                          .withTags(tags)
                                                          .withRegion(region)
                                                          .withFunctionArns(functionMetas)
                                                          .build();

    return ExecutionResponse.builder()
        .stateExecutionData(stateExecutionData)
        .executionStatus(wfResponse.getExecutionStatus())
        .contextElement(awsLambdaContextElement)
        .notifyElement(awsLambdaContextElement)
        .build();
  }

  private ExecutionResponse executeInternal(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    notNullCheck("workflowStandardParams", workflowStandardParams, USER);
    notNullCheck("currentUser", workflowStandardParams.getCurrentUser(), USER);

    Application app = workflowStandardParams.getApp();
    Environment env = workflowStandardParams.getEnv();

    String envId = env.getUuid();
    Service service = serviceResourceService.get(app.getUuid(), serviceId);
    Command command =
        serviceResourceService.getCommandByName(app.getUuid(), serviceId, envId, getCommandName()).getCommand();

    AwsLambdaInfraStructureMapping infrastructureMapping =
        (AwsLambdaInfraStructureMapping) infrastructureMappingService.get(
            app.getUuid(), phaseElement.getInfraMappingId());

    SettingAttribute cloudProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    String region = infrastructureMapping.getRegion();

    List<CommandUnit> commandUnitList =
        serviceResourceService.getFlattenCommandUnitList(app.getUuid(), serviceId, envId, command.getName());
    ActivityBuilder activityBuilder = Activity.builder()
                                          .applicationName(app.getName())
                                          .environmentId(envId)
                                          .environmentName(env.getName())
                                          .environmentType(env.getEnvironmentType())
                                          .serviceId(service.getUuid())
                                          .serviceName(service.getName())
                                          .commandName(command.getName())
                                          .type(Type.Command)
                                          .workflowExecutionId(context.getWorkflowExecutionId())
                                          .workflowId(context.getWorkflowId())
                                          .workflowType(context.getWorkflowType())
                                          .workflowExecutionName(context.getWorkflowExecutionName())
                                          .stateExecutionInstanceId(context.getStateExecutionInstanceId())
                                          .stateExecutionInstanceName(context.getStateExecutionInstanceName())
                                          .commandUnits(commandUnitList)
                                          .commandType(command.getCommandUnitType().name())
                                          .status(ExecutionStatus.RUNNING)
                                          .triggeredBy(TriggeredBy.builder()
                                                           .email(workflowStandardParams.getCurrentUser().getEmail())
                                                           .name(workflowStandardParams.getCurrentUser().getName())
                                                           .build());

    Artifact artifact =
        getArtifact(app.getUuid(), serviceId, context.getWorkflowExecutionId(), (DeploymentExecutionContext) context);
    if (artifact == null) {
      throw new WingsException(format("Unable to find artifact for service %s", service.getName()));
    }

    ArtifactStream artifactStream = artifactStreamService.get(artifact.getArtifactStreamId());

    activityBuilder.artifactStreamId(artifactStream.getUuid())
        .artifactStreamName(artifactStream.getSourceName())
        .artifactName(artifact.getDisplayName())
        .artifactId(artifact.getUuid());
    activityBuilder.artifactId(artifact.getUuid()).artifactName(artifact.getDisplayName());

    Activity build = activityBuilder.build();
    build.setAppId(app.getUuid());
    Activity activity = activityService.save(build);

    Builder logBuilder = aLog()
                             .withAppId(activity.getAppId())
                             .withActivityId(activity.getUuid())
                             .withLogLevel(LogLevel.INFO)
                             .withCommandUnitName(commandUnitList.get(0).getName())
                             .withExecutionResult(CommandExecutionStatus.RUNNING);

    logService.batchedSave(singletonList(logBuilder.but().withLogLine("Begin command execution.").build()));

    CommandStateExecutionData.Builder executionDataBuilder = aCommandStateExecutionData()
                                                                 .withServiceId(service.getUuid())
                                                                 .withServiceName(service.getName())
                                                                 .withAppId(app.getUuid())
                                                                 .withCommandName(getCommandName())
                                                                 .withActivityId(activity.getUuid());

    LambdaSpecification specification = serviceResourceService.getLambdaSpecification(app.getUuid(), service.getUuid());

    if (isEmpty(specification.getFunctions())) {
      logService.batchedSave(singletonList(logBuilder.but().withLogLine("No Lambda function to deploy.").build()));
      activityService.updateStatus(activity.getUuid(), activity.getAppId(), SUCCESS);
      List<FunctionMeta> functionArns = new ArrayList<>();
      AwsLambdaContextElement awsLambdaContextElement = AwsLambdaContextElement.Builder.anAwsLambdaContextElement()
                                                            .withAwsConfig((AwsConfig) cloudProviderSetting.getValue())
                                                            .withRegion(region)
                                                            .withFunctionArns(functionArns)
                                                            .build();
      return ExecutionResponse.builder()
          .stateExecutionData(executionDataBuilder.build())
          .contextElement(awsLambdaContextElement)
          .notifyElement(awsLambdaContextElement)
          .executionStatus(SUCCESS)
          .build();
    } else {
      AwsLambdaExecuteWfRequest wfRequest = constructLambdaWfRequestParams(specification, context, context.getAppId(),
          env.getUuid(), infrastructureMapping, (AwsConfig) cloudProviderSetting.getValue(), region, artifact,
          app.getAccountId(), activity.getUuid());
      DelegateTask delegateTask =
          DelegateTask.builder()
              .async(true)
              .accountId(app.getAccountId())
              .waitId(activity.getUuid())
              .appId(context.getAppId())
              .tags(isNotEmpty(wfRequest.getAwsConfig().getTag()) ? singletonList(wfRequest.getAwsConfig().getTag())
                                                                  : null)
              .data(TaskData.builder()
                        .taskType(AWS_LAMBDA_TASK.name())
                        .parameters(new Object[] {wfRequest})
                        .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                        .build())
              .build();
      String delegateTaskId = delegateService.queueTask(delegateTask);
      return ExecutionResponse.builder()
          .async(true)
          .correlationIds(singletonList(activity.getUuid()))
          .delegateTaskId(delegateTaskId)
          .stateExecutionData(executionDataBuilder.build())
          .build();
    }
  }

  protected List<String> getEvaluatedAliases(ExecutionContext context) {
    if (isNotEmpty(aliases)) {
      return aliases.stream().map(context::renderExpression).collect(toList());
    }
    return emptyList();
  }

  private AwsLambdaExecuteWfRequest constructLambdaWfRequestParams(LambdaSpecification specification,
      ExecutionContext context, String appId, String envId, AwsLambdaInfraStructureMapping infrastructureMapping,
      AwsConfig awsConfig, String region, Artifact artifact, String accountId, String activityId) {
    AwsLambdaExecuteWfRequestBuilder wfRequestBuilder = AwsLambdaExecuteWfRequest.builder();
    wfRequestBuilder.awsConfig(awsConfig);
    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails(awsConfig, appId, context.getWorkflowExecutionId());
    wfRequestBuilder.encryptionDetails(encryptionDetails);
    wfRequestBuilder.region(region);
    wfRequestBuilder.accountId(accountId);
    wfRequestBuilder.appId(appId);
    wfRequestBuilder.activityId(activityId);
    wfRequestBuilder.commandName(getCommandName());
    wfRequestBuilder.roleArn(infrastructureMapping.getRole());

    if (isNotEmpty(aliases)) {
      wfRequestBuilder.evaluatedAliases(getEvaluatedAliases(context));
    }
    String serviceTemplateId = serviceTemplateHelper.fetchServiceTemplateId(infrastructureMapping);
    Map<String, String> serviceVariables =
        serviceTemplateService
            .computeServiceVariables(appId, envId, serviceTemplateId, context.getWorkflowExecutionId(), OBTAIN_VALUE)
            .stream()
            .filter(serviceVariable -> !ServiceVariable.Type.ARTIFACT.equals(serviceVariable.getType()))
            .collect(
                Collectors.toMap(ServiceVariable::getName, sv -> context.renderExpression(new String(sv.getValue()))));
    wfRequestBuilder.serviceVariables(serviceVariables);
    wfRequestBuilder.lambdaVpcConfig(getLambdaVpcConfig(infrastructureMapping));

    List<AwsLambdaFunctionParams> functionParams = new ArrayList<>();
    List<FunctionSpecification> functions = specification.getFunctions();
    if (isNotEmpty(functions)) {
      functions.forEach(functionSpecification -> {
        AwsLambdaFunctionParamsBuilder functionParamsBuilder = AwsLambdaFunctionParams.builder();
        functionParamsBuilder.key(context.renderExpression(artifact.getMetadata().get("key")));
        functionParamsBuilder.bucket(context.renderExpression(artifact.getMetadata().get("bucketName")));
        String functionName = context.renderExpression(functionSpecification.getFunctionName());
        functionName = LambdaConvention.normalizeFunctionName(functionName);
        functionParamsBuilder.functionName(functionName);
        functionParamsBuilder.handler(context.renderExpression(functionSpecification.getHandler()));
        functionParamsBuilder.runtime(context.renderExpression(functionSpecification.getRuntime()));
        functionParamsBuilder.memory(functionSpecification.getMemorySize());
        functionParamsBuilder.timeout(functionSpecification.getTimeout());
        functionParamsBuilder.functionTags(getFunctionTags(context));
        functionParams.add(functionParamsBuilder.build());
      });
    }
    wfRequestBuilder.functionParams(functionParams);
    return wfRequestBuilder.build();
  }

  protected Map<String, String> getFunctionTags(ExecutionContext context) {
    Map<String, String> functionTags = Maps.newHashMap();
    if (isNotEmpty(tags)) {
      tags.forEach(tag -> { functionTags.put(tag.getKey(), context.renderExpression(tag.getValue())); });
    }
    return functionTags;
  }

  private AwsLambdaVpcConfig getLambdaVpcConfig(AwsLambdaInfraStructureMapping infrastructureMapping) {
    String vpcId = infrastructureMapping.getVpcId();
    AwsLambdaVpcConfigBuilder builder = AwsLambdaVpcConfig.builder();
    if (vpcId != null) {
      builder.vpcId(vpcId);
      List<String> subnetIds = infrastructureMapping.getSubnetIds();
      List<String> securityGroupIds = infrastructureMapping.getSecurityGroupIds();
      if (!securityGroupIds.isEmpty() && !subnetIds.isEmpty()) {
        builder.subnetIds(subnetIds);
        builder.securityGroupIds(securityGroupIds);
      } else {
        throw new InvalidRequestException("At least one security group and one subnet must be provided");
      }
    }
    return builder.build();
  }

  /**
   * Gets artifact.
   *
   * @param appId                  the app id
   * @param serviceId              the service id
   * @param workflowExecutionId    the workflow execution id
   * @param deploymentExecutionContext the deploymentExecutionContext
   * @return the artifact
   */
  protected Artifact getArtifact(String appId, String serviceId, String workflowExecutionId,
      DeploymentExecutionContext deploymentExecutionContext) {
    return deploymentExecutionContext.getDefaultArtifactForService(serviceId);
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  /**
   * Gets command name.
   *
   * @return the command name
   */
  public String getCommandName() {
    return commandName;
  }

  /**
   * Sets command name.
   *
   * @param commandName the command name
   */
  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  /**
   * Gets aliases.
   *
   * @return the aliases
   */
  @SchemaIgnore
  public List<String> getAliases() {
    return aliases;
  }

  /**
   * Sets aliases.
   *
   * @param aliases the aliases
   */
  public void setAliases(List<String> aliases) {
    this.aliases = aliases;
  }
}

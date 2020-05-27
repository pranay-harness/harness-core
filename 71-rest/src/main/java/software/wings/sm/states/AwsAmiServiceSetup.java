package software.wings.sm.states;

import static io.harness.beans.OrchestrationWorkflowType.BLUE_GREEN;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;
import static software.wings.service.impl.aws.model.AwsConstants.AMI_SETUP_COMMAND_NAME;
import static software.wings.service.impl.aws.model.AwsConstants.DEFAULT_AMI_ASG_DESIRED_INSTANCES;
import static software.wings.service.impl.aws.model.AwsConstants.DEFAULT_AMI_ASG_MAX_INSTANCES;
import static software.wings.service.impl.aws.model.AwsConstants.DEFAULT_AMI_ASG_MIN_INSTANCES;
import static software.wings.service.impl.aws.model.AwsConstants.DEFAULT_AMI_ASG_TIMEOUT_MIN;
import static software.wings.utils.Misc.normalizeExpression;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import com.google.inject.Inject;

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
import lombok.extern.slf4j.Slf4j;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.AmiServiceSetupElement;
import software.wings.api.AwsAmiSetupExecutionData;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.Environment;
import software.wings.beans.Log.Builder;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.container.UserDataSpecification;
import software.wings.service.impl.aws.model.AwsAmiServiceSetupRequest;
import software.wings.service.impl.aws.model.AwsAmiServiceSetupRequest.AwsAmiServiceSetupRequestBuilder;
import software.wings.service.impl.aws.model.AwsAmiServiceSetupResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.spotinst.SpotInstStateHelper;
import software.wings.utils.AsgConvention;
import software.wings.utils.Misc;

import java.util.List;
import java.util.Map;

@Slf4j
public class AwsAmiServiceSetup extends State {
  private String autoScalingGroupName;
  private int autoScalingSteadyStateTimeout;
  private boolean useCurrentRunningCount;
  private String maxInstances;
  private String minInstances;
  private String desiredInstances;
  private ResizeStrategy resizeStrategy;
  private boolean blueGreen;

  @Inject private SettingsService settingsService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private SecretManager secretManager;
  @Inject private ActivityService activityService;
  @Inject private LogService logService;
  @Inject private DelegateService delegateService;
  @Inject private SpotInstStateHelper spotinstStateHelper;

  private String commandName = AMI_SETUP_COMMAND_NAME;

  public AwsAmiServiceSetup(String name) {
    super(name, StateType.AWS_AMI_SERVICE_SETUP.name());
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      return handleAsyncInternal(context, response);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  private int getTimeOut() {
    if (autoScalingSteadyStateTimeout == 0) {
      return DEFAULT_AMI_ASG_TIMEOUT_MIN;
    } else {
      return autoScalingSteadyStateTimeout;
    }
  }

  private ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, ResponseData> response) {
    String activityId = response.keySet().iterator().next();
    AwsAmiServiceSetupResponse amiServiceSetupResponse =
        (AwsAmiServiceSetupResponse) response.values().iterator().next();
    activityService.updateStatus(activityId, context.getAppId(), amiServiceSetupResponse.getExecutionStatus());

    AwsAmiSetupExecutionData awsAmiExecutionData = (AwsAmiSetupExecutionData) context.getStateExecutionData();
    awsAmiExecutionData.setNewAutoScalingGroupName(amiServiceSetupResponse.getNewAsgName());
    awsAmiExecutionData.setOldAutoScalingGroupName(amiServiceSetupResponse.getLastDeployedAsgName());
    awsAmiExecutionData.setNewVersion(amiServiceSetupResponse.getHarnessRevision());
    awsAmiExecutionData.setDelegateMetaInfo(amiServiceSetupResponse.getDelegateMetaInfo());

    AmiServiceSetupElement amiServiceElement =
        AmiServiceSetupElement.builder()
            .newAutoScalingGroupName(amiServiceSetupResponse.getNewAsgName())
            .oldAutoScalingGroupName(amiServiceSetupResponse.getLastDeployedAsgName())
            .baseScalingPolicyJSONs(amiServiceSetupResponse.getBaseAsgScalingPolicyJSONs())
            .minInstances(amiServiceSetupResponse.getMinInstances())
            .maxInstances(amiServiceSetupResponse.getMaxInstances())
            .desiredInstances(amiServiceSetupResponse.getDesiredInstances())
            .blueGreen(isBlueGreenWorkflow(context))
            .resizeStrategy(getResizeStrategy() == null ? RESIZE_NEW_FIRST : getResizeStrategy())
            .autoScalingSteadyStateTimeout(getTimeOut())
            .commandName(commandName)
            .oldAsgNames(amiServiceSetupResponse.getOldAsgNames())
            .preDeploymentData(amiServiceSetupResponse.getPreDeploymentData())
            .build();

    return ExecutionResponse.builder()
        .executionStatus(amiServiceSetupResponse.getExecutionStatus())
        .errorMessage(amiServiceSetupResponse.getErrorMessage())
        .stateExecutionData(awsAmiExecutionData)
        .contextElement(amiServiceElement)
        .notifyElement(amiServiceElement)
        .build();
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    notNullCheck("workflowStandardParams", workflowStandardParams, USER);
    notNullCheck("currentUser", workflowStandardParams.getCurrentUser(), USER);

    Artifact artifact = ((DeploymentExecutionContext) context).getDefaultArtifactForService(serviceId);
    if (artifact == null) {
      throw new InvalidRequestException(format("Unable to find artifact for service id: %s", serviceId));
    }

    Application app = workflowStandardParams.getApp();
    notNullCheck("Application cannot be null", app);
    Environment env = workflowStandardParams.getEnv();
    Service service = serviceResourceService.getWithDetails(app.getUuid(), serviceId);

    AwsAmiInfrastructureMapping infrastructureMapping =
        (AwsAmiInfrastructureMapping) infrastructureMappingService.get(app.getUuid(), context.fetchInfraMappingId());

    SettingAttribute cloudProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(
        (EncryptableSetting) cloudProviderSetting.getValue(), context.getAppId(), context.getWorkflowExecutionId());
    String region = infrastructureMapping.getRegion();
    AwsConfig awsConfig = (AwsConfig) cloudProviderSetting.getValue();

    AwsAmiSetupExecutionData awsAmiExecutionData = AwsAmiSetupExecutionData.builder().build();
    ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
    String errorMessage = null;

    String envId = env.getUuid();
    Command command =
        serviceResourceService.getCommandByName(app.getUuid(), serviceId, envId, getCommandName()).getCommand();

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

    Activity build = activityBuilder.build();
    build.setAppId(app.getUuid());
    Activity activity = activityService.save(build);

    String commandUnitName = commandUnitList.get(0).getName();

    Builder logBuilder =
        aLog().withAppId(activity.getAppId()).withActivityId(activity.getUuid()).withCommandUnitName(commandUnitName);

    ManagerExecutionLogCallback executionLogCallback =
        new ManagerExecutionLogCallback(logService, logBuilder, activity.getUuid());

    try {
      blueGreen = isBlueGreenWorkflow(context);
      executionLogCallback.saveExecutionLog("Starting AWS AMI Setup");

      List<String> classicLbs;
      List<String> targetGroupARNs;
      if (blueGreen) {
        // The new ASG should point to stage LBs and Target Groups
        classicLbs = infrastructureMapping.getStageClassicLoadBalancers();
        targetGroupARNs = infrastructureMapping.getStageTargetGroupArns();
      } else {
        classicLbs = infrastructureMapping.getClassicLoadBalancers();
        targetGroupARNs = infrastructureMapping.getTargetGroupArns();
      }

      AwsAmiServiceSetupRequestBuilder requestBuilder =
          AwsAmiServiceSetupRequest.builder()
              .accountId(awsConfig.getAccountId())
              .appId(activity.getAppId())
              .activityId(activity.getUuid())
              .commandName(getCommandName())
              .awsConfig(awsConfig)
              .encryptionDetails(encryptionDetails)
              .region(region)
              .infraMappingAsgName(infrastructureMapping.getAutoScalingGroupName())
              .infraMappingId(infrastructureMapping.getUuid())
              .infraMappingClassisLbs(classicLbs)
              .infraMappingTargetGroupArns(targetGroupARNs)
              .artifactRevision(artifact.getRevision())
              .blueGreen(blueGreen);

      UserDataSpecification userDataSpecification =
          serviceResourceService.getUserDataSpecification(app.getUuid(), serviceId);
      if (userDataSpecification != null && userDataSpecification.getData() != null) {
        String userData = userDataSpecification.getData();
        String userDataAfterEvaluation = context.renderExpression(userData);
        requestBuilder.userData(BaseEncoding.base64().encode(userDataAfterEvaluation.getBytes(Charsets.UTF_8)));
      }

      String asgNamePrefix = isNotEmpty(autoScalingGroupName)
          ? normalizeExpression(context.renderExpression(autoScalingGroupName))
          : AsgConvention.getAsgNamePrefix(app.getName(), service.getName(), env.getName());

      requestBuilder.newAsgNamePrefix(asgNamePrefix);

      // Evaluate expressions for min, max and desired
      requestBuilder.minInstances(
          spotinstStateHelper.renderCount(minInstances, context, DEFAULT_AMI_ASG_MIN_INSTANCES));
      requestBuilder.maxInstances(
          spotinstStateHelper.renderCount(maxInstances, context, DEFAULT_AMI_ASG_MAX_INSTANCES));
      requestBuilder.desiredInstances(
          spotinstStateHelper.renderCount(desiredInstances, context, DEFAULT_AMI_ASG_DESIRED_INSTANCES));
      requestBuilder.autoScalingSteadyStateTimeout(getTimeOut());
      requestBuilder.useCurrentRunningCount(useCurrentRunningCount);
      awsAmiExecutionData =
          AwsAmiSetupExecutionData.builder()
              .activityId(activity.getUuid())
              .maxInstances(spotinstStateHelper.renderCount(maxInstances, context, DEFAULT_AMI_ASG_MAX_INSTANCES))
              .desiredInstances(
                  spotinstStateHelper.renderCount(desiredInstances, context, DEFAULT_AMI_ASG_DESIRED_INSTANCES))
              .resizeStrategy(resizeStrategy)
              .build();

      AwsAmiServiceSetupRequest request = requestBuilder.build();
      DelegateTask delegateTask =
          DelegateTask.builder()
              .accountId(app.getAccountId())
              .appId(app.getUuid())
              .waitId(activity.getUuid())
              .tags(isNotEmpty(request.getAwsConfig().getTag()) ? singletonList(request.getAwsConfig().getTag()) : null)
              .data(TaskData.builder()
                        .async(true)
                        .taskType(TaskType.AWS_AMI_ASYNC_TASK.name())
                        .parameters(new Object[] {request})
                        .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                        .build())
              .envId(env.getUuid())
              .build();
      delegateService.queueTask(delegateTask);
    } catch (Exception exception) {
      logger.error("Ami setup step failed with error ", exception);
      executionStatus = ExecutionStatus.FAILED;
      errorMessage = ExceptionUtils.getMessage(exception);
      awsAmiExecutionData.setStatus(executionStatus);
      awsAmiExecutionData.setErrorMsg(errorMessage);
      Misc.logAllMessages(exception, executionLogCallback, CommandExecutionStatus.FAILURE);
    }

    return ExecutionResponse.builder()
        .correlationIds(singletonList(activity.getUuid()))
        .stateExecutionData(awsAmiExecutionData)
        .async(true)
        .executionStatus(executionStatus)
        .errorMessage(errorMessage)
        .build();
  }

  boolean isBlueGreenWorkflow(ExecutionContext context) {
    return BLUE_GREEN == context.getOrchestrationWorkflowType();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  public ResizeStrategy getResizeStrategy() {
    return resizeStrategy;
  }

  public void setResizeStrategy(ResizeStrategy resizeStrategy) {
    this.resizeStrategy = resizeStrategy;
  }

  public String getAutoScalingGroupName() {
    return autoScalingGroupName;
  }

  public void setAutoScalingGroupName(String autoScalingGroupName) {
    this.autoScalingGroupName = autoScalingGroupName;
  }

  public int getAutoScalingSteadyStateTimeout() {
    return autoScalingSteadyStateTimeout;
  }

  public void setAutoScalingSteadyStateTimeout(int autoScalingSteadyStateTimeout) {
    this.autoScalingSteadyStateTimeout = autoScalingSteadyStateTimeout;
  }

  public String getMaxInstances() {
    return maxInstances;
  }

  public String getMinInstances() {
    return minInstances;
  }

  public void setMinInstances(String minInstances) {
    this.minInstances = minInstances;
  }

  public void setMaxInstances(String maxInstances) {
    this.maxInstances = maxInstances;
  }

  public String getCommandName() {
    return commandName;
  }

  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  public boolean isBlueGreen() {
    return blueGreen;
  }

  public void setBlueGreen(boolean blueGreen) {
    this.blueGreen = blueGreen;
  }

  public boolean isUseCurrentRunningCount() {
    return useCurrentRunningCount;
  }

  public void setUseCurrentRunningCount(boolean useCurrentRunningCount) {
    this.useCurrentRunningCount = useCurrentRunningCount;
  }

  public String getDesiredInstances() {
    return desiredInstances;
  }

  public void setDesiredInstances(String desiredInstances) {
    this.desiredInstances = desiredInstances;
  }
}

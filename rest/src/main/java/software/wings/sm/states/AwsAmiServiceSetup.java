package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.singletonList;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.utils.Misc.normalizeExpression;

import com.google.common.io.BaseEncoding;
import com.google.inject.Inject;

import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.api.AmiServiceSetupElement;
import software.wings.api.AwsAmiSetupExecutionData;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.Environment;
import software.wings.beans.Log.Builder;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.container.UserDataSpecification;
import software.wings.common.Constants;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
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
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.AsgConvention;
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyResponseData;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
/**
 * Created by anubhaw on 12/19/17.
 */
public class AwsAmiServiceSetup extends State {
  private String autoScalingGroupName;
  private int autoScalingSteadyStateTimeout;
  private int maxInstances;
  private ResizeStrategy resizeStrategy;

  // @Inject @Transient private transient AwsHelperService awsHelperService;
  @Inject @Transient protected transient SettingsService settingsService;
  @Inject @Transient protected transient ServiceResourceService serviceResourceService;
  @Inject @Transient protected transient InfrastructureMappingService infrastructureMappingService;
  @Inject @Transient protected transient SecretManager secretManager;
  @Inject @Transient protected transient ActivityService activityService;
  @Inject @Transient protected transient ExecutorService executorService;
  @Inject @Transient protected transient LogService logService;
  @Inject @Transient private transient DelegateService delegateService;

  @Transient private static final Logger logger = LoggerFactory.getLogger(AwsAmiServiceSetup.class);

  private String commandName = Constants.AMI_SETUP_COMMAND_NAME;
  /**
   * Instantiates a new state.
   *
   * @param name the name
   */
  public AwsAmiServiceSetup(String name) {
    super(name, StateType.AWS_AMI_SERVICE_SETUP.name());
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    try {
      return handleAsyncInternal(context, response);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(Misc.getMessage(e), e);
    }
  }

  private ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, NotifyResponseData> response) {
    String activityId = response.keySet().iterator().next();
    AwsAmiServiceSetupResponse amiServiceSetupResponse =
        (AwsAmiServiceSetupResponse) response.values().iterator().next();
    activityService.updateStatus(activityId, context.getAppId(), amiServiceSetupResponse.getExecutionStatus());

    AwsAmiSetupExecutionData awsAmiExecutionData = (AwsAmiSetupExecutionData) context.getStateExecutionData();
    awsAmiExecutionData.setNewAutoScalingGroupName(amiServiceSetupResponse.getNewAsgName());
    awsAmiExecutionData.setOldAutoScalingGroupName(amiServiceSetupResponse.getLastDeployedAsgName());
    awsAmiExecutionData.setNewVersion(amiServiceSetupResponse.getHarnessRevision());

    maxInstances = getMaxInstances() == 0 ? 10 : getMaxInstances();
    autoScalingSteadyStateTimeout =
        getAutoScalingSteadyStateTimeout() == 0 ? (int) TimeUnit.MINUTES.toMinutes(10) : autoScalingSteadyStateTimeout;

    AmiServiceSetupElement amiServiceElement =
        AmiServiceSetupElement.builder()
            .newAutoScalingGroupName(amiServiceSetupResponse.getNewAsgName())
            .oldAutoScalingGroupName(amiServiceSetupResponse.getLastDeployedAsgName())
            .maxInstances(maxInstances)
            .resizeStrategy(getResizeStrategy() == null ? RESIZE_NEW_FIRST : getResizeStrategy())
            .autoScalingSteadyStateTimeout(autoScalingSteadyStateTimeout)
            .commandName(commandName)
            .build();

    return ExecutionResponse.Builder.anExecutionResponse()
        .withExecutionStatus(amiServiceSetupResponse.getExecutionStatus())
        .withErrorMessage(amiServiceSetupResponse.getErrorMessage())
        .withStateExecutionData(awsAmiExecutionData)
        .addContextElement(amiServiceElement)
        .addNotifyElement(amiServiceElement)
        .build();
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Artifact artifact = ((DeploymentExecutionContext) context).getArtifactForService(serviceId);

    Application app = workflowStandardParams.getApp();
    Environment env = workflowStandardParams.getEnv();
    Service service = serviceResourceService.get(app.getUuid(), serviceId);

    AwsAmiInfrastructureMapping infrastructureMapping =
        (AwsAmiInfrastructureMapping) infrastructureMappingService.get(app.getUuid(), phaseElement.getInfraMappingId());

    SettingAttribute cloudProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(
        (Encryptable) cloudProviderSetting.getValue(), context.getAppId(), context.getWorkflowExecutionId());
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
                                          .serviceVariables(context.getServiceVariables())
                                          .status(ExecutionStatus.RUNNING);

    Activity build = activityBuilder.build();
    build.setAppId(app.getUuid());
    Activity activity = activityService.save(build);

    String commandUnitName = commandUnitList.get(0).getName();

    Builder logBuilder =
        aLog().withAppId(activity.getAppId()).withActivityId(activity.getUuid()).withCommandUnitName(commandUnitName);

    ManagerExecutionLogCallback executionLogCallback =
        new ManagerExecutionLogCallback(logService, logBuilder, activity.getUuid());

    try {
      executionLogCallback.saveExecutionLog("Starting AWS AMI Setup");

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
              .infraMappingClassisLbs(infrastructureMapping.getClassicLoadBalancers())
              .infraMappingTargetGroupArns(infrastructureMapping.getTargetGroupArns())
              .artifactRevision(artifact.getRevision());

      UserDataSpecification userDataSpecification =
          serviceResourceService.getUserDataSpecification(app.getUuid(), serviceId);
      if (userDataSpecification != null && userDataSpecification.getData() != null) {
        try {
          String userData = userDataSpecification.getData();
          String userDataAfterEvaluation = context.renderExpression(userData);
          requestBuilder.userData(BaseEncoding.base64().encode(userDataAfterEvaluation.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
          logger.error("Error in setting user data ", e);
        }
      }

      String asgNamePrefix = isNotEmpty(autoScalingGroupName)
          ? normalizeExpression(context.renderExpression(autoScalingGroupName))
          : AsgConvention.getAsgNamePrefix(app.getName(), service.getName(), env.getName());
      requestBuilder.newAsgNamePrefix(asgNamePrefix);

      /**
       * Defaulting to 10 max instances in case we get maxInstances as 0
       */
      maxInstances = getMaxInstances() == 0 ? 10 : getMaxInstances();
      /**
       * Defaulting to 10 minutes timeout in case we get autoScalingSteadyStateTimeout as 0
       */
      autoScalingSteadyStateTimeout = getAutoScalingSteadyStateTimeout() == 0 ? (int) TimeUnit.MINUTES.toMinutes(10)
                                                                              : autoScalingSteadyStateTimeout;

      requestBuilder.maxInstances(maxInstances);
      requestBuilder.autoScalingSteadyStateTimeout(autoScalingSteadyStateTimeout);

      awsAmiExecutionData = AwsAmiSetupExecutionData.builder()
                                .activityId(activity.getUuid())
                                .maxInstances(maxInstances)
                                .resizeStrategy(resizeStrategy)
                                .build();

      DelegateTask delegateTask = aDelegateTask()
                                      .withAccountId(app.getAccountId())
                                      .withAppId(app.getUuid())
                                      .withTaskType(TaskType.AWS_AMI_ASYNC_TASK)
                                      .withWaitId(activity.getUuid())
                                      .withParameters(new Object[] {requestBuilder.build()})
                                      .withEnvId(env.getUuid())
                                      .withAsync(true)
                                      .build();
      delegateService.queueTask(delegateTask);
    } catch (Exception exception) {
      logger.error("Ami setup step failed with error ", exception);
      executionStatus = ExecutionStatus.FAILED;
      errorMessage = Misc.getMessage(exception);
      awsAmiExecutionData.setStatus(executionStatus);
      awsAmiExecutionData.setErrorMsg(errorMessage);
      Misc.logAllMessages(exception, executionLogCallback, CommandExecutionStatus.FAILURE);
    }

    return anExecutionResponse()
        .withCorrelationIds(singletonList(activity.getUuid()))
        .withStateExecutionData(awsAmiExecutionData)
        .withAsync(true)
        .withExecutionStatus(executionStatus)
        .withErrorMessage(errorMessage)
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  /**
   * Gets resize strategy.
   *
   * @return the resize strategy
   */
  public ResizeStrategy getResizeStrategy() {
    return resizeStrategy;
  }

  /**
   * Sets resize strategy.
   *
   * @param resizeStrategy the resize strategy
   */
  public void setResizeStrategy(ResizeStrategy resizeStrategy) {
    this.resizeStrategy = resizeStrategy;
  }

  /**
   * Gets ecs service name.
   *
   * @return the ecs service name
   */
  public String getAutoScalingGroupName() {
    return autoScalingGroupName;
  }

  /**
   * Sets ecs service name.
   *
   * @param autoScalingGroupName the ecs service name
   */
  public void setAutoScalingGroupName(String autoScalingGroupName) {
    this.autoScalingGroupName = autoScalingGroupName;
  }

  /**
   * Gets auto scaling steady state timeout.
   *
   * @return the auto scaling steady state timeout
   */
  public int getAutoScalingSteadyStateTimeout() {
    return autoScalingSteadyStateTimeout;
  }

  /**
   * Sets auto scaling steady state timeout.
   *
   * @param autoScalingSteadyStateTimeout the auto scaling steady state timeout
   */
  public void setAutoScalingSteadyStateTimeout(int autoScalingSteadyStateTimeout) {
    this.autoScalingSteadyStateTimeout = autoScalingSteadyStateTimeout;
  }

  public int getMaxInstances() {
    return maxInstances;
  }

  public void setMaxInstances(int maxInstances) {
    this.maxInstances = maxInstances;
  }

  public String getCommandName() {
    return commandName;
  }

  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }
}
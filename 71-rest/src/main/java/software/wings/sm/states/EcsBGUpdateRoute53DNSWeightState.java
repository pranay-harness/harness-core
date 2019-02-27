package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.RUNNING;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static software.wings.beans.Activity.Type.Command;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.TaskType.ECS_COMMAND_TASK;
import static software.wings.beans.command.CommandUnitDetails.CommandUnitType.AWS_ECS_UPDATE_ROUTE_53_DNS_WEIGHT;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.ECS_ROUTE53_DNS_WEIGHT_UPDATE;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.Getter;
import lombok.Setter;
import software.wings.api.ContainerServiceElement;
import software.wings.api.PhaseElement;
import software.wings.api.ecs.EcsRoute53WeightUpdateStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.SettingAttribute;
import software.wings.common.Constants;
import software.wings.helpers.ext.ecs.request.EcsBGRoute53DNSWeightUpdateRequest;
import software.wings.helpers.ext.ecs.response.EcsCommandExecutionResponse;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EcsBGUpdateRoute53DNSWeightState extends State {
  public static final String UPDATE_ROUTE_53_DNS_WEIGHTS = "Update Route 53 DNS Weights";
  private static final int MIN_WEIGHT = 0;
  private static final int MAX_WEIGHT = 100;
  @Getter @Setter @Attributes(title = "Record TTL") private int recordTTL;
  @Getter @Setter @Attributes(title = "Old Service DNS Weight [0, 100]") private int oldServiceDNSWeight;
  @Getter @Setter @Attributes(title = "New Service DNS Weight [0, 100]") private int newServiceDNSWeight;
  @Getter @Setter @Attributes(title = "Downsize old service") private boolean downsizeOldService;

  @Inject private transient AppService appService;
  @Inject private transient SecretManager secretManager;
  @Inject private transient SettingsService settingsService;
  @Inject private transient ActivityService activityService;
  @Inject private transient DelegateService delegateService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;

  public EcsBGUpdateRoute53DNSWeightState(String name) {
    super(name, ECS_ROUTE53_DNS_WEIGHT_UPDATE.name());
  }

  public EcsBGUpdateRoute53DNSWeightState(String name, String stateType) {
    super(name, stateType);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      return executeInternal(context, false);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  protected ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, ResponseData> response) {
    String activityId = response.keySet().iterator().next();
    EcsCommandExecutionResponse executionResponse = (EcsCommandExecutionResponse) response.values().iterator().next();
    ExecutionStatus executionStatus =
        CommandExecutionStatus.SUCCESS.equals(executionResponse.getCommandExecutionStatus()) ? SUCCESS : FAILED;
    activityService.updateStatus(activityId, context.getAppId(), executionStatus);
    return anExecutionResponse()
        .withErrorMessage(executionResponse.getErrorMessage())
        .withExecutionStatus(executionStatus)
        .build();
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

  protected ExecutionResponse executeInternal(ExecutionContext context, boolean rollback) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    Optional<ContainerServiceElement> containerServiceElementOptional =
        context.<ContainerServiceElement>getContextElementList(ContextElementType.CONTAINER_SERVICE)
            .stream()
            .filter(cse -> phaseElement.getDeploymentType().equals(cse.getDeploymentType().name()))
            .filter(cse -> phaseElement.getInfraMappingId().equals(cse.getInfraMappingId()))
            .findFirst();
    if (!containerServiceElementOptional.isPresent()) {
      throw new InvalidRequestException("No container service element found while swap route 53 END weights state");
    }

    ContainerServiceElement containerServiceElement = containerServiceElementOptional.get();
    Activity activity = createActivity(context);
    Application application = appService.get(context.getAppId());
    EcsInfrastructureMapping infrastructureMapping = (EcsInfrastructureMapping) infrastructureMappingService.get(
        application.getUuid(), phaseElement.getInfraMappingId());
    SettingAttribute settingAttribute = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> encryptedDetails =
        secretManager.getEncryptionDetails(awsConfig, context.getAppId(), context.getWorkflowExecutionId());

    EcsBGRoute53DNSWeightUpdateRequest request =
        EcsBGRoute53DNSWeightUpdateRequest.builder()
            .accountId(application.getAccountId())
            .appId(application.getUuid())
            .commandName(UPDATE_ROUTE_53_DNS_WEIGHTS)
            .activityId(activity.getUuid())
            .region(infrastructureMapping.getRegion())
            .cluster(infrastructureMapping.getClusterName())
            .awsConfig(awsConfig)
            .rollback(rollback)
            .serviceName(containerServiceElement.getNewEcsServiceName())
            .serviceNameDownsized(containerServiceElement.getEcsBGSetupData().getDownsizedServiceName())
            .serviceCountDownsized(containerServiceElement.getEcsBGSetupData().getDownsizedServiceCount())
            .downsizeOldService(downsizeOldService)
            .ttl(recordTTL)
            .oldServiceWeight(getDNSWeightWithinLimits(oldServiceDNSWeight))
            .newServiceWeight(getDNSWeightWithinLimits(newServiceDNSWeight))
            .parentRecordName(containerServiceElement.getEcsBGSetupData().getParentRecordName())
            .parentRecordHostedZoneId(containerServiceElement.getEcsBGSetupData().getParentRecordHostedZoneId())
            .oldServiceDiscoveryArn(containerServiceElement.getEcsBGSetupData().getOldServiceDiscoveryArn())
            .newServiceDiscoveryArn(containerServiceElement.getEcsBGSetupData().getNewServiceDiscoveryArn())
            .timeout(containerServiceElement.getServiceSteadyStateTimeout())
            .build();

    EcsRoute53WeightUpdateStateExecutionData executionData =
        EcsRoute53WeightUpdateStateExecutionData.builder()
            .activityId(activity.getUuid())
            .newServiceName(containerServiceElement.getNewEcsServiceName())
            .newServiceDiscoveryServiceArn(containerServiceElement.getEcsBGSetupData().getNewServiceDiscoveryArn())
            .newServiceWeight(rollback ? 0 : getDNSWeightWithinLimits(newServiceDNSWeight))
            .oldServiceName(containerServiceElement.getEcsBGSetupData().getDownsizedServiceName())
            .oldServiceDiscoveryServiceArn(containerServiceElement.getEcsBGSetupData().getOldServiceDiscoveryArn())
            .oldServiceWeight(rollback ? 100 : getDNSWeightWithinLimits(oldServiceDNSWeight))
            .build();

    DelegateTask delegateTask =
        aDelegateTask()
            .withAccountId(infrastructureMapping.getAccountId())
            .withAppId(infrastructureMapping.getAppId())
            .withWaitId(activity.getUuid())
            .withTimeout(MINUTES.toMillis(containerServiceElement.getServiceSteadyStateTimeout()))
            .withParameters(new Object[] {request, encryptedDetails})
            .withTags(
                isNotEmpty(request.getAwsConfig().getTag()) ? singletonList(request.getAwsConfig().getTag()) : null)
            .withTaskType(ECS_COMMAND_TASK.name())
            .withAsync(true)
            .withEnvId(infrastructureMapping.getEnvId())
            .build();

    delegateService.queueTask(delegateTask);

    return anExecutionResponse()
        .withAsync(true)
        .withStateExecutionData(executionData)
        .withExecutionStatus(SUCCESS)
        .addCorrelationIds(activity.getUuid())
        .build();
  }

  private int getDNSWeightWithinLimits(int weight) {
    if (weight < MIN_WEIGHT) {
      return MIN_WEIGHT;
    } else if (weight > MAX_WEIGHT) {
      return MAX_WEIGHT;
    } else {
      return weight;
    }
  }

  private Activity createActivity(ExecutionContext executionContext) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();
    ActivityBuilder activityBuilder = Activity.builder()
                                          .appId(app.getUuid())
                                          .applicationName(app.getName())
                                          .commandName(getName())
                                          .type(Command)
                                          .commandUnitType(AWS_ECS_UPDATE_ROUTE_53_DNS_WEIGHT)
                                          .workflowType(executionContext.getWorkflowType())
                                          .workflowExecutionName(executionContext.getWorkflowExecutionName())
                                          .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
                                          .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
                                          .commandType(getStateType())
                                          .workflowExecutionId(executionContext.getWorkflowExecutionId())
                                          .workflowId(executionContext.getWorkflowId())
                                          .commandUnits(emptyList())
                                          .status(RUNNING)
                                          .environmentId(env.getUuid())
                                          .environmentName(env.getName())
                                          .environmentType(env.getEnvironmentType());
    return activityService.save(activityBuilder.build());
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}
}
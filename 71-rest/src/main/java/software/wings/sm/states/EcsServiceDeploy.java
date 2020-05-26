package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.SKIPPED;
import static io.harness.exception.ExceptionUtils.getMessage;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.api.CommandStateExecutionData.Builder.aCommandStateExecutionData;
import static software.wings.beans.command.CommandUnitDetails.CommandUnitType.AWS_ECS_SERVICE_DEPLOY;
import static software.wings.beans.command.EcsResizeParams.EcsResizeParamsBuilder.anEcsResizeParams;
import static software.wings.sm.StateExecutionData.StateExecutionDataBuilder.aStateExecutionData;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.Getter;
import lombok.Setter;
import software.wings.api.CommandStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.command.EcsResizeParams;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.ecs.request.EcsServiceDeployRequest;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EcsServiceDeploy extends State {
  public static final String ECS_SERVICE_DEPLOY = "ECS Service Deploy";
  @Getter @Setter private String instanceCount;
  @Getter @Setter private String downsizeInstanceCount;
  @Getter @Setter private InstanceUnitType instanceUnitType = InstanceUnitType.PERCENTAGE;
  @Getter @Setter private InstanceUnitType downsizeInstanceUnitType = InstanceUnitType.PERCENTAGE;

  @Inject private transient SecretManager secretManager;
  @Inject private transient EcsStateHelper ecsStateHelper;
  @Inject private transient SettingsService settingsService;
  @Inject private transient DelegateService delegateService;
  @Inject private transient ActivityService activityService;
  @Inject private transient ServiceResourceService serviceResourceService;
  @Inject private transient ServiceTemplateService serviceTemplateService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient ContainerDeploymentManagerHelper containerDeploymentHelper;

  public EcsServiceDeploy(String name) {
    super(name, StateType.ECS_SERVICE_DEPLOY.name());
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      return executeInternal(context);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(getMessage(e), e);
    }
  }

  private ExecutionResponse executeInternal(ExecutionContext context) {
    EcsDeployDataBag deployDataBag = ecsStateHelper.prepareBagForEcsDeploy(
        context, serviceResourceService, infrastructureMappingService, settingsService, secretManager, false);
    if (deployDataBag.getContainerElement() == null) {
      return ExecutionResponse.builder()
          .executionStatus(SKIPPED)
          .stateExecutionData(aStateExecutionData().withErrorMsg("No container setup element found. Skipping.").build())
          .build();
    }

    Activity activity = ecsStateHelper.createActivity(
        context, ECS_SERVICE_DEPLOY, getStateType(), AWS_ECS_SERVICE_DEPLOY, activityService);

    CommandStateExecutionData executionData = aCommandStateExecutionData()
                                                  .withServiceId(deployDataBag.getService().getUuid())
                                                  .withServiceName(deployDataBag.getService().getName())
                                                  .withAppId(deployDataBag.getApp().getUuid())
                                                  .withCommandName(ECS_SERVICE_DEPLOY)
                                                  .withClusterName(deployDataBag.getContainerElement().getClusterName())
                                                  .withActivityId(activity.getUuid())
                                                  .build();

    EcsResizeParams resizeParams =
        anEcsResizeParams()
            .withRollback(false)
            .withRegion(deployDataBag.getRegion())
            .withInstanceUnitType(getInstanceUnitType())
            .withImage(deployDataBag.getContainerElement().getImage())
            .withClusterName(deployDataBag.getContainerElement().getClusterName())
            .withContainerServiceName(deployDataBag.getContainerElement().getName())
            .withMaxInstances(deployDataBag.getContainerElement().getMaxInstances())
            .withFixedInstances(deployDataBag.getContainerElement().getFixedInstances())
            .withResizeStrategy(deployDataBag.getContainerElement().getResizeStrategy())
            .withInstanceCount(Integer.valueOf(context.renderExpression(getInstanceCount())))
            .withUseFixedInstances(deployDataBag.getContainerElement().isUseFixedInstances())
            .withContainerServiceName(deployDataBag.getContainerElement().getNewEcsServiceName())
            .withOriginalServiceCounts(deployDataBag.getContainerElement().getActiveServiceCounts())
            .withServiceSteadyStateTimeout(deployDataBag.getContainerElement().getServiceSteadyStateTimeout())
            .withPreviousAwsAutoScalarConfigs(deployDataBag.getContainerElement().getPreviousAwsAutoScalarConfigs())
            .withAwsAutoScalarConfigForNewService(deployDataBag.getContainerElement().getNewServiceAutoScalarConfig())
            .withPreviousEcsAutoScalarsAlreadyRemoved(
                deployDataBag.getContainerElement().isPrevAutoscalarsAlreadyRemoved())
            .withDownsizeInstanceCount(getDownsizeCount(context))
            .withDownsizeInstanceUnitType(getDownsizeInstanceUnitType())
            .build();

    EcsServiceDeployRequest request = EcsServiceDeployRequest.builder()
                                          .accountId(deployDataBag.getApp().getAccountId())
                                          .appId(deployDataBag.getApp().getUuid())
                                          .commandName(ECS_SERVICE_DEPLOY)
                                          .activityId(activity.getUuid())
                                          .region(deployDataBag.getRegion())
                                          .cluster(deployDataBag.getEcsInfrastructureMapping().getClusterName())
                                          .awsConfig(deployDataBag.getAwsConfig())
                                          .ecsResizeParams(resizeParams)
                                          .build();

    String delegateTaskId =
        ecsStateHelper.createAndQueueDelegateTaskForEcsServiceDeploy(deployDataBag, request, activity, delegateService);

    return ExecutionResponse.builder()
        .async(true)
        .correlationIds(singletonList(activity.getUuid()))
        .stateExecutionData(executionData)
        .delegateTaskId(delegateTaskId)
        .build();
  }

  private Integer getDownsizeCount(ExecutionContext context) {
    return isNotBlank(getDownsizeInstanceCount())
        ? Integer.valueOf(context.renderExpression(getDownsizeInstanceCount()))
        : null;
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      return ecsStateHelper.handleDelegateResponseForEcsDeploy(
          context, response, false, activityService, serviceTemplateService, containerDeploymentHelper);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(getMessage(e), e);
    }
  }

  @Override
  public Map<String, String> validateFields() {
    Map<String, String> invalidFields = new HashMap<>();
    if (!isRollback() && isBlank(getInstanceCount())) {
      invalidFields.put("instanceCount", "Instance count must not be blank");
    }
    return invalidFields;
  }

  public static final class EcsServiceDeployBuilder {
    private String id;
    private String name;
    private ContextElementType requiredContextElementType;
    private String stateType;
    private String commandName;
    private String instanceCount;
    private InstanceUnitType instanceUnitType = InstanceUnitType.COUNT;

    private EcsServiceDeployBuilder(String name) {
      this.name = name;
    }

    public static EcsServiceDeployBuilder anEcsServiceDeploy(String name) {
      return new EcsServiceDeployBuilder(name);
    }

    public EcsServiceDeployBuilder withId(String id) {
      this.id = id;
      return this;
    }

    public EcsServiceDeployBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public EcsServiceDeployBuilder withRequiredContextElementType(ContextElementType requiredContextElementType) {
      this.requiredContextElementType = requiredContextElementType;
      return this;
    }

    public EcsServiceDeployBuilder withStateType(String stateType) {
      this.stateType = stateType;
      return this;
    }

    public EcsServiceDeployBuilder withCommandName(String commandName) {
      this.commandName = commandName;
      return this;
    }

    public EcsServiceDeployBuilder withInstanceCount(String instanceCount) {
      this.instanceCount = instanceCount;
      return this;
    }

    public EcsServiceDeployBuilder withInstanceUnitType(InstanceUnitType instanceUnitType) {
      this.instanceUnitType = instanceUnitType;
      return this;
    }

    public EcsServiceDeploy build() {
      EcsServiceDeploy ecsServiceDeploy = new EcsServiceDeploy(name);
      ecsServiceDeploy.setId(id);
      ecsServiceDeploy.setRequiredContextElementType(requiredContextElementType);
      ecsServiceDeploy.setStateType(stateType);
      ecsServiceDeploy.setRollback(false);
      ecsServiceDeploy.setInstanceCount(instanceCount);
      ecsServiceDeploy.setInstanceUnitType(instanceUnitType);
      return ecsServiceDeploy;
    }
  }
}

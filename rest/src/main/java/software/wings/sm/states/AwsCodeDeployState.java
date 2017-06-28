package software.wings.sm.states;

import static java.util.Collections.singletonList;
import static software.wings.api.CommandStateExecutionData.Builder.aCommandStateExecutionData;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.InstanceElementListParam.InstanceElementListParamBuilder.anInstanceElementListParam;
import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;
import static software.wings.beans.Activity.Builder.anActivity;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;

import com.google.inject.Inject;

import com.amazonaws.services.codedeploy.model.RevisionLocation;
import com.amazonaws.services.codedeploy.model.S3Location;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.lang.StringUtils;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.InstanceElement;
import software.wings.api.InstanceElementListParam;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.CodeDeployInfrastructureMapping;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.command.CodeDeployCommandExecutionData;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandExecutionContext.CodeDeployParams;
import software.wings.beans.command.CommandExecutionContext.CodeDeployParams.Builder;
import software.wings.beans.command.CommandExecutionResult;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.cloudprovider.aws.AwsCodeDeployService;
import software.wings.common.Constants;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by brett on 6/22/17
 */
public class AwsCodeDeployState extends State {
  private static final Logger logger = LoggerFactory.getLogger(AwsCodeDeployState.class);

  @Attributes(title = "Bucket", required = true) private String bucket;
  @Attributes(title = "Key", required = true) private String key;
  @Attributes(title = "Bundle Type", required = true) private String bundleType;

  @Attributes(title = "Command")
  @EnumData(enumDataProvider = CommandStateEnumDataProvider.class)
  @DefaultValue("Amazon Code Deploy")
  private String commandName = "Amazon Code Deploy";

  @Inject @Transient protected transient AwsCodeDeployService awsCodeDeployService;

  @Inject @Transient protected transient SettingsService settingsService;

  @Inject @Transient protected transient DelegateService delegateService;

  @Inject @Transient protected transient ServiceResourceService serviceResourceService;

  @Inject @Transient protected transient ActivityService activityService;

  @Inject @Transient protected transient InfrastructureMappingService infrastructureMappingService;

  @Inject @Transient protected transient ServiceTemplateService serviceTemplateService;

  @Inject @Transient private transient AwsHelperService awsHelperService;

  public AwsCodeDeployState(String name) {
    super(name, StateType.AWS_CODEDEPLOY_STATE.name());
  }

  protected AwsCodeDeployState(String name, String type) {
    super(name, type);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    CommandStateExecutionData.Builder executionDataBuilder = aCommandStateExecutionData();

    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Application app = workflowStandardParams.getApp();
    Environment env = workflowStandardParams.getEnv();

    Service service = serviceResourceService.get(app.getUuid(), serviceId);
    Command command =
        serviceResourceService.getCommandByName(app.getUuid(), serviceId, env.getUuid(), getCommandName()).getCommand();

    CodeDeployInfrastructureMapping infrastructureMapping =
        (CodeDeployInfrastructureMapping) infrastructureMappingService.get(
            app.getUuid(), phaseElement.getInfraMappingId());

    SettingAttribute cloudProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    String region = infrastructureMapping.getRegion();

    Activity.Builder activityBuilder = anActivity()
                                           .withAppId(app.getUuid())
                                           .withApplicationName(app.getName())
                                           .withEnvironmentId(env.getUuid())
                                           .withEnvironmentName(env.getName())
                                           .withEnvironmentType(env.getEnvironmentType())
                                           .withServiceId(service.getUuid())
                                           .withServiceName(service.getName())
                                           .withCommandName(command.getName())
                                           .withType(Type.Command)
                                           .withWorkflowExecutionId(context.getWorkflowExecutionId())
                                           .withWorkflowType(context.getWorkflowType())
                                           .withWorkflowExecutionName(context.getWorkflowExecutionName())
                                           .withStateExecutionInstanceId(context.getStateExecutionInstanceId())
                                           .withStateExecutionInstanceName(context.getStateExecutionInstanceName())
                                           .withCommandUnits(serviceResourceService.getFlattenCommandUnitList(
                                               app.getUuid(), serviceId, env.getUuid(), command.getName()))
                                           .withCommandType(command.getCommandUnitType().name())
                                           .withServiceVariables(context.getServiceVariables());

    Activity activity = activityService.save(activityBuilder.build());

    executionDataBuilder.withServiceId(service.getUuid())
        .withServiceName(service.getName())
        .withAppId(app.getUuid())
        .withCommandName(getCommandName())
        .withActivityId(activity.getUuid());

    CodeDeployParams codeDeployParams =
        prepareCodeDeployParams(context, infrastructureMapping, cloudProviderSetting, executionDataBuilder);

    CommandExecutionContext commandExecutionContext = aCommandExecutionContext()
                                                          .withAccountId(app.getAccountId())
                                                          .withAppId(app.getUuid())
                                                          .withEnvId(env.getUuid())
                                                          .withServiceName(service.getName())
                                                          .withRegion(region)
                                                          .withActivityId(activity.getUuid())
                                                          .withCloudProviderSetting(cloudProviderSetting)
                                                          .withCodeDeployParams(codeDeployParams)
                                                          .build();

    String delegateTaskId =
        delegateService.queueTask(DelegateTask.Builder.aDelegateTask()
                                      .withAccountId(app.getAccountId())
                                      .withAppId(app.getAppId())
                                      .withTaskType(TaskType.COMMAND)
                                      .withWaitId(activity.getUuid())
                                      .withParameters(new Object[] {command, commandExecutionContext})
                                      .build());

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(singletonList(activity.getUuid()))
        .withStateExecutionData(executionDataBuilder.build())
        .withDelegateTaskId(delegateTaskId)
        .build();
  }

  protected CodeDeployParams prepareCodeDeployParams(ExecutionContext context,
      CodeDeployInfrastructureMapping infrastructureMapping, SettingAttribute cloudProviderSetting,
      CommandStateExecutionData.Builder executionDataBuilder) {
    String key = context.renderExpression(this.key);
    String bucket = context.renderExpression(this.bucket);

    CodeDeployParams codeDeployParams =
        new Builder()
            .withApplicationName(infrastructureMapping.getApplicationName())
            .withDeploymentGroupName(infrastructureMapping.getDeploymentGroup())
            .withRegion(infrastructureMapping.getRegion())
            .withDeploymentConfigurationName(infrastructureMapping.getDeploymentConfig())
            .withBucket(bucket)
            .withKey(key)
            .withBundleType(bundleType)
            .build();
    executionDataBuilder.withCodeDeployParams(codeDeployParams);

    RevisionLocation revisionLocation = awsCodeDeployService.getApplicationRevisionList(codeDeployParams.getRegion(),
        codeDeployParams.getApplicationName(), codeDeployParams.getDeploymentGroupName(), cloudProviderSetting);
    if (revisionLocation != null && revisionLocation.getS3Location() != null) {
      S3Location s3Location = revisionLocation.getS3Location();
      CodeDeployParams oldCodeDeployParams =
          new Builder()
              .withApplicationName(codeDeployParams.getApplicationName())
              .withDeploymentGroupName(codeDeployParams.getDeploymentGroupName())
              .withDeploymentConfigurationName(codeDeployParams.getDeploymentConfigurationName())
              .withBucket(s3Location.getBucket())
              .withKey(s3Location.getKey())
              .withBundleType(s3Location.getBundleType())
              .build();
      executionDataBuilder.withOldCodeDeployParams(oldCodeDeployParams);
    }
    return codeDeployParams;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    CommandStateExecutionData commandStateExecutionData = (CommandStateExecutionData) context.getStateExecutionData();
    CommandExecutionResult commandExecutionResult = (CommandExecutionResult) response.values().iterator().next();

    ExecutionStatus status =
        commandExecutionResult != null && CommandExecutionStatus.SUCCESS.equals(commandExecutionResult.getStatus())
        ? ExecutionStatus.SUCCESS
        : ExecutionStatus.FAILED;
    activityService.updateStatus(
        commandStateExecutionData.getActivityId(), commandStateExecutionData.getAppId(), status);

    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Application app = workflowStandardParams.getApp();
    Environment env = workflowStandardParams.getEnv();
    Key<ServiceTemplate> serviceTemplateKey =
        serviceTemplateService.getTemplateRefKeysByService(app.getUuid(), serviceId, env.getUuid()).get(0);

    InstanceElementListParam instanceElementListParam = anInstanceElementListParam().build();
    List<InstanceStatusSummary> instanceStatusSummaries = new ArrayList<>();
    if (commandExecutionResult != null && commandExecutionResult.getCommandExecutionData() != null) {
      CodeDeployCommandExecutionData commandExecutionData =
          (CodeDeployCommandExecutionData) commandExecutionResult.getCommandExecutionData();
      List<InstanceElement> instanceElements = new ArrayList<>();
      commandExecutionData.getInstances().forEach(instance -> {
        String hostName = awsHelperService.getHostnameFromDnsName(instance.getPrivateDnsName());
        InstanceElement instanceElement =
            anInstanceElement()
                .withUuid(instance.getInstanceId())
                .withHostName(hostName)
                .withDisplayName(instance.getPublicDnsName())
                .withHostElement(
                    aHostElement().withHostName(hostName).withPublicDns(instance.getPublicDnsName()).build())
                .withServiceTemplateElement(aServiceTemplateElement()
                                                .withUuid(serviceTemplateKey.getId().toString())
                                                .withServiceElement(phaseElement.getServiceElement())
                                                .build())
                .build();
        instanceElements.add(instanceElement);

        instanceStatusSummaries.add(
            anInstanceStatusSummary().withInstanceElement(instanceElement).withStatus(ExecutionStatus.SUCCESS).build());
      });
      instanceElementListParam = anInstanceElementListParam().withInstanceElements(instanceElements).build();
      commandStateExecutionData.setNewInstanceStatusSummaries(instanceStatusSummaries);
    }

    return anExecutionResponse()
        .withStateExecutionData(commandStateExecutionData)
        .withExecutionStatus(status)
        .addContextElement(instanceElementListParam)
        .addNotifyElement(instanceElementListParam)
        .build();
  }

  @Override
  public Map<String, String> validateFields() {
    Map<String, String> invalidFields = new HashMap<>();
    if (!isRollback()) {
      if (StringUtils.isBlank(bucket)) {
        invalidFields.put("bucket", "Bucket should not be empty");
      }
      if (StringUtils.isBlank(key)) {
        invalidFields.put("key", "Key should not be empty");
      }
      if (StringUtils.isBlank(bundleType)) {
        invalidFields.put("bundleType", "Bundle Type should not be empty");
      }
    }
    if (getCommandName() == null) {
      invalidFields.put("commandName", "Command Name should not be null");
    }
    return invalidFields;
  }

  @SchemaIgnore
  public String getCommandName() {
    return commandName;
  }

  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getBundleType() {
    return bundleType;
  }

  public void setBundleType(String bundleType) {
    this.bundleType = bundleType;
  }
}

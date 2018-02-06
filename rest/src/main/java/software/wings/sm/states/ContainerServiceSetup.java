package software.wings.sm.states;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static software.wings.api.CommandStateExecutionData.Builder.aCommandStateExecutionData;
import static software.wings.api.InstanceElementListParam.InstanceElementListParamBuilder.anInstanceElementListParam;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.FeatureName.ECS_CREATE_CLUSTER;
import static software.wings.beans.FeatureName.KUBERNETES_CREATE_CLUSTER;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;
import static software.wings.beans.artifact.ArtifactStreamType.ECR;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;
import static software.wings.beans.artifact.ArtifactStreamType.NEXUS;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.api.ClusterElement;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.ContainerServiceElement;
import software.wings.api.InstanceElementListParam;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.DockerConfig;
import software.wings.beans.EcrConfig;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactoryArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.beans.artifact.GcrArtifactStream;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandExecutionResult;
import software.wings.beans.command.ContainerSetupParams;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.ImageDetails;
import software.wings.beans.container.ImageDetails.ImageDetailsBuilder;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.ecr.EcrClassicService;
import software.wings.helpers.ext.ecr.EcrService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;
import software.wings.waitnotify.NotifyResponseData;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by brett on 9/29/17
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class ContainerServiceSetup extends State {
  @Transient private static final Logger logger = LoggerFactory.getLogger(ContainerServiceSetup.class);

  static final String FIXED_INSTANCES = "fixedInstances";
  static final int DEFAULT_MAX = 2;

  private String desiredInstanceCount;
  private int fixedInstances;
  private int maxInstances; // Named minimum in the UI
  private ResizeStrategy resizeStrategy;
  private int serviceSteadyStateTimeout; // Minutes
  @Inject @Transient private transient EcrService ecrService;
  @Inject @Transient private transient EcrClassicService ecrClassicService;
  @Inject @Transient private transient AwsHelperService awsHelperService;
  @Inject @Transient protected transient SettingsService settingsService;
  @Inject @Transient protected transient ServiceResourceService serviceResourceService;
  @Inject @Transient protected transient InfrastructureMappingService infrastructureMappingService;
  @Inject @Transient protected transient ArtifactStreamService artifactStreamService;
  @Inject @Transient protected transient FeatureFlagService featureFlagService;
  @Inject @Transient protected transient SecretManager secretManager;
  @Inject @Transient protected transient EncryptionService encryptionService;
  @Inject @Transient protected transient ActivityService activityService;
  @Inject @Transient protected transient DelegateService delegateService;

  ContainerServiceSetup(String name, String type) {
    super(name, type);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
      String serviceId = phaseElement.getServiceElement().getUuid();

      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      Artifact artifact = ((DeploymentExecutionContext) context).getArtifactForService(serviceId);
      if (artifact == null) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT).addParam("args", "Artifact is null");
      }

      ImageDetails imageDetails = fetchArtifactDetails(artifact, context);

      Application app = workflowStandardParams.getApp();
      Environment env = workflowStandardParams.getEnv();

      Service service = serviceResourceService.get(app.getUuid(), serviceId);

      logger.info("Setting up container service for account {}, app {}, service {}", app.getAccountId(), app.getUuid(),
          service.getName());
      ContainerTask containerTask =
          serviceResourceService.getContainerTaskByDeploymentType(app.getUuid(), serviceId, getDeploymentType());

      InfrastructureMapping infrastructureMapping =
          infrastructureMappingService.get(app.getUuid(), phaseElement.getInfraMappingId());
      if (infrastructureMapping == null || !(infrastructureMapping instanceof ContainerInfrastructureMapping)
          || !isValidInfraMapping(infrastructureMapping)) {
        throw new WingsException(ErrorCode.INVALID_REQUEST).addParam("message", "Invalid infrastructure type");
      }

      ContainerInfrastructureMapping containerInfrastructureMapping =
          (ContainerInfrastructureMapping) infrastructureMapping;

      String clusterName = containerInfrastructureMapping.getClusterName();
      if (!(infrastructureMapping instanceof DirectKubernetesInfrastructureMapping)
          && Constants.RUNTIME.equals(clusterName)) {
        if ((infrastructureMapping instanceof GcpKubernetesInfrastructureMapping
                && featureFlagService.isEnabled(KUBERNETES_CREATE_CLUSTER, app.getAccountId()))
            || (infrastructureMapping instanceof EcsInfrastructureMapping
                   && featureFlagService.isEnabled(ECS_CREATE_CLUSTER, app.getAccountId()))) {
          clusterName = getClusterNameFromContextElement(context);
        } else {
          throw new WingsException(ErrorCode.INVALID_REQUEST)
              .addParam("message", "Runtime creation of clusters is not yet supported.");
        }
      }

      Command command =
          serviceResourceService.getCommandByName(app.getUuid(), serviceId, env.getUuid(), getCommandName())
              .getCommand();

      Activity activity = buildActivity(context, app, env, service, command);

      SettingAttribute settingAttribute = infrastructureMapping instanceof DirectKubernetesInfrastructureMapping
          ? aSettingAttribute()
                .withValue(((DirectKubernetesInfrastructureMapping) infrastructureMapping).createKubernetesConfig())
                .build()
          : settingsService.get(infrastructureMapping.getComputeProviderSettingId());

      List<EncryptedDataDetail> encryptedDataDetails = secretManager.getEncryptionDetails(
          (Encryptable) settingAttribute.getValue(), context.getAppId(), context.getWorkflowExecutionId());

      ContainerSetupParams containerSetupParams = buildContainerSetupParams(context, service.getName(), imageDetails,
          app, env, containerInfrastructureMapping, containerTask, clusterName);

      CommandStateExecutionData executionData = aCommandStateExecutionData()
                                                    .withServiceId(service.getUuid())
                                                    .withServiceName(service.getName())
                                                    .withAppId(app.getUuid())
                                                    .withCommandName(getCommandName())
                                                    .withContainerSetupParams(containerSetupParams)
                                                    .withClusterName(clusterName)
                                                    .withActivityId(activity.getUuid())
                                                    .build();

      CommandExecutionContext commandExecutionContext =
          aCommandExecutionContext()
              .withAccountId(app.getAccountId())
              .withAppId(app.getUuid())
              .withEnvId(env.getUuid())
              .withContainerSetupParams(containerSetupParams)
              .withActivityId(activity.getUuid())
              .withCloudProviderSetting(settingAttribute)
              .withCloudProviderCredentials(encryptedDataDetails)
              .withServiceVariables(context.getServiceVariables())
              .withSafeDisplayServiceVariables(context.getSafeDisplayServiceVariables())
              .build();

      String delegateTaskId =
          delegateService.queueTask(aDelegateTask()
                                        .withAccountId(app.getAccountId())
                                        .withAppId(app.getUuid())
                                        .withTaskType(TaskType.COMMAND)
                                        .withWaitId(activity.getUuid())
                                        .withParameters(new Object[] {command, commandExecutionContext})
                                        .withEnvId(env.getUuid())
                                        .withInfrastructureMappingId(infrastructureMapping.getUuid())
                                        .withTimeout(TimeUnit.HOURS.toMillis(1))
                                        .build());

      return anExecutionResponse()
          .withAsync(true)
          .withCorrelationIds(singletonList(activity.getUuid()))
          .withStateExecutionData(executionData)
          .withDelegateTaskId(delegateTaskId)
          .build();

    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      throw new WingsException(ErrorCode.INVALID_REQUEST, e).addParam("message", e.getMessage());
    }
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    try {
      logger.info("Received async response");
      CommandStateExecutionData executionData = (CommandStateExecutionData) context.getStateExecutionData();
      CommandExecutionResult commandExecutionResult = (CommandExecutionResult) response.values().iterator().next();

      if (commandExecutionResult == null || commandExecutionResult.getStatus() != SUCCESS) {
        return buildEndStateExecution(executionData, commandExecutionResult, ExecutionStatus.FAILED);
      }

      return buildEndStateExecution(executionData, commandExecutionResult, ExecutionStatus.SUCCESS);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      throw new WingsException(ErrorCode.INVALID_REQUEST, e).addParam("message", e.getMessage());
    }
  }

  private ExecutionResponse buildEndStateExecution(
      CommandStateExecutionData executionData, CommandExecutionResult executionResult, ExecutionStatus status) {
    activityService.updateStatus(executionData.getActivityId(), executionData.getAppId(), status);

    ContainerServiceElement containerServiceElement =
        buildContainerServiceElement(executionData, executionResult, status);

    InstanceElementListParam instanceElementListParam =
        anInstanceElementListParam()
            .withInstanceElements(Optional
                                      .ofNullable(executionData.getNewInstanceStatusSummaries()
                                                      .stream()
                                                      .map(InstanceStatusSummary::getInstanceElement)
                                                      .collect(Collectors.toList()))
                                      .orElse(emptyList()))
            .build();

    return anExecutionResponse()
        .withStateExecutionData(executionData)
        .withExecutionStatus(status)
        .addContextElement(containerServiceElement)
        .addContextElement(instanceElementListParam)
        .addNotifyElement(containerServiceElement)
        .addNotifyElement(instanceElementListParam)
        .build();
  }

  public String getDesiredInstanceCount() {
    return desiredInstanceCount;
  }

  public void setDesiredInstanceCount(String desiredInstanceCount) {
    this.desiredInstanceCount = desiredInstanceCount;
  }

  public int getFixedInstances() {
    return fixedInstances;
  }

  public void setFixedInstances(int fixedInstances) {
    this.fixedInstances = fixedInstances;
  }

  public int getMaxInstances() {
    return maxInstances;
  }

  public void setMaxInstances(int maxInstances) {
    this.maxInstances = maxInstances;
  }

  public ResizeStrategy getResizeStrategy() {
    return resizeStrategy;
  }

  public void setResizeStrategy(ResizeStrategy resizeStrategy) {
    this.resizeStrategy = resizeStrategy;
  }

  public long getServiceSteadyStateTimeout() {
    return serviceSteadyStateTimeout;
  }

  public void setServiceSteadyStateTimeout(int serviceSteadyStateTimeout) {
    this.serviceSteadyStateTimeout = serviceSteadyStateTimeout;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  private ImageDetails fetchArtifactDetails(Artifact artifact, ExecutionContext context) {
    ImageDetailsBuilder imageDetails = ImageDetails.builder().tag(artifact.getBuildNo());
    ArtifactStream artifactStream = artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId());
    String settingId = artifactStream.getSettingId();
    if (artifactStream.getArtifactStreamType().equals(DOCKER.name())) {
      DockerArtifactStream dockerArtifactStream = (DockerArtifactStream) artifactStream;
      DockerConfig dockerConfig = (DockerConfig) settingsService.get(settingId).getValue();
      encryptionService.decrypt(dockerConfig,
          secretManager.getEncryptionDetails(dockerConfig, context.getAppId(), context.getWorkflowExecutionId()));
      imageDetails.name(dockerArtifactStream.getImageName())
          .sourceName(dockerArtifactStream.getSourceName())
          .registryUrl(dockerConfig.getDockerRegistryUrl())
          .username(dockerConfig.getUsername())
          .password(new String(dockerConfig.getPassword()));
    } else if (artifactStream.getArtifactStreamType().equals(ECR.name())) {
      EcrArtifactStream ecrArtifactStream = (EcrArtifactStream) artifactStream;
      String imageUrl = getImageUrl(ecrArtifactStream, context);
      // name should be 830767422336.dkr.ecr.us-east-1.amazonaws.com/todolist
      // sourceName should be todolist
      // registryUrl should be https://830767422336.dkr.ecr.us-east-1.amazonaws.com/
      imageDetails.name(imageUrl)
          .sourceName(ecrArtifactStream.getSourceName())
          .registryUrl("https://" + imageUrl + (imageUrl.endsWith("/") ? "" : "/"))
          .username("AWS");
      SettingValue settingValue = settingsService.get(settingId).getValue();

      // All the new ECR artifact streams use cloud provider AWS settings for accesskey and secret
      if (SettingVariableTypes.AWS.name().equals(settingValue.getType())) {
        AwsConfig awsConfig = (AwsConfig) settingsService.get(settingId).getValue();
        encryptionService.decrypt(awsConfig,
            secretManager.getEncryptionDetails(awsConfig, context.getAppId(), context.getWorkflowExecutionId()));
        imageDetails.password(awsHelperService.getAmazonEcrAuthToken(imageUrl.substring(0, imageUrl.indexOf('.')),
            ecrArtifactStream.getRegion(), awsConfig.getAccessKey(), awsConfig.getSecretKey()));
      } else {
        // There is a point when old ECR artifact streams would be using the old ECR Artifact Server definition until
        // migration happens. The deployment code handles both the cases.
        EcrConfig ecrConfig = (EcrConfig) settingsService.get(settingId).getValue();
        imageDetails.password(awsHelperService.getAmazonEcrAuthToken(ecrConfig,
            secretManager.getEncryptionDetails(ecrConfig, context.getAppId(), context.getWorkflowExecutionId())));
      }
    } else if (artifactStream.getArtifactStreamType().equals(GCR.name())) {
      GcrArtifactStream gcrArtifactStream = (GcrArtifactStream) artifactStream;
      String imageName = gcrArtifactStream.getRegistryHostName() + "/" + gcrArtifactStream.getDockerImageName();
      imageDetails.name(imageName).sourceName(imageName).registryUrl(imageName);
    } else if (artifactStream.getArtifactStreamType().equals(ARTIFACTORY.name())) {
      ArtifactoryArtifactStream artifactoryArtifactStream = (ArtifactoryArtifactStream) artifactStream;
      ArtifactoryConfig artifactoryConfig = (ArtifactoryConfig) settingsService.get(settingId).getValue();
      encryptionService.decrypt(artifactoryConfig,
          secretManager.getEncryptionDetails(artifactoryConfig, context.getAppId(), context.getWorkflowExecutionId()));
      String url = artifactoryConfig.getArtifactoryUrl();
      if (artifactoryArtifactStream.getDockerRepositoryServer() != null) {
        String registryUrl = String.format(
            "http%s://%s", url.startsWith("https") ? "s" : "", artifactoryArtifactStream.getDockerRepositoryServer());

        imageDetails
            .name(
                artifactoryArtifactStream.getDockerRepositoryServer() + "/" + artifactoryArtifactStream.getImageName())
            .sourceName(artifactoryArtifactStream.getSourceName())
            .registryUrl(registryUrl)
            .username(artifactoryConfig.getUsername())
            .password(new String(artifactoryConfig.getPassword()));
      } else {
        int firstDotIndex = url.indexOf('.');
        int slashAfterDomain = url.indexOf('/', firstDotIndex);
        String registryUrl = url.substring(0, firstDotIndex) + "-" + artifactoryArtifactStream.getJobname()
            + url.substring(firstDotIndex, slashAfterDomain > 0 ? slashAfterDomain : url.length());
        String namePrefix = registryUrl.substring(registryUrl.indexOf("://") + 3);
        imageDetails.name(namePrefix + "/" + artifactoryArtifactStream.getImageName())
            .sourceName(artifactoryArtifactStream.getSourceName())
            .registryUrl(registryUrl)
            .username(artifactoryConfig.getUsername())
            .password(new String(artifactoryConfig.getPassword()));
      }
    } else if (artifactStream.getArtifactStreamType().equals(NEXUS.name())) {
      NexusArtifactStream nexusArtifactStream = (NexusArtifactStream) artifactStream;
      NexusConfig nexusConfig = (NexusConfig) settingsService.get(settingId).getValue();
      encryptionService.decrypt(nexusConfig,
          secretManager.getEncryptionDetails(nexusConfig, context.getAppId(), context.getWorkflowExecutionId()));

      String url = nexusConfig.getNexusUrl();
      int firstDotIndex = url.indexOf('.');
      int colonIndex = url.indexOf(':', firstDotIndex);
      int endIndex = colonIndex > 0 ? colonIndex : url.length();
      String registryUrl = url.substring(0, endIndex) + ":"
          + (nexusArtifactStream.getDockerPort() != null ? nexusArtifactStream.getDockerPort() : "5000");
      String namePrefix = registryUrl.substring(registryUrl.indexOf("://") + 3);
      logger.info("Nexus Registry url: " + registryUrl);
      imageDetails.name(namePrefix + "/" + nexusArtifactStream.getImageName())
          .sourceName(nexusArtifactStream.getSourceName())
          .registryUrl(registryUrl)
          .username(nexusConfig.getUsername())
          .password(new String(nexusConfig.getPassword()));
    } else {
      throw new WingsException(ErrorCode.INVALID_REQUEST)
          .addParam(
              "message", artifactStream.getArtifactStreamType() + " artifact source can't be used for containers");
    }
    return imageDetails.build();
  }

  private String getImageUrl(EcrArtifactStream ecrArtifactStream, ExecutionContext context) {
    SettingAttribute settingAttribute = settingsService.get(ecrArtifactStream.getSettingId());
    SettingValue value = settingAttribute.getValue();
    if (SettingVariableTypes.AWS.name().equals(value.getType())) {
      AwsConfig awsConfig = (AwsConfig) value;
      return ecrService.getEcrImageUrl(awsConfig,
          secretManager.getEncryptionDetails(awsConfig, context.getAppId(), context.getWorkflowExecutionId()),
          ecrArtifactStream.getRegion(), ecrArtifactStream);
    } else {
      EcrConfig ecrConfig = (EcrConfig) value;
      return ecrClassicService.getEcrImageUrl(ecrConfig, ecrArtifactStream);
    }
  }

  private Activity buildActivity(
      ExecutionContext context, Application app, Environment env, Service service, Command command) {
    Activity activity = Activity.builder()
                            .applicationName(app.getName())
                            .environmentId(env.getUuid())
                            .environmentName(env.getName())
                            .environmentType(env.getEnvironmentType())
                            .serviceId(service.getUuid())
                            .serviceName(service.getName())
                            .commandName(command.getName())
                            .type(Activity.Type.Command)
                            .workflowExecutionId(context.getWorkflowExecutionId())
                            .workflowType(context.getWorkflowType())
                            .workflowId(context.getWorkflowId())
                            .workflowExecutionName(context.getWorkflowExecutionName())
                            .stateExecutionInstanceId(context.getStateExecutionInstanceId())
                            .stateExecutionInstanceName(context.getStateExecutionInstanceName())
                            .commandUnits(serviceResourceService.getFlattenCommandUnitList(
                                app.getUuid(), service.getUuid(), env.getUuid(), command.getName()))
                            .commandType(command.getCommandUnitType().name())
                            .serviceVariables(context.getServiceVariables())
                            .status(ExecutionStatus.RUNNING)
                            .build();

    activity.setAppId(app.getUuid());
    return activityService.save(activity);
  }

  protected String getClusterNameFromContextElement(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);

    Optional<ClusterElement> contextElement =
        context.<ClusterElement>getContextElementList(ContextElementType.CLUSTER)
            .stream()
            .filter(clusterElement -> phaseElement.getInfraMappingId().equals(clusterElement.getInfraMappingId()))
            .findFirst();

    return contextElement.isPresent() ? contextElement.get().getName() : "";
  }

  protected abstract String getDeploymentType();

  public abstract String getCommandName();

  protected abstract ContainerSetupParams buildContainerSetupParams(ExecutionContext context, String serviceName,
      ImageDetails imageDetails, Application app, Environment env, ContainerInfrastructureMapping infrastructureMapping,
      ContainerTask containerTask, String clusterName);

  protected abstract boolean isValidInfraMapping(InfrastructureMapping infrastructureMapping);

  protected abstract ContainerServiceElement buildContainerServiceElement(
      CommandStateExecutionData executionData, CommandExecutionResult executionResult, ExecutionStatus status);
}

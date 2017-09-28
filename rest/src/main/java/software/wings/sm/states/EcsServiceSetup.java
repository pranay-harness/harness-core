package software.wings.sm.states;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static software.wings.api.ContainerServiceElement.ContainerServiceElementBuilder.aContainerServiceElement;
import static software.wings.api.EcsServiceExecutionData.Builder.anEcsServiceExecutionData;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;
import static software.wings.beans.container.ContainerTask.CONTAINER_NAME_PLACEHOLDER_REGEX;
import static software.wings.beans.container.ContainerTask.DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.ECS_SERVICE_SETUP;
import static software.wings.utils.EcsConvention.getRevisionFromServiceName;
import static software.wings.utils.EcsConvention.getServiceNamePrefixFromServiceName;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.amazonaws.services.ecs.model.ContainerDefinition;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.DeploymentConfiguration;
import com.amazonaws.services.ecs.model.KeyValuePair;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.TaskDefinition;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ClusterElement;
import software.wings.api.ContainerServiceElement;
import software.wings.api.DeploymentType;
import software.wings.api.EcsServiceExecutionData;
import software.wings.api.PhaseElement;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.EcrConfig;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.ArtifactoryArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.beans.artifact.GcrArtifactStream;
import software.wings.beans.container.EcsContainerTask;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.ecr.EcrClassicService;
import software.wings.helpers.ext.ecr.EcrService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.EcsConvention;
import software.wings.utils.JsonUtils;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by peeyushaggarwal on 2/3/17.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EcsServiceSetup extends State {
  private static final int KEEP_N_REVISIONS = 3;

  @Transient private static final Logger logger = LoggerFactory.getLogger(EcsServiceSetup.class);

  // *** Note: UI Schema specified in wingsui/src/containers/WorkflowEditor/custom/ECSLoadBalancerModal.js

  private String ecsServiceName;
  private int maxInstances;
  private ResizeStrategy resizeStrategy;
  private int serviceSteadyStateTimeout = 10; // Minutes
  private boolean useLoadBalancer;
  private String loadBalancerName;
  private String targetGroupArn;
  private String roleArn;
  @Inject @Transient private transient AwsClusterService awsClusterService;
  @Inject @Transient private transient EcrService ecrService;
  @Inject @Transient private transient EcrClassicService ecrClassicService;
  @Inject @Transient private transient SettingsService settingsService;
  @Inject @Transient private transient ServiceResourceService serviceResourceService;
  @Inject @Transient private transient InfrastructureMappingService infrastructureMappingService;
  @Inject @Transient private transient ArtifactStreamService artifactStreamService;

  /**
   * Instantiates a new state.
   *
   * @param name the name
   */
  public EcsServiceSetup(String name) {
    super(name, ECS_SERVICE_SETUP.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
      String serviceId = phaseElement.getServiceElement().getUuid();

      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      Artifact artifact = workflowStandardParams.getArtifactForService(serviceId);
      String imageName = fetchArtifactImageName(artifact);

      Application app = workflowStandardParams.getApp();
      Environment env = workflowStandardParams.getEnv();

      InfrastructureMapping infrastructureMapping =
          infrastructureMappingService.get(app.getUuid(), phaseElement.getInfraMappingId());
      if (infrastructureMapping == null || !(infrastructureMapping instanceof EcsInfrastructureMapping)) {
        throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Invalid infrastructure type");
      }

      EcsInfrastructureMapping ecsInfrastructureMapping = (EcsInfrastructureMapping) infrastructureMapping;
      String region = ecsInfrastructureMapping.getRegion();
      String clusterName = ecsInfrastructureMapping.getClusterName();
      if (Constants.RUNTIME.equals(clusterName)) {
        String regionCluster = getClusterElement(context).getName();
        clusterName = regionCluster.split("/")[1];
      }

      String serviceName = serviceResourceService.get(app.getUuid(), serviceId).getName();
      SettingAttribute settingAttribute = settingsService.get(infrastructureMapping.getComputeProviderSettingId());

      EcsContainerTask ecsContainerTask = (EcsContainerTask) serviceResourceService.getContainerTaskByDeploymentType(
          app.getAppId(), serviceId, DeploymentType.ECS.name());

      if (ecsContainerTask == null) {
        ecsContainerTask = new EcsContainerTask();
        EcsContainerTask.ContainerDefinition containerDefinition = new EcsContainerTask.ContainerDefinition();
        containerDefinition.setMemory(256);
        containerDefinition.setPortMappings(emptyList());
        ecsContainerTask.setContainerDefinitions(Lists.newArrayList(containerDefinition));
      }

      String containerName = EcsConvention.getContainerName(imageName);
      String taskFamily = isNotEmpty(ecsServiceName)
          ? Misc.normalizeExpression(context.renderExpression(ecsServiceName))
          : EcsConvention.getTaskFamily(app.getName(), serviceName, env.getName());

      TaskDefinition taskDefinition = createTaskDefinition(ecsContainerTask, containerName, imageName, taskFamily,
          region, settingAttribute, context.getServiceVariables());

      String ecsServiceName = EcsConvention.getServiceName(taskDefinition.getFamily(), taskDefinition.getRevision());

      CreateServiceRequest createServiceRequest =
          new CreateServiceRequest()
              .withServiceName(ecsServiceName)
              .withCluster(clusterName)
              .withDesiredCount(0)
              .withDeploymentConfiguration(
                  new DeploymentConfiguration().withMaximumPercent(200).withMinimumHealthyPercent(100))
              .withTaskDefinition(taskDefinition.getFamily() + ":" + taskDefinition.getRevision());

      EcsServiceExecutionData.Builder ecsServiceExecutionDataBuilder = anEcsServiceExecutionData()
                                                                           .withEcsClusterName(clusterName)
                                                                           .withEcsServiceName(ecsServiceName)
                                                                           .withDockerImageName(imageName);

      int portToExpose =
          ecsContainerTask.getContainerDefinitions()
              .stream()
              .flatMap(containerDefinition
                  -> Optional.ofNullable(containerDefinition.getPortMappings()).orElse(emptyList()).stream())
              .filter(EcsContainerTask.PortMapping::isLoadBalancerPort)
              .findFirst()
              .map(EcsContainerTask.PortMapping::getContainerPort)
              .orElse(0);
      if (useLoadBalancer && portToExpose != 0) {
        createServiceRequest
            .withLoadBalancers(new com.amazonaws.services.ecs.model.LoadBalancer()
                                   .withContainerName(containerName)
                                   .withContainerPort(portToExpose)
                                   .withTargetGroupArn(targetGroupArn))
            .withRole(roleArn);
        ecsServiceExecutionDataBuilder.withLoadBalancerName(loadBalancerName)
            .withRoleArn(roleArn)
            .withTargetGroupArn(targetGroupArn);
      }

      logger.info("Creating ECS service {} in cluster {}", ecsServiceName, clusterName);
      awsClusterService.createService(region, settingAttribute, createServiceRequest);

      logger.info("Cleaning up old versions");
      cleanup(settingAttribute, region, ecsServiceName, clusterName);

      ContainerServiceElement containerServiceElement =
          aContainerServiceElement()
              .withUuid(serviceId)
              .withName(ecsServiceName)
              .withMaxInstances(maxInstances == 0 ? 10 : maxInstances)
              .withResizeStrategy(resizeStrategy == null ? RESIZE_NEW_FIRST : resizeStrategy)
              .withServiceSteadyStateTimeout(serviceSteadyStateTimeout)
              .withClusterName(clusterName)
              .withDeploymentType(DeploymentType.ECS)
              .withInfraMappingId(phaseElement.getInfraMappingId())
              .build();

      return anExecutionResponse()
          .withExecutionStatus(ExecutionStatus.SUCCESS)
          .addContextElement(containerServiceElement)
          .addNotifyElement(containerServiceElement)
          .withStateExecutionData(ecsServiceExecutionDataBuilder.build())
          .build();
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", e.getMessage(), e);
    }
  }

  private TaskDefinition createTaskDefinition(EcsContainerTask ecsContainerTask, String containerName, String imageName,
      String taskFamily, String region, SettingAttribute computeProviderSetting, Map<String, String> serviceVariables) {
    String configTemplate;
    if (StringUtils.isNotEmpty(ecsContainerTask.getAdvancedConfig())) {
      configTemplate = ecsContainerTask.fetchAdvancedConfigNoComments();
    } else {
      configTemplate = ecsContainerTask.fetchJsonConfig();
    }

    String config = configTemplate.replaceAll(DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX, imageName)
                        .replaceAll(CONTAINER_NAME_PLACEHOLDER_REGEX, containerName);
    TaskDefinition taskDefinition = JsonUtils.asObject(config, TaskDefinition.class);
    taskDefinition.setFamily(taskFamily);

    // Set service variables as environment variables
    if (serviceVariables != null && !serviceVariables.isEmpty()) {
      Map<String, KeyValuePair> serviceValuePairs = serviceVariables.entrySet().stream().collect(Collectors.toMap(
          Map.Entry::getKey, entry -> new KeyValuePair().withName(entry.getKey()).withValue(entry.getValue())));
      for (ContainerDefinition containerDefinition : taskDefinition.getContainerDefinitions()) {
        Map<String, KeyValuePair> valuePairsMap = new HashMap<>();
        if (containerDefinition.getEnvironment() != null) {
          containerDefinition.getEnvironment().forEach(
              keyValuePair -> valuePairsMap.put(keyValuePair.getName(), keyValuePair));
        }
        valuePairsMap.putAll(serviceValuePairs);
        containerDefinition.setEnvironment(new ArrayList<>(valuePairsMap.values()));
      }
    }

    RegisterTaskDefinitionRequest registerTaskDefinitionRequest =
        new RegisterTaskDefinitionRequest()
            .withContainerDefinitions(taskDefinition.getContainerDefinitions())
            .withFamily(taskDefinition.getFamily())
            .withTaskRoleArn(taskDefinition.getTaskRoleArn())
            .withNetworkMode(taskDefinition.getNetworkMode())
            .withPlacementConstraints(taskDefinition.getPlacementConstraints())
            .withVolumes(taskDefinition.getVolumes());

    logger.info("Creating task definition {} with container image {}", taskFamily, imageName);
    return awsClusterService.createTask(region, computeProviderSetting, registerTaskDefinitionRequest);
  }

  /**
   * Fetch artifact image name string.
   *
   * @param artifact the artifact
   * @return the string
   */
  private String fetchArtifactImageName(Artifact artifact) {
    ArtifactStream artifactStream = artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId());

    if (artifactStream.getArtifactStreamType().equals(ArtifactStreamType.DOCKER.name())) {
      DockerArtifactStream dockerArtifactStream = (DockerArtifactStream) artifactStream;
      return dockerArtifactStream.getImageName() + ":" + artifact.getBuildNo();
    } else if (artifactStream.getArtifactStreamType().equals(ArtifactStreamType.ECR.name())) {
      EcrArtifactStream ecrArtifactStream = (EcrArtifactStream) artifactStream;
      String imageUrl = getImageUrl(ecrArtifactStream);
      return imageUrl + ":" + artifact.getBuildNo();
    } else if (artifactStream.getArtifactStreamType().equals(ArtifactStreamType.GCR.name())) {
      GcrArtifactStream gcrArtifactStream = (GcrArtifactStream) artifactStream;
      return gcrArtifactStream.getDockerImageName() + ":" + artifact.getBuildNo();
    } else if (artifactStream.getArtifactStreamType().equals(ArtifactStreamType.ARTIFACTORY.name())) {
      ArtifactoryArtifactStream artifactoryArtifactStream = (ArtifactoryArtifactStream) artifactStream;
      return artifactoryArtifactStream.getImageName() + ":" + artifact.getBuildNo();
    } else {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message",
          artifactStream.getArtifactStreamType() + " artifact source can't be used for Containers");
    }
  }

  private String getImageUrl(EcrArtifactStream ecrArtifactStream) {
    SettingAttribute settingAttribute = settingsService.get(ecrArtifactStream.getSettingId());
    SettingValue value = settingAttribute.getValue();
    if (SettingVariableTypes.AWS.name().equals(value.getType())) {
      AwsConfig awsConfig = (AwsConfig) value;
      return ecrService.getEcrImageUrl(awsConfig, ecrArtifactStream.getRegion(), ecrArtifactStream);
    } else {
      EcrConfig ecrConfig = (EcrConfig) value;
      return ecrClassicService.getEcrImageUrl(ecrConfig, ecrArtifactStream);
    }
  }

  private ClusterElement getClusterElement(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);

    return context.<ClusterElement>getContextElementList(ContextElementType.CLUSTER)
        .stream()
        .filter(clusterElement -> phaseElement.getInfraMappingId().equals(clusterElement.getInfraMappingId()))
        .findFirst()
        .orElse(null);
  }

  private void cleanup(SettingAttribute settingAttribute, String region, String ecsServiceName, String clusterName) {
    int revision = getRevisionFromServiceName(ecsServiceName);
    if (revision > KEEP_N_REVISIONS) {
      int minRevisionToKeep = revision - KEEP_N_REVISIONS;
      String serviceNamePrefix = getServiceNamePrefixFromServiceName(ecsServiceName);
      awsClusterService.getServices(region, settingAttribute, clusterName)
          .stream()
          .filter(s -> s.getServiceName().startsWith(serviceNamePrefix) && s.getDesiredCount() == 0)
          .collect(Collectors.toList())
          .forEach(s -> {
            String oldServiceName = s.getServiceName();
            if (getRevisionFromServiceName(oldServiceName) < minRevisionToKeep) {
              logger.info("Deleting old version: " + oldServiceName);
              awsClusterService.deleteService(region, settingAttribute, clusterName, oldServiceName);
            }
          });
    }
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  /**
   * Gets load balancer setting id.
   *
   * @return the load balancer setting id
   */
  public String getLoadBalancerName() {
    return loadBalancerName;
  }

  /**
   * Sets load balancer setting id.
   *
   * @param loadBalancerName the load balancer setting id
   */
  public void setLoadBalancerName(String loadBalancerName) {
    this.loadBalancerName = loadBalancerName;
  }

  /**
   * Getter for property 'targetGroupArn'.
   *
   * @return Value for property 'targetGroupArn'.
   */
  public String getTargetGroupArn() {
    return targetGroupArn;
  }

  /**
   * Setter for property 'targetGroupArn'.
   *
   * @param targetGroupArn Value to set for property 'targetGroupArn'.
   */
  public void setTargetGroupArn(String targetGroupArn) {
    this.targetGroupArn = targetGroupArn;
  }

  /**
   * Getter for property 'roleArn'.
   *
   * @return Value for property 'roleArn'.
   */
  public String getRoleArn() {
    return roleArn;
  }

  /**
   * Setter for property 'roleArn'.
   *
   * @param roleArn Value to set for property 'roleArn'.
   */
  public void setRoleArn(String roleArn) {
    this.roleArn = roleArn;
  }

  /**
   * Getter for property 'useLoadBalancer'.
   *
   * @return Value for property 'useLoadBalancer'.
   */
  public boolean isUseLoadBalancer() {
    return useLoadBalancer;
  }

  /**
   * Setter for property 'useLoadBalancer'.
   *
   * @param useLoadBalancer Value to set for property 'useLoadBalancer'.
   */
  public void setUseLoadBalancer(boolean useLoadBalancer) {
    this.useLoadBalancer = useLoadBalancer;
  }

  public String getEcsServiceName() {
    return ecsServiceName;
  }

  public void setEcsServiceName(String ecsServiceName) {
    this.ecsServiceName = ecsServiceName;
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

  public static final class EcsServiceSetupBuilder {
    private String id;
    private String name;
    private int maxInstances;
    private ResizeStrategy resizeStrategy;
    private ContextElementType requiredContextElementType;
    private String stateType;
    private boolean rollback;
    private String serviceName;
    private boolean useLoadBalancer;
    private String loadBalancerName;
    private String targetGroupArn;
    private String roleArn;

    private EcsServiceSetupBuilder() {}

    public static EcsServiceSetupBuilder anEcsServiceSetup() {
      return new EcsServiceSetupBuilder();
    }

    public EcsServiceSetupBuilder withId(String id) {
      this.id = id;
      return this;
    }

    public EcsServiceSetupBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public EcsServiceSetupBuilder withMaxInstances(int maxInstances) {
      this.maxInstances = maxInstances;
      return this;
    }

    public EcsServiceSetupBuilder withResizeStrategy(ResizeStrategy resizeStrategy) {
      this.resizeStrategy = resizeStrategy;
      return this;
    }

    public EcsServiceSetupBuilder withRequiredContextElementType(ContextElementType requiredContextElementType) {
      this.requiredContextElementType = requiredContextElementType;
      return this;
    }

    public EcsServiceSetupBuilder withStateType(String stateType) {
      this.stateType = stateType;
      return this;
    }

    public EcsServiceSetupBuilder withRollback(boolean rollback) {
      this.rollback = rollback;
      return this;
    }

    public EcsServiceSetupBuilder withServiceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public EcsServiceSetupBuilder withUseLoadBalancer(boolean useLoadBalancer) {
      this.useLoadBalancer = useLoadBalancer;
      return this;
    }

    public EcsServiceSetupBuilder withLoadBalancerName(String loadBalancerName) {
      this.loadBalancerName = loadBalancerName;
      return this;
    }

    public EcsServiceSetupBuilder withTargetGroupArn(String targetGroupArn) {
      this.targetGroupArn = targetGroupArn;
      return this;
    }

    public EcsServiceSetupBuilder withRoleArn(String roleArn) {
      this.roleArn = roleArn;
      return this;
    }

    public EcsServiceSetupBuilder but() {
      return anEcsServiceSetup()
          .withId(id)
          .withName(name)
          .withMaxInstances(maxInstances)
          .withRequiredContextElementType(requiredContextElementType)
          .withStateType(stateType)
          .withRollback(rollback)
          .withServiceName(serviceName)
          .withUseLoadBalancer(useLoadBalancer)
          .withLoadBalancerName(loadBalancerName)
          .withTargetGroupArn(targetGroupArn)
          .withRoleArn(roleArn)
          .withResizeStrategy(resizeStrategy);
    }

    public EcsServiceSetup build() {
      EcsServiceSetup ecsServiceSetup = new EcsServiceSetup(name);
      ecsServiceSetup.setMaxInstances(maxInstances);
      ecsServiceSetup.setResizeStrategy(resizeStrategy);
      ecsServiceSetup.setId(id);
      ecsServiceSetup.setRequiredContextElementType(requiredContextElementType);
      ecsServiceSetup.setStateType(stateType);
      ecsServiceSetup.setRollback(rollback);
      ecsServiceSetup.setEcsServiceName(serviceName);
      ecsServiceSetup.setUseLoadBalancer(useLoadBalancer);
      ecsServiceSetup.setLoadBalancerName(loadBalancerName);
      ecsServiceSetup.setTargetGroupArn(targetGroupArn);
      ecsServiceSetup.setRoleArn(roleArn);
      return ecsServiceSetup;
    }
  }
}

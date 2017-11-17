package software.wings.sm.states;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static software.wings.api.ContainerServiceElement.ContainerServiceElementBuilder.aContainerServiceElement;
import static software.wings.api.EcsServiceExecutionData.Builder.anEcsServiceExecutionData;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;
import static software.wings.beans.container.ContainerTask.CONTAINER_NAME_PLACEHOLDER_REGEX;
import static software.wings.beans.container.ContainerTask.DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX;
import static software.wings.sm.StateType.ECS_SERVICE_SETUP;
import static software.wings.utils.EcsConvention.getRevisionFromServiceName;
import static software.wings.utils.EcsConvention.getServiceNamePrefixFromServiceName;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.amazonaws.services.ecs.model.ContainerDefinition;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.DeploymentConfiguration;
import com.amazonaws.services.ecs.model.KeyValuePair;
import com.amazonaws.services.ecs.model.LoadBalancer;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.TaskDefinition;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.api.ContainerServiceElement;
import software.wings.api.DeploymentType;
import software.wings.api.EcsServiceExecutionData;
import software.wings.api.PhaseElement;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.EcsContainerTask;
import software.wings.beans.container.EcsContainerTask.PortMapping;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateExecutionData;
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
public class EcsServiceSetup extends ContainerServiceSetup {
  @Transient private static final Logger logger = LoggerFactory.getLogger(EcsServiceSetup.class);

  // *** Note: UI Schema specified in wingsui/src/containers/WorkflowEditor/custom/ECSLoadBalancerModal.js

  private String ecsServiceName;
  private int serviceSteadyStateTimeout; // Minutes
  private boolean useLoadBalancer;
  private String loadBalancerName;
  private String targetGroupArn;
  private String roleArn;
  @Inject @Transient private transient AwsClusterService awsClusterService;

  /**
   * Instantiates a new state.
   *
   * @param name the name
   */
  public EcsServiceSetup(String name) {
    super(name, ECS_SERVICE_SETUP.name());
  }

  @Override
  protected StateExecutionData createService(ExecutionContext context, String serviceName, ImageDetails imageDetails,
      String appName, String envName, String clusterName, ContainerInfrastructureMapping infrastructureMapping,
      ContainerTask containerTask) {
    logger.info("Create ECS service in cluster {}", clusterName);
    SettingAttribute settingAttribute = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    EcsInfrastructureMapping ecsInfrastructureMapping = (EcsInfrastructureMapping) infrastructureMapping;
    String region = ecsInfrastructureMapping.getRegion();

    String dockerImageName = imageDetails.name + ":" + imageDetails.tag;
    String containerName = EcsConvention.getContainerName(dockerImageName);
    String taskFamily = isNotEmpty(ecsServiceName) ? Misc.normalizeExpression(context.renderExpression(ecsServiceName))
                                                   : EcsConvention.getTaskFamily(appName, serviceName, envName);

    EcsContainerTask ecsContainerTask = (EcsContainerTask) containerTask;
    if (ecsContainerTask == null) {
      ecsContainerTask = new EcsContainerTask();
      EcsContainerTask.ContainerDefinition containerDefinition = new EcsContainerTask.ContainerDefinition();
      containerDefinition.setMemory(256);
      containerDefinition.setPortMappings(emptyList());
      ecsContainerTask.setContainerDefinitions(Lists.newArrayList(containerDefinition));
    }

    TaskDefinition taskDefinition = createTaskDefinition(ecsContainerTask, containerName, dockerImageName, taskFamily,
        region, settingAttribute, context.getServiceVariables(), context.getAppId(), context.getWorkflowExecutionId());

    String containerServiceName = EcsConvention.getServiceName(taskFamily, taskDefinition.getRevision());

    CreateServiceRequest createServiceRequest =
        new CreateServiceRequest()
            .withServiceName(containerServiceName)
            .withCluster(clusterName)
            .withDesiredCount(0)
            .withDeploymentConfiguration(
                new DeploymentConfiguration().withMaximumPercent(200).withMinimumHealthyPercent(100))
            .withTaskDefinition(taskDefinition.getFamily() + ":" + taskDefinition.getRevision());

    int portToExpose =
        ecsContainerTask.getContainerDefinitions()
            .stream()
            .flatMap(containerDefinition
                -> Optional.ofNullable(containerDefinition.getPortMappings()).orElse(emptyList()).stream())
            .filter(PortMapping::isLoadBalancerPort)
            .findFirst()
            .map(PortMapping::getContainerPort)
            .orElse(0);

    boolean exposePort = useLoadBalancer && portToExpose != 0;

    if (exposePort) {
      createServiceRequest
          .withLoadBalancers(new LoadBalancer()
                                 .withContainerName(containerName)
                                 .withContainerPort(portToExpose)
                                 .withTargetGroupArn(targetGroupArn))
          .withRole(roleArn);
    }

    logger.info("Creating ECS service {} in cluster {}", containerServiceName, clusterName);
    awsClusterService.createService(region, settingAttribute,
        secretManager.getEncryptionDetails(
            (Encryptable) settingAttribute.getValue(), context.getAppId(), context.getWorkflowExecutionId()),
        createServiceRequest);

    logger.info("Cleaning up old versions");
    cleanup(settingAttribute, region, containerServiceName, clusterName, context.getAppId(),
        context.getWorkflowExecutionId());

    return buildExecutionData(clusterName, dockerImageName, exposePort, containerServiceName);
  }

  @Override
  protected ContainerServiceElement buildContainerServiceElement(PhaseElement phaseElement, String serviceId,
      String appId, String workflowExecutionId, ContainerInfrastructureMapping infrastructureMapping,
      String containerServiceName) {
    return aContainerServiceElement()
        .withUuid(serviceId)
        .withName(containerServiceName)
        .withMaxInstances(getMaxInstances() == 0 ? 10 : getMaxInstances())
        .withResizeStrategy(getResizeStrategy() == null ? RESIZE_NEW_FIRST : getResizeStrategy())
        .withServiceSteadyStateTimeout(serviceSteadyStateTimeout)
        .withClusterName(infrastructureMapping.getClusterName())
        .withDeploymentType(DeploymentType.ECS)
        .withInfraMappingId(phaseElement.getInfraMappingId())
        .build();
  }

  private StateExecutionData buildExecutionData(
      String clusterName, String dockerImageName, boolean exposePort, String containerServiceName) {
    EcsServiceExecutionData.Builder ecsServiceExecutionDataBuilder = anEcsServiceExecutionData()
                                                                         .withEcsClusterName(clusterName)
                                                                         .withEcsServiceName(containerServiceName)
                                                                         .withDockerImageName(dockerImageName);
    if (exposePort) {
      ecsServiceExecutionDataBuilder.withLoadBalancerName(loadBalancerName)
          .withRoleArn(roleArn)
          .withTargetGroupArn(targetGroupArn);
    }
    return ecsServiceExecutionDataBuilder.build();
  }

  @Override
  protected boolean isValidInfraMapping(InfrastructureMapping infrastructureMapping) {
    return infrastructureMapping instanceof EcsInfrastructureMapping;
  }

  @Override
  protected String getDeploymentType() {
    return DeploymentType.ECS.name();
  }

  @Override
  protected String getContainerServiceNameFromExecutionData(StateExecutionData executionData) {
    return ((EcsServiceExecutionData) executionData).getEcsServiceName();
  }

  private TaskDefinition createTaskDefinition(EcsContainerTask ecsContainerTask, String containerName,
      String dockerImageName, String taskFamily, String region, SettingAttribute settingAttribute,
      Map<String, String> serviceVariables, String appId, String workflowExecutionId) {
    String configTemplate;
    if (StringUtils.isNotEmpty(ecsContainerTask.getAdvancedConfig())) {
      configTemplate = ecsContainerTask.fetchAdvancedConfigNoComments();
    } else {
      configTemplate = ecsContainerTask.fetchJsonConfig();
    }

    String config = configTemplate.replaceAll(DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX, dockerImageName)
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

    logger.info("Creating task definition {} with container image {}", taskFamily, dockerImageName);
    return awsClusterService.createTask(region, settingAttribute,
        secretManager.getEncryptionDetails((Encryptable) settingAttribute.getValue(), appId, workflowExecutionId),
        registerTaskDefinitionRequest);
  }

  private void cleanup(SettingAttribute settingAttribute, String region, String containerServiceName,
      String clusterName, String appId, String workflowExecutionId) {
    int revision = getRevisionFromServiceName(containerServiceName);
    if (revision > KEEP_N_REVISIONS) {
      int minRevisionToKeep = revision - KEEP_N_REVISIONS;
      String serviceNamePrefix = getServiceNamePrefixFromServiceName(containerServiceName);
      awsClusterService
          .getServices(region, settingAttribute,
              secretManager.getEncryptionDetails((Encryptable) settingAttribute.getValue(), appId, workflowExecutionId),
              clusterName)
          .stream()
          .filter(s -> s.getServiceName().startsWith(serviceNamePrefix) && s.getDesiredCount() == 0)
          .collect(Collectors.toList())
          .forEach(s -> {
            String oldServiceName = s.getServiceName();
            if (getRevisionFromServiceName(oldServiceName) < minRevisionToKeep) {
              logger.info("Deleting old version: " + oldServiceName);
              awsClusterService.deleteService(region, settingAttribute,
                  secretManager.getEncryptionDetails(
                      (Encryptable) settingAttribute.getValue(), appId, workflowExecutionId),
                  clusterName, oldServiceName);
            }
          });
    }
  }

  protected String getClusterNameFromContextElement(ExecutionContext context) {
    return super.getClusterNameFromContextElement(context).split("/")[1];
  }

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

  public long getServiceSteadyStateTimeout() {
    return serviceSteadyStateTimeout;
  }

  public void setServiceSteadyStateTimeout(int serviceSteadyStateTimeout) {
    this.serviceSteadyStateTimeout = serviceSteadyStateTimeout;
  }
}

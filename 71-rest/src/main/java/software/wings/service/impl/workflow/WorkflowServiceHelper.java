package software.wings.service.impl.workflow;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.ListUtils.trimList;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.EntityType.INFRASTRUCTURE_MAPPING;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;
import static software.wings.beans.InfrastructureMappingType.AWS_SSH;
import static software.wings.beans.InfrastructureMappingType.PCF_PCF;
import static software.wings.beans.InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH;
import static software.wings.beans.OrchestrationWorkflowType.BASIC;
import static software.wings.beans.OrchestrationWorkflowType.BLUE_GREEN;
import static software.wings.beans.OrchestrationWorkflowType.CANARY;
import static software.wings.beans.OrchestrationWorkflowType.MULTI_SERVICE;
import static software.wings.beans.OrchestrationWorkflowType.ROLLING;
import static software.wings.beans.PhaseStep.PhaseStepBuilder;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.AMI_AUTOSCALING_GROUP_SETUP;
import static software.wings.beans.PhaseStepType.AMI_DEPLOY_AUTOSCALING_GROUP;
import static software.wings.beans.PhaseStepType.AMI_SWITCH_AUTOSCALING_GROUP_ROUTES;
import static software.wings.beans.PhaseStepType.CLUSTER_SETUP;
import static software.wings.beans.PhaseStepType.CONTAINER_DEPLOY;
import static software.wings.beans.PhaseStepType.CONTAINER_SETUP;
import static software.wings.beans.PhaseStepType.DEPLOY_AWSCODEDEPLOY;
import static software.wings.beans.PhaseStepType.DEPLOY_AWS_LAMBDA;
import static software.wings.beans.PhaseStepType.DEPLOY_SERVICE;
import static software.wings.beans.PhaseStepType.DISABLE_SERVICE;
import static software.wings.beans.PhaseStepType.ENABLE_SERVICE;
import static software.wings.beans.PhaseStepType.INFRASTRUCTURE_NODE;
import static software.wings.beans.PhaseStepType.PCF_RESIZE;
import static software.wings.beans.PhaseStepType.PCF_SETUP;
import static software.wings.beans.PhaseStepType.PCF_SWICH_ROUTES;
import static software.wings.beans.PhaseStepType.PREPARE_STEPS;
import static software.wings.beans.PhaseStepType.ROUTE_UPDATE;
import static software.wings.beans.PhaseStepType.STOP_SERVICE;
import static software.wings.beans.PhaseStepType.VERIFY_SERVICE;
import static software.wings.beans.PhaseStepType.WRAP_UP;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.common.Constants.PRIMARY_SERVICE_NAME_EXPRESSION;
import static software.wings.common.Constants.ROLLBACK_AUTOSCALING_GROUP_ROUTE;
import static software.wings.common.Constants.ROLLBACK_SERVICE;
import static software.wings.common.Constants.STAGE_SERVICE_NAME_EXPRESSION;
import static software.wings.common.Constants.SWAP_AUTOSCALING_GROUP_ROUTE;
import static software.wings.common.Constants.UPGRADE_AUTOSCALING_GROUP_ROUTE;
import static software.wings.common.Constants.VERIFY_STAGING;
import static software.wings.settings.SettingValue.SettingVariableTypes.PHYSICAL_DATA_CENTER;
import static software.wings.sm.StateType.ARTIFACT_CHECK;
import static software.wings.sm.StateType.AWS_AMI_ROLLBACK_SWITCH_ROUTES;
import static software.wings.sm.StateType.AWS_AMI_SERVICE_DEPLOY;
import static software.wings.sm.StateType.AWS_AMI_SERVICE_ROLLBACK;
import static software.wings.sm.StateType.AWS_AMI_SERVICE_SETUP;
import static software.wings.sm.StateType.AWS_AMI_SWITCH_ROUTES;
import static software.wings.sm.StateType.AWS_CODEDEPLOY_ROLLBACK;
import static software.wings.sm.StateType.AWS_CODEDEPLOY_STATE;
import static software.wings.sm.StateType.AWS_LAMBDA_ROLLBACK;
import static software.wings.sm.StateType.AWS_LAMBDA_STATE;
import static software.wings.sm.StateType.AWS_NODE_SELECT;
import static software.wings.sm.StateType.COMMAND;
import static software.wings.sm.StateType.DC_NODE_SELECT;
import static software.wings.sm.StateType.ECS_SERVICE_DEPLOY;
import static software.wings.sm.StateType.ECS_SERVICE_ROLLBACK;
import static software.wings.sm.StateType.ECS_SERVICE_SETUP;
import static software.wings.sm.StateType.ELASTIC_LOAD_BALANCER;
import static software.wings.sm.StateType.GCP_CLUSTER_SETUP;
import static software.wings.sm.StateType.HELM_DEPLOY;
import static software.wings.sm.StateType.HELM_ROLLBACK;
import static software.wings.sm.StateType.KUBERNETES_DEPLOY;
import static software.wings.sm.StateType.KUBERNETES_DEPLOY_ROLLBACK;
import static software.wings.sm.StateType.KUBERNETES_SETUP;
import static software.wings.sm.StateType.KUBERNETES_SETUP_ROLLBACK;
import static software.wings.sm.StateType.KUBERNETES_SWAP_SERVICE_SELECTORS;
import static software.wings.sm.StateType.PCF_BG_MAP_ROUTE;
import static software.wings.sm.StateType.PCF_ROLLBACK;
import static software.wings.sm.StateType.ROLLING_NODE_SELECT;
import static software.wings.sm.states.ElasticLoadBalancerState.Operation.Disable;
import static software.wings.sm.states.ElasticLoadBalancerState.Operation.Enable;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscaler;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.DeploymentType;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.GraphNode;
import software.wings.beans.GraphNode.GraphNodeBuilder;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.OrchestrationWorkflowType;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.PhysicalInfrastructureMappingBase;
import software.wings.beans.Service;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.ServiceCommand;
import software.wings.common.Constants;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.sm.states.AwsCodeDeployState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

@Singleton
public class WorkflowServiceHelper {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceHelper.class);

  private static final String MIN_REPLICAS = "\\$\\{MIN_REPLICAS}";
  private static final String MAX_REPLICAS = "\\$\\{MAX_REPLICAS}";
  private static final String UTILIZATION = "\\$\\{UTILIZATION}";
  // yaml template for custom metric HPA for cpu utilization threshold
  private static final String yamlForHPAWithCustomMetric = "apiVersion: autoscaling/v2beta1\n"
      + "kind: HorizontalPodAutoscaler\n"
      + "metadata:\n"
      + "  name: hpa-name\n"
      + "spec:\n"
      + "  scaleTargetRef:\n"
      + "    apiVersion: extensions/v1beta1\n"
      + "    kind: Deployment\n"
      + "    name: target-name\n"
      + "  minReplicas: ${MIN_REPLICAS}\n"
      + "  maxReplicas: ${MAX_REPLICAS}\n"
      + "  metrics:\n"
      + "  - type: Resource\n"
      + "    resource:\n"
      + "      name: cpu\n"
      + "      targetAverageUtilization: ${UTILIZATION}\n";

  @Inject private EnvironmentService environmentService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private ArtifactStreamService artifactStreamService;

  public String getHPAYamlStringWithCustomMetric(
      Integer minAutoscaleInstances, Integer maxAutoscaleInstances, Integer targetCpuUtilizationPercentage) {
    try {
      String hpaYaml =
          yamlForHPAWithCustomMetric.replaceAll(MIN_REPLICAS, String.valueOf(minAutoscaleInstances.intValue()))
              .replaceAll(MAX_REPLICAS, String.valueOf(maxAutoscaleInstances.intValue()))
              .replaceAll(UTILIZATION, String.valueOf(targetCpuUtilizationPercentage.intValue()));
      if (KubernetesHelper.loadYaml(hpaYaml, HorizontalPodAutoscaler.class) == null) {
        logger.error("HPA couldn't be parsed: {}", hpaYaml);
      }
      return hpaYaml;
    } catch (IOException e) {
      throw new WingsException("Unable to generate Yaml String for Horizontal pod autoscalar");
    }
  }

  public boolean workflowHasSshDeploymentPhase(CanaryOrchestrationWorkflow canaryOrchestrationWorkflow) {
    List<WorkflowPhase> workflowPhases = canaryOrchestrationWorkflow.getWorkflowPhases();
    if (isNotEmpty(workflowPhases)) {
      return workflowPhases.stream().anyMatch(
          workflowPhase -> DeploymentType.SSH.equals(workflowPhase.getDeploymentType()));
    }
    return false;
  }

  public boolean needArtifactCheckStep(String appId, CanaryOrchestrationWorkflow canaryOrchestrationWorkflow) {
    List<WorkflowPhase> workflowPhases = canaryOrchestrationWorkflow.getWorkflowPhases();
    if (isNotEmpty(canaryOrchestrationWorkflow.getWorkflowPhases())) {
      List<String> infraMappingIds = workflowPhases.stream()
                                         .filter(workflowPhase -> workflowPhase.getInfraMappingId() != null)
                                         .map(WorkflowPhase::getInfraMappingId)
                                         .collect(toList());
      return infrastructureMappingService.getInfraStructureMappingsByUuids(appId, infraMappingIds)
          .stream()
          .anyMatch((InfrastructureMapping infra)
                        -> AWS_SSH.name().equals(infra.getInfraMappingType())
                  || PHYSICAL_DATA_CENTER_SSH.name().equals(infra.getInfraMappingType())
                  || PCF_PCF.name().equals(infra.getInfraMappingType()));
    }
    return false;
  }

  public boolean ensureArtifactCheckInPreDeployment(CanaryOrchestrationWorkflow canaryOrchestrationWorkflow) {
    PhaseStep preDeploymentSteps = canaryOrchestrationWorkflow.getPreDeploymentSteps();
    if (preDeploymentSteps == null) {
      preDeploymentSteps = new PhaseStep();
      canaryOrchestrationWorkflow.setPreDeploymentSteps(preDeploymentSteps);
    }
    if (preDeploymentSteps.getSteps() == null) {
      preDeploymentSteps.setSteps(new ArrayList<>());
    }
    boolean artifactCheckFound =
        preDeploymentSteps.getSteps().stream().anyMatch(graphNode -> ARTIFACT_CHECK.name().equals(graphNode.getType()));
    if (artifactCheckFound) {
      return false;
    } else {
      preDeploymentSteps.getSteps().add(
          aGraphNode().withType(ARTIFACT_CHECK.name()).withName("Artifact Check").build());
      return true;
    }
  }

  public String resolveEnvironmentId(Workflow workflow, Map<String, String> workflowVariables) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    if (!workflow.checkEnvironmentTemplatized()) {
      return workflow.getEnvId();
    } else {
      if (isNotEmpty(workflowVariables)) {
        String envName =
            WorkflowServiceTemplateHelper.getTemplatizedEnvVariableName(orchestrationWorkflow.getUserVariables());
        if (envName != null) {
          if (workflowVariables.get(envName) != null) {
            return workflowVariables.get(envName);
          }
        }
      }
    }
    throw new WingsException(
        "Workflow [" + workflow.getName() + "] environment parameterized. However, the value not supplied");
  }

  public List<String> getKeywords(Workflow workflow) {
    List<Object> keywords = workflow.generateKeywords();
    if (workflow.getEnvId() != null) {
      Environment environment = environmentService.get(workflow.getAppId(), workflow.getEnvId());
      if (environment != null) {
        keywords.add(environment.getName());
      }
    }
    return trimList(keywords);
  }

  public void setKeywords(Workflow workflow) {
    workflow.setDefaultVersion(1);
    workflow.setKeywords(trimList(
        newArrayList(workflow.getName(), workflow.getDescription(), workflow.getWorkflowType(), workflow.getNotes())));
  }

  /**
   * Validates whether service id and mapped service are of same type
   */
  public void validateServiceMapping(String appId, String targetAppId, Map<String, String> serviceMapping) {
    if (serviceMapping == null) {
      throw new InvalidRequestException("At least one service mapping required to clone across applications", USER);
    }
    for (Map.Entry<String, String> service : serviceMapping.entrySet()) {
      if (service.getKey() == null || service.getValue() == null) {
        continue;
      }
      Service oldService = serviceResourceService.get(appId, service.getKey(), false);
      notNullCheck("Source service does not exist", oldService, USER);
      Service newService = serviceResourceService.get(targetAppId, service.getValue(), false);
      notNullCheck("Target service does not exist", newService, USER);
      if (oldService.getArtifactType() != null && !oldService.getArtifactType().equals(newService.getArtifactType())) {
        throw new InvalidRequestException("Target service  [" + oldService.getName()
                + " ] is not compatible with service [" + newService.getName() + "]",
            USER);
      }
    }
  }

  public void validateServiceandInframapping(String appId, String serviceId, String inframappingId) {
    // Validate if service Id is valid or not
    if (serviceId == null || inframappingId == null) {
      return;
    }
    Service service = serviceResourceService.get(appId, serviceId, false);
    if (service == null) {
      throw new InvalidRequestException("Service [" + serviceId + "] does not exist", USER);
    }
    InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, inframappingId);
    if (infrastructureMapping == null) {
      throw new InvalidRequestException("Service Infrastructure [" + inframappingId + "] does not exist", USER);
    }
    if (!service.getUuid().equals(infrastructureMapping.getServiceId())) {
      throw new InvalidRequestException("Service Infrastructure [" + infrastructureMapping.getName()
              + "] not mapped to Service [" + service.getName() + "]",
          USER);
    }
  }

  public void setCloudProvider(String appId, WorkflowPhase workflowPhase) {
    if (workflowPhase.checkInfraTemplatized()) {
      return;
    }
    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
    if (infrastructureMapping == null) {
      logger.warn(
          "Service Infrastructure with id {}  for appId {} does not exist", workflowPhase.getInfraMappingId(), appId);
      throw new InvalidRequestException("ServiceInfrastructure does not exist", USER);
    }
    workflowPhase.setComputeProviderId(infrastructureMapping.getComputeProviderSettingId());
    workflowPhase.setInfraMappingName(infrastructureMapping.getName());
    workflowPhase.setDeploymentType(DeploymentType.valueOf(infrastructureMapping.getDeploymentType()));
  }

  public void generateNewWorkflowPhaseStepsForAWSAmiBlueGreen(
      String appId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    if (serviceSetupRequired) {
      InfrastructureMapping infraMapping = infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
      if (infraMapping instanceof AwsAmiInfrastructureMapping) {
        Map<String, Object> defaultData = newHashMap();
        defaultData.put("maxInstances", 10);
        defaultData.put("autoScalingSteadyStateTimeout", 10);
        defaultData.put("blueGreen", true);
        workflowPhase.addPhaseStep(aPhaseStep(AMI_AUTOSCALING_GROUP_SETUP, Constants.SETUP_AUTOSCALING_GROUP)
                                       .addStep(aGraphNode()
                                                    .withId(generateUuid())
                                                    .withType(AWS_AMI_SERVICE_SETUP.name())
                                                    .withName("AWS AutoScaling Group Setup")
                                                    .withProperties(defaultData)
                                                    .build())
                                       .build());
      }
    }
    workflowPhase.addPhaseStep(aPhaseStep(AMI_DEPLOY_AUTOSCALING_GROUP, Constants.DEPLOY_SERVICE)
                                   .addStep(aGraphNode()
                                                .withId(generateUuid())
                                                .withType(AWS_AMI_SERVICE_DEPLOY.name())
                                                .withName(Constants.UPGRADE_AUTOSCALING_GROUP)
                                                .build())
                                   .build());

    workflowPhase.addPhaseStep(
        aPhaseStep(VERIFY_SERVICE, VERIFY_STAGING).addAllSteps(commandNodes(commandMap, CommandType.VERIFY)).build());

    Map<String, Object> defaultDataSwitchRoutes = newHashMap();
    defaultDataSwitchRoutes.put("downsizeOldAsg", true);
    workflowPhase.addPhaseStep(aPhaseStep(AMI_SWITCH_AUTOSCALING_GROUP_ROUTES, SWAP_AUTOSCALING_GROUP_ROUTE)
                                   .addStep(aGraphNode()
                                                .withId(generateUuid())
                                                .withType(AWS_AMI_SWITCH_ROUTES.name())
                                                .withName(UPGRADE_AUTOSCALING_GROUP_ROUTE)
                                                .withProperties(defaultDataSwitchRoutes)
                                                .build())
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  public void generateNewWorkflowPhaseStepsForAWSAmi(
      String appId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    if (serviceSetupRequired) {
      InfrastructureMapping infraMapping = infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
      if (infraMapping instanceof AwsAmiInfrastructureMapping) {
        Map<String, Object> defaultData = new HashMap<>();
        defaultData.put("maxInstances", 10);
        defaultData.put("autoScalingSteadyStateTimeout", 10);
        defaultData.put("blueGreen", false);
        workflowPhase.addPhaseStep(aPhaseStep(AMI_AUTOSCALING_GROUP_SETUP, Constants.SETUP_AUTOSCALING_GROUP)
                                       .addStep(aGraphNode()
                                                    .withId(generateUuid())
                                                    .withType(AWS_AMI_SERVICE_SETUP.name())
                                                    .withName("AWS AutoScaling Group Setup")
                                                    .withProperties(defaultData)
                                                    .build())
                                       .build());
      }
    }
    workflowPhase.addPhaseStep(aPhaseStep(AMI_DEPLOY_AUTOSCALING_GROUP, Constants.DEPLOY_SERVICE)
                                   .addStep(aGraphNode()
                                                .withId(generateUuid())
                                                .withType(AWS_AMI_SERVICE_DEPLOY.name())
                                                .withName(Constants.UPGRADE_AUTOSCALING_GROUP)
                                                .build())
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  public void generateNewWorkflowPhaseStepsForAWSLambda(String appId, String envId, WorkflowPhase workflowPhase) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    workflowPhase.addPhaseStep(aPhaseStep(PREPARE_STEPS, Constants.PREPARE_STEPS).build());

    workflowPhase.addPhaseStep(aPhaseStep(DEPLOY_AWS_LAMBDA, Constants.DEPLOY_SERVICE)
                                   .addStep(aGraphNode()
                                                .withId(generateUuid())
                                                .withType(AWS_LAMBDA_STATE.name())
                                                .withName(Constants.AWS_LAMBDA)
                                                .build())
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  public void generateNewWorkflowPhaseStepsForAWSCodeDeploy(String appId, WorkflowPhase workflowPhase) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    workflowPhase.addPhaseStep(aPhaseStep(PREPARE_STEPS, Constants.PREPARE_STEPS).build());

    Map<String, String> stateDefaults = getStateDefaults(appId, service.getUuid(), AWS_CODEDEPLOY_STATE);
    GraphNodeBuilder node =
        aGraphNode().withId(generateUuid()).withType(AWS_CODEDEPLOY_STATE.name()).withName(Constants.AWS_CODE_DEPLOY);
    if (isNotEmpty(stateDefaults)) {
      if (isNotBlank(stateDefaults.get("bucket"))) {
        node.addProperty("bucket", stateDefaults.get("bucket"));
      }
      if (isNotBlank(stateDefaults.get("key"))) {
        node.addProperty("key", stateDefaults.get("key"));
      }
      if (isNotBlank(stateDefaults.get("bundleType"))) {
        node.addProperty("bundleType", stateDefaults.get("bundleType"));
      }
    }
    workflowPhase.addPhaseStep(
        aPhaseStep(DEPLOY_AWSCODEDEPLOY, Constants.DEPLOY_SERVICE).addStep(node.build()).build());

    workflowPhase.addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  public Map<String, String> getStateDefaults(String appId, String serviceId, StateType stateType) {
    switch (stateType) {
      case AWS_CODEDEPLOY_STATE: {
        List<ArtifactStream> artifactStreams = artifactStreamService.fetchArtifactStreamsForService(appId, serviceId);
        if (artifactStreams.stream().anyMatch(
                artifactStream -> ArtifactStreamType.AMAZON_S3.name().equals(artifactStream.getArtifactStreamType()))) {
          return AwsCodeDeployState.loadDefaults();
        }
        break;
      }
      default:
        unhandled(stateType);
    }
    return Collections.emptyMap();
  }

  public void generateNewWorkflowPhaseStepsForECS(
      String appId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    if (serviceSetupRequired) {
      workflowPhase.addPhaseStep(aPhaseStep(CONTAINER_SETUP, Constants.SETUP_CONTAINER)
                                     .addStep(aGraphNode()
                                                  .withId(generateUuid())
                                                  .withType(ECS_SERVICE_SETUP.name())
                                                  .withName(Constants.ECS_SERVICE_SETUP)
                                                  .build())
                                     .build());
    }
    workflowPhase.addPhaseStep(aPhaseStep(CONTAINER_DEPLOY, Constants.DEPLOY_CONTAINERS)
                                   .addStep(aGraphNode()
                                                .withId(generateUuid())
                                                .withType(ECS_SERVICE_DEPLOY.name())
                                                .withName(Constants.UPGRADE_CONTAINERS)
                                                .build())
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForPCFBlueGreen(
      WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    if (workflowPhase.isDaemonSet() || workflowPhase.isStatefulSet()) {
      throw new InvalidRequestException("DaemonSet and StatefulSet are not supported with Blue/Green Deployment", USER);
    }

    Map<String, Object> defaultRouteUpdateProperties = new HashMap<>();
    defaultRouteUpdateProperties.put("service1", PRIMARY_SERVICE_NAME_EXPRESSION);
    defaultRouteUpdateProperties.put("service2", STAGE_SERVICE_NAME_EXPRESSION);

    WorkflowPhaseBuilder workflowPhaseBuilder =
        aWorkflowPhase()
            .withName(Constants.ROLLBACK_PREFIX + workflowPhase.getName())
            .withRollback(true)
            .withServiceId(workflowPhase.getServiceId())
            .withComputeProviderId(workflowPhase.getComputeProviderId())
            .withInfraMappingName(workflowPhase.getInfraMappingName())
            .withPhaseNameForRollback(workflowPhase.getName())
            .withDeploymentType(workflowPhase.getDeploymentType())
            .withInfraMappingId(workflowPhase.getInfraMappingId())
            .addPhaseStep(aPhaseStep(PCF_SWICH_ROUTES, Constants.PCF_BG_MAP_ROUTE)
                              .addStep(aGraphNode()
                                           .withId(generateUuid())
                                           .withType(PCF_BG_MAP_ROUTE.name())
                                           .withName(Constants.PCF_BG_SWAP_ROUTE)
                                           .withProperties(defaultRouteUpdateProperties)
                                           .withRollback(true)
                                           .build())
                              .withPhaseStepNameForRollback(Constants.PCF_BG_MAP_ROUTE)
                              .withStatusForRollback(ExecutionStatus.SUCCESS)
                              .withRollback(true)
                              .build())
            .addPhaseStep(aPhaseStep(PhaseStepType.PCF_RESIZE, Constants.DEPLOY)
                              .addStep(aGraphNode()
                                           .withId(generateUuid())
                                           .withType(PCF_ROLLBACK.name())
                                           .withName(Constants.PCF_ROLLBACK)
                                           .withRollback(true)
                                           .build())
                              .withPhaseStepNameForRollback(Constants.DEPLOY)
                              .withStatusForRollback(ExecutionStatus.SUCCESS)
                              .withRollback(true)
                              .build());

    // When we rolling back the verification steps the same criterie to run if deployment is needed should be used
    workflowPhaseBuilder
        .addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                          .withPhaseStepNameForRollback(Constants.PCF_RESIZE)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        .addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build());
    return workflowPhaseBuilder.build();
  }

  public void generateNewWorkflowPhaseStepsForPCFBlueGreen(
      String appId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    if (workflowPhase.isDaemonSet() || workflowPhase.isStatefulSet()) {
      throw new InvalidRequestException("DaemonSet and StatefulSet are not supported with Blue/Green Deployment", USER);
    }

    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    // SETUP
    if (serviceSetupRequired) {
      Map<String, Object> defaultSetupProperties = new HashMap<>();
      defaultSetupProperties.put("blueGreen", true);
      defaultSetupProperties.put("resizeStrategy", "RESIZE_NEW_FIRST");
      defaultSetupProperties.put("route", "${" + Constants.INFRA_TEMP_ROUTE_PCF + "}");

      workflowPhase.addPhaseStep(aPhaseStep(PCF_SETUP, Constants.SETUP)
                                     .addStep(aGraphNode()
                                                  .withId(generateUuid())
                                                  .withType(PCF_SETUP.name())
                                                  .withName(Constants.PCF_SETUP)
                                                  .withProperties(defaultSetupProperties)
                                                  .build())
                                     .build());
    }

    // RESIZE
    Map<String, Object> defaultUpgradeStageContainerProperties = new HashMap<>();
    defaultUpgradeStageContainerProperties.put("instanceUnitType", "PERCENTAGE");
    defaultUpgradeStageContainerProperties.put("instanceCount", 100);
    defaultUpgradeStageContainerProperties.put("downsizeInstanceUnitType", "PERCENTAGE");
    defaultUpgradeStageContainerProperties.put("downsizeInstanceCount", 100);

    workflowPhase.addPhaseStep(aPhaseStep(PCF_RESIZE, Constants.DEPLOY)
                                   .addStep(aGraphNode()
                                                .withId(generateUuid())
                                                .withType(PCF_RESIZE.name())
                                                .withName(Constants.PCF_RESIZE)
                                                .withProperties(defaultUpgradeStageContainerProperties)
                                                .build())
                                   .build());

    // Verify
    workflowPhase.addPhaseStep(
        aPhaseStep(VERIFY_SERVICE, VERIFY_STAGING).addAllSteps(commandNodes(commandMap, CommandType.VERIFY)).build());

    // Swap Routes
    Map<String, Object> defaultRouteUpdateProperties = new HashMap<>();
    defaultRouteUpdateProperties.put("downsizeOldApps", false);
    workflowPhase.addPhaseStep(aPhaseStep(PCF_SWICH_ROUTES, Constants.PCF_BG_MAP_ROUTE)
                                   .addStep(aGraphNode()
                                                .withId(generateUuid())
                                                .withType(PCF_BG_MAP_ROUTE.name())
                                                .withName(Constants.PCF_BG_SWAP_ROUTE)
                                                .withProperties(defaultRouteUpdateProperties)
                                                .build())
                                   .build());

    // Wrap up
    workflowPhase.addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  public void generateNewWorkflowPhaseStepsForPCF(String appId, String envId, WorkflowPhase workflowPhase,
      boolean serviceSetupRequired, OrchestrationWorkflowType orchestrationWorkflowType) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    if (serviceSetupRequired) {
      Map<String, Object> defaultProperties = new HashMap<>();
      defaultProperties.put("blueGreen", false);
      defaultProperties.put("resizeStrategy", "DOWNSIZE_OLD_FIRST");
      defaultProperties.put("route", "${" + Constants.INFRA_ROUTE_PCF + "}");

      workflowPhase.addPhaseStep(aPhaseStep(PCF_SETUP, Constants.SETUP)
                                     .addStep(aGraphNode()
                                                  .withId(generateUuid())
                                                  .withType(PCF_SETUP.name())
                                                  .withName(Constants.PCF_SETUP)
                                                  .withProperties(defaultProperties)
                                                  .build())
                                     .build());
    }

    workflowPhase.addPhaseStep(
        aPhaseStep(PCF_RESIZE, Constants.DEPLOY)
            .addStep(
                aGraphNode().withId(generateUuid()).withType(PCF_RESIZE.name()).withName(Constants.PCF_RESIZE).build())
            .build());

    workflowPhase.addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  public void generateNewWorkflowPhaseStepsForHelm(
      String appId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.HELM_DEPLOY, Constants.DEPLOY_CONTAINERS)
                                   .addStep(aGraphNode()
                                                .withId(generateUuid())
                                                .withType(HELM_DEPLOY.name())
                                                .withName(Constants.HELM_DEPLOY)
                                                .build())
                                   .build());
    workflowPhase.addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                                   .build());
    workflowPhase.addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  public void generateNewWorkflowPhaseStepsForKubernetes(
      String appId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    if (serviceSetupRequired) {
      InfrastructureMapping infraMapping = infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
      if (infraMapping instanceof GcpKubernetesInfrastructureMapping
          && Constants.RUNTIME.equals(((GcpKubernetesInfrastructureMapping) infraMapping).getClusterName())) {
        workflowPhase.addPhaseStep(aPhaseStep(CLUSTER_SETUP, Constants.SETUP_CLUSTER)
                                       .addStep(aGraphNode()
                                                    .withId(generateUuid())
                                                    .withType(GCP_CLUSTER_SETUP.name())
                                                    .withName("GCP Cluster Setup")
                                                    .build())
                                       .build());
      }

      Map<String, Object> defaultSetupProperties = new HashMap<>();
      defaultSetupProperties.put("replicationControllerName", "${app.name}-${service.name}-${env.name}");
      defaultSetupProperties.put("resizeStrategy", "RESIZE_NEW_FIRST");
      workflowPhase.addPhaseStep(aPhaseStep(CONTAINER_SETUP, Constants.SETUP_CONTAINER)
                                     .addStep(aGraphNode()
                                                  .withId(generateUuid())
                                                  .withType(KUBERNETES_SETUP.name())
                                                  .withName(Constants.KUBERNETES_SERVICE_SETUP)
                                                  .withProperties(defaultSetupProperties)
                                                  .build())
                                     .build());
    }

    if (!workflowPhase.isDaemonSet() && !workflowPhase.isStatefulSet()) {
      workflowPhase.addPhaseStep(aPhaseStep(CONTAINER_DEPLOY, Constants.DEPLOY_CONTAINERS)
                                     .addStep(aGraphNode()
                                                  .withId(generateUuid())
                                                  .withType(KUBERNETES_DEPLOY.name())
                                                  .withName(Constants.UPGRADE_CONTAINERS)
                                                  .build())
                                     .build());
    }
    workflowPhase.addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                                   .build());
    workflowPhase.addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  public void generateNewWorkflowPhaseStepsForKubernetesBlueGreen(
      String appId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    if (workflowPhase.isDaemonSet() || workflowPhase.isStatefulSet()) {
      throw new InvalidRequestException("DaemonSet and StatefulSet are not supported with Blue/Green Deployment", USER);
    }

    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    if (serviceSetupRequired) {
      Map<String, Object> defaultServiceSpec = new HashMap<>();
      defaultServiceSpec.put("serviceType", "ClusterIP");
      defaultServiceSpec.put("port", 80);
      defaultServiceSpec.put("targetPort", 8080);
      defaultServiceSpec.put("protocol", "TCP");

      Map<String, Object> defaultBlueGreenConfig = new HashMap<>();
      defaultBlueGreenConfig.put("primaryService", defaultServiceSpec);
      defaultBlueGreenConfig.put("stageService", defaultServiceSpec);

      Map<String, Object> defaultSetupProperties = new HashMap<>();
      defaultSetupProperties.put("replicationControllerName", "${app.name}-${service.name}-${env.name}");
      defaultSetupProperties.put("blueGreen", true);
      defaultSetupProperties.put("blueGreenConfig", defaultBlueGreenConfig);
      defaultSetupProperties.put("resizeStrategy", "RESIZE_NEW_FIRST");

      workflowPhase.addPhaseStep(aPhaseStep(CONTAINER_SETUP, Constants.SETUP_CONTAINER)
                                     .addStep(aGraphNode()
                                                  .withId(generateUuid())
                                                  .withType(KUBERNETES_SETUP.name())
                                                  .withName(Constants.KUBERNETES_SERVICE_SETUP_BLUEGREEN)
                                                  .withProperties(defaultSetupProperties)
                                                  .build())
                                     .build());
    }

    Map<String, Object> defaultUpgradeStageContainerProperties = new HashMap<>();
    defaultUpgradeStageContainerProperties.put("instanceUnitType", "PERCENTAGE");
    defaultUpgradeStageContainerProperties.put("instanceCount", 100);
    defaultUpgradeStageContainerProperties.put("downsizeInstanceUnitType", "PERCENTAGE");
    defaultUpgradeStageContainerProperties.put("downsizeInstanceCount", 100);

    workflowPhase.addPhaseStep(aPhaseStep(CONTAINER_DEPLOY, Constants.DEPLOY_CONTAINERS)
                                   .addStep(aGraphNode()
                                                .withId(generateUuid())
                                                .withType(KUBERNETES_DEPLOY.name())
                                                .withName(Constants.UPGRADE_CONTAINERS)
                                                .withProperties(defaultUpgradeStageContainerProperties)
                                                .build())
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_STAGE_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                                   .build());

    Map<String, Object> defaultRouteUpdateProperties = new HashMap<>();
    defaultRouteUpdateProperties.put("service1", PRIMARY_SERVICE_NAME_EXPRESSION);
    defaultRouteUpdateProperties.put("service2", STAGE_SERVICE_NAME_EXPRESSION);
    workflowPhase.addPhaseStep(aPhaseStep(ROUTE_UPDATE, Constants.ROUTE_UPDATE)
                                   .addStep(aGraphNode()
                                                .withId(generateUuid())
                                                .withType(KUBERNETES_SWAP_SERVICE_SELECTORS.name())
                                                .withName(Constants.KUBERNETES_SWAP_SERVICES_PRIMARY_STAGE)
                                                .withProperties(defaultRouteUpdateProperties)
                                                .build())
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  public void generateNewWorkflowPhaseStepsForSSH(
      String appId, WorkflowPhase workflowPhase, OrchestrationWorkflowType orchestrationWorkflowType) {
    // For DC only - for other types it has to be customized

    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
    StateType stateType;
    if (orchestrationWorkflowType == ROLLING) {
      stateType = ROLLING_NODE_SELECT;
    } else {
      stateType = infrastructureMapping.getComputeProviderType().equals(PHYSICAL_DATA_CENTER.name()) ? DC_NODE_SELECT
                                                                                                     : AWS_NODE_SELECT;
    }

    if (!asList(ROLLING_NODE_SELECT, DC_NODE_SELECT, AWS_NODE_SELECT).contains(stateType)) {
      throw new InvalidRequestException("Unsupported state type: " + stateType, USER);
    }

    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    final PhaseStepBuilder infrastructurePhaseStepBuilder =
        aPhaseStep(INFRASTRUCTURE_NODE, Constants.INFRASTRUCTURE_NODE_NAME);

    infrastructurePhaseStepBuilder.addStep(aGraphNode()
                                               .withType(stateType.name())
                                               .withName(Constants.SELECT_NODE_NAME)
                                               .addProperty("specificHosts", false)
                                               .addProperty("instanceCount", 1)
                                               .addProperty("excludeSelectedHostsFromFuturePhases", true)
                                               .build());

    workflowPhase.addPhaseStep(infrastructurePhaseStepBuilder.build());

    List<GraphNode> disableServiceSteps = commandNodes(commandMap, CommandType.DISABLE);
    List<GraphNode> enableServiceSteps = commandNodes(commandMap, CommandType.ENABLE);

    if (attachElbSteps(infrastructureMapping)) {
      disableServiceSteps.add(aGraphNode()
                                  .withType(ELASTIC_LOAD_BALANCER.name())
                                  .withName("Elastic Load Balancer")
                                  .addProperty("operation", Disable)
                                  .build());
      enableServiceSteps.add(aGraphNode()
                                 .withType(ELASTIC_LOAD_BALANCER.name())
                                 .withName("Elastic Load Balancer")
                                 .addProperty("operation", Enable)
                                 .build());
    }

    workflowPhase.addPhaseStep(
        aPhaseStep(DISABLE_SERVICE, Constants.DISABLE_SERVICE).addAllSteps(disableServiceSteps).build());

    workflowPhase.addPhaseStep(aPhaseStep(DEPLOY_SERVICE, Constants.DEPLOY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.INSTALL))
                                   .build());

    workflowPhase.addPhaseStep(
        aPhaseStep(ENABLE_SERVICE, Constants.ENABLE_SERVICE).addAllSteps(enableServiceSteps).build());

    workflowPhase.addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  private boolean attachElbSteps(InfrastructureMapping infrastructureMapping) {
    return (infrastructureMapping instanceof PhysicalInfrastructureMappingBase
               && isNotBlank(((PhysicalInfrastructureMappingBase) infrastructureMapping).getLoadBalancerId()))
        || (infrastructureMapping instanceof AwsInfrastructureMapping
               && isNotBlank(((AwsInfrastructureMapping) infrastructureMapping).getLoadBalancerId()));
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForPCF(WorkflowPhase workflowPhase) {
    return aWorkflowPhase()
        .withName(Constants.ROLLBACK_PREFIX + workflowPhase.getName())
        .withRollback(true)
        .withServiceId(workflowPhase.getServiceId())
        .withComputeProviderId(workflowPhase.getComputeProviderId())
        .withInfraMappingName(workflowPhase.getInfraMappingName())
        .withPhaseNameForRollback(workflowPhase.getName())
        .withDeploymentType(workflowPhase.getDeploymentType())
        .withInfraMappingId(workflowPhase.getInfraMappingId())
        .addPhaseStep(aPhaseStep(PhaseStepType.PCF_RESIZE, Constants.DEPLOY)
                          .addStep(aGraphNode()
                                       .withId(generateUuid())
                                       .withType(PCF_ROLLBACK.name())
                                       .withName(Constants.PCF_ROLLBACK)
                                       .withRollback(true)
                                       .build())
                          .withPhaseStepNameForRollback(Constants.DEPLOY)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        // When we rolling back the verification steps the same criterie to run if deployment is needed should be used
        .addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                          .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        .addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build())
        .build();
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForHelm(WorkflowPhase workflowPhase) {
    return aWorkflowPhase()
        .withName(Constants.ROLLBACK_PREFIX + workflowPhase.getName())
        .withRollback(true)
        .withServiceId(workflowPhase.getServiceId())
        .withComputeProviderId(workflowPhase.getComputeProviderId())
        .withInfraMappingName(workflowPhase.getInfraMappingName())
        .withPhaseNameForRollback(workflowPhase.getName())
        .withDeploymentType(workflowPhase.getDeploymentType())
        .withInfraMappingId(workflowPhase.getInfraMappingId())
        .addPhaseStep(aPhaseStep(PhaseStepType.HELM_DEPLOY, Constants.DEPLOY_CONTAINERS)
                          .addStep(aGraphNode()
                                       .withId(generateUuid())
                                       .withType(HELM_ROLLBACK.name())
                                       .withName(Constants.HELM_ROLLBACK)
                                       .withRollback(true)
                                       .build())
                          .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        // When we rolling back the verification steps the same criterie to run if deployment is needed should be used
        .addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                          .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        .addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build())
        .build();
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForAwsAmi(WorkflowPhase workflowPhase) {
    return aWorkflowPhase()
        .withName(Constants.ROLLBACK_PREFIX + workflowPhase.getName())
        .withRollback(true)
        .withServiceId(workflowPhase.getServiceId())
        .withComputeProviderId(workflowPhase.getComputeProviderId())
        .withInfraMappingName(workflowPhase.getInfraMappingName())
        .withPhaseNameForRollback(workflowPhase.getName())
        .withDeploymentType(workflowPhase.getDeploymentType())
        .withInfraMappingId(workflowPhase.getInfraMappingId())
        .addPhaseStep(aPhaseStep(AMI_DEPLOY_AUTOSCALING_GROUP, ROLLBACK_SERVICE)
                          .addStep(aGraphNode()
                                       .withId(generateUuid())
                                       .withType(AWS_AMI_SERVICE_ROLLBACK.name())
                                       .withName(Constants.ROLLBACK_AWS_AMI_CLUSTER)
                                       .withRollback(true)
                                       .build())
                          .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        .addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                          .withRollback(true)
                          .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        .addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build())
        .build();
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForAwsAmiBlueGreen(WorkflowPhase workflowPhase) {
    return aWorkflowPhase()
        .withName(Constants.ROLLBACK_PREFIX + workflowPhase.getName())
        .withRollback(true)
        .withServiceId(workflowPhase.getServiceId())
        .withComputeProviderId(workflowPhase.getComputeProviderId())
        .withInfraMappingName(workflowPhase.getInfraMappingName())
        .withPhaseNameForRollback(workflowPhase.getName())
        .withDeploymentType(workflowPhase.getDeploymentType())
        .withInfraMappingId(workflowPhase.getInfraMappingId())
        .addPhaseStep(aPhaseStep(AMI_SWITCH_AUTOSCALING_GROUP_ROUTES, ROLLBACK_SERVICE)
                          .addStep(aGraphNode()
                                       .withId(generateUuid())
                                       .withType(AWS_AMI_ROLLBACK_SWITCH_ROUTES.name())
                                       .withName(ROLLBACK_AUTOSCALING_GROUP_ROUTE)
                                       .withRollback(true)
                                       .build())
                          .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        .addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                          .withRollback(true)
                          .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        .addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build())
        .build();
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForAwsLambda(WorkflowPhase workflowPhase) {
    return aWorkflowPhase()
        .withName(Constants.ROLLBACK_PREFIX + workflowPhase.getName())
        .withRollback(true)
        .withServiceId(workflowPhase.getServiceId())
        .withComputeProviderId(workflowPhase.getComputeProviderId())
        .withInfraMappingName(workflowPhase.getInfraMappingName())
        .withPhaseNameForRollback(workflowPhase.getName())
        .withDeploymentType(workflowPhase.getDeploymentType())
        .withInfraMappingId(workflowPhase.getInfraMappingId())
        .addPhaseStep(aPhaseStep(DEPLOY_AWS_LAMBDA, Constants.DEPLOY_SERVICE)
                          .addStep(aGraphNode()
                                       .withId(generateUuid())
                                       .withType(AWS_LAMBDA_ROLLBACK.name())
                                       .withName(Constants.ROLLBACK_AWS_LAMBDA)
                                       .withRollback(true)
                                       .build())
                          .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        // Verificanion is not exactly rollbacking operation. It should be executed if deployment is needed
        .addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                          .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        .addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build())
        .build();
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForEcs(WorkflowPhase workflowPhase) {
    return aWorkflowPhase()
        .withName(Constants.ROLLBACK_PREFIX + workflowPhase.getName())
        .withRollback(true)
        .withServiceId(workflowPhase.getServiceId())
        .withComputeProviderId(workflowPhase.getComputeProviderId())
        .withInfraMappingName(workflowPhase.getInfraMappingName())
        .withPhaseNameForRollback(workflowPhase.getName())
        .withDeploymentType(workflowPhase.getDeploymentType())
        .withInfraMappingId(workflowPhase.getInfraMappingId())
        .addPhaseStep(aPhaseStep(CONTAINER_DEPLOY, Constants.DEPLOY_CONTAINERS)
                          .addStep(aGraphNode()
                                       .withId(generateUuid())
                                       .withType(ECS_SERVICE_ROLLBACK.name())
                                       .withName(Constants.ROLLBACK_CONTAINERS)
                                       .withRollback(true)
                                       .build())
                          .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        // When we rolling back the verification steps the same criterie to run if deployment is needed should be used
        .addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                          .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        .addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build())
        .build();
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForAwsCodeDeploy(WorkflowPhase workflowPhase) {
    return aWorkflowPhase()
        .withName(Constants.ROLLBACK_PREFIX + workflowPhase.getName())
        .withRollback(true)
        .withServiceId(workflowPhase.getServiceId())
        .withComputeProviderId(workflowPhase.getComputeProviderId())
        .withInfraMappingName(workflowPhase.getInfraMappingName())
        .withPhaseNameForRollback(workflowPhase.getName())
        .withDeploymentType(workflowPhase.getDeploymentType())
        .withInfraMappingId(workflowPhase.getInfraMappingId())
        .addPhaseStep(aPhaseStep(DEPLOY_AWSCODEDEPLOY, Constants.DEPLOY_SERVICE)
                          .addStep(aGraphNode()
                                       .withId(generateUuid())
                                       .withType(AWS_CODEDEPLOY_ROLLBACK.name())
                                       .withName(Constants.ROLLBACK_AWS_CODE_DEPLOY)
                                       .withRollback(true)
                                       .build())
                          .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        // When we rolling back the verification steps the same criterie to run if deployment is needed should be used
        .addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                          .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        .addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build())
        .build();
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForSSH(String appId, WorkflowPhase workflowPhase) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());

    List<GraphNode> disableServiceSteps = commandNodes(commandMap, CommandType.DISABLE, true);
    List<GraphNode> enableServiceSteps = commandNodes(commandMap, CommandType.ENABLE, true);

    if (attachElbSteps(infrastructureMapping)) {
      disableServiceSteps.add(aGraphNode()
                                  .withType(ELASTIC_LOAD_BALANCER.name())
                                  .withName("Elastic Load Balancer")
                                  .addProperty("operation", Disable)
                                  .withRollback(true)
                                  .build());
      enableServiceSteps.add(aGraphNode()
                                 .withType(ELASTIC_LOAD_BALANCER.name())
                                 .withName("Elastic Load Balancer")
                                 .addProperty("operation", Enable)
                                 .withRollback(true)
                                 .build());
    }

    return aWorkflowPhase()
        .withName(Constants.ROLLBACK_PREFIX + workflowPhase.getName())
        .withRollback(true)
        .withServiceId(workflowPhase.getServiceId())
        .withComputeProviderId(workflowPhase.getComputeProviderId())
        .withInfraMappingName(workflowPhase.getInfraMappingName())
        .withPhaseNameForRollback(workflowPhase.getName())
        .withDeploymentType(workflowPhase.getDeploymentType())
        .withInfraMappingId(workflowPhase.getInfraMappingId())
        .addPhaseStep(aPhaseStep(DISABLE_SERVICE, Constants.DISABLE_SERVICE)
                          .addAllSteps(disableServiceSteps)
                          .withPhaseStepNameForRollback(Constants.ENABLE_SERVICE)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        .addPhaseStep(aPhaseStep(STOP_SERVICE, Constants.STOP_SERVICE)
                          .addAllSteps(commandNodes(commandMap, CommandType.STOP, true))
                          .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        .addPhaseStep(aPhaseStep(DEPLOY_SERVICE, Constants.DEPLOY_SERVICE)
                          .addAllSteps(commandNodes(commandMap, CommandType.INSTALL, true))
                          .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        .addPhaseStep(aPhaseStep(ENABLE_SERVICE, Constants.ENABLE_SERVICE)
                          .addAllSteps(enableServiceSteps)
                          .withPhaseStepNameForRollback(Constants.DISABLE_SERVICE)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        // When we rolling back the verification steps the same criterie to run if deployment is needed should be
        // used
        .addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                          .addAllSteps(commandNodes(commandMap, CommandType.VERIFY, true))
                          .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        .addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build())
        .build();
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForKubernetes(
      WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    if (workflowPhase.isDaemonSet() || workflowPhase.isStatefulSet()) {
      return generateRollbackSetupWorkflowPhase(workflowPhase);
    }

    WorkflowPhaseBuilder workflowPhaseBuilder =
        aWorkflowPhase()
            .withName(Constants.ROLLBACK_PREFIX + workflowPhase.getName())
            .withRollback(true)
            .withServiceId(workflowPhase.getServiceId())
            .withComputeProviderId(workflowPhase.getComputeProviderId())
            .withInfraMappingName(workflowPhase.getInfraMappingName())
            .withPhaseNameForRollback(workflowPhase.getName())
            .withDeploymentType(workflowPhase.getDeploymentType())
            .withInfraMappingId(workflowPhase.getInfraMappingId())
            .addPhaseStep(aPhaseStep(CONTAINER_DEPLOY, Constants.DEPLOY_CONTAINERS)
                              .addStep(aGraphNode()
                                           .withId(generateUuid())
                                           .withType(KUBERNETES_DEPLOY_ROLLBACK.name())
                                           .withName(Constants.ROLLBACK_CONTAINERS)
                                           .withRollback(true)
                                           .build())
                              .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                              .withStatusForRollback(ExecutionStatus.SUCCESS)
                              .withRollback(true)
                              .build());
    if (serviceSetupRequired) {
      workflowPhaseBuilder.addPhaseStep(aPhaseStep(CONTAINER_SETUP, Constants.SETUP_CONTAINER)
                                            .addStep(aGraphNode()
                                                         .withId(generateUuid())
                                                         .withType(KUBERNETES_SETUP_ROLLBACK.name())
                                                         .withName(Constants.ROLLBACK_KUBERNETES_SETUP)
                                                         .withRollback(true)
                                                         .build())
                                            .withPhaseStepNameForRollback(Constants.SETUP_CONTAINER)
                                            .withStatusForRollback(ExecutionStatus.SUCCESS)
                                            .withRollback(true)
                                            .build());
    }

    // When we rolling back the verification steps the same criterie to run if deployment is needed should be used
    workflowPhaseBuilder
        .addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                          .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        .addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build());
    return workflowPhaseBuilder.build();
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForKubernetesBlueGreen(
      WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    if (workflowPhase.isDaemonSet() || workflowPhase.isStatefulSet()) {
      throw new InvalidRequestException("DaemonSet and StatefulSet are not supported with Blue/Green Deployment", USER);
    }

    Map<String, Object> defaultRouteUpdateProperties = new HashMap<>();
    defaultRouteUpdateProperties.put("service1", PRIMARY_SERVICE_NAME_EXPRESSION);
    defaultRouteUpdateProperties.put("service2", STAGE_SERVICE_NAME_EXPRESSION);

    WorkflowPhaseBuilder workflowPhaseBuilder =
        aWorkflowPhase()
            .withName(Constants.ROLLBACK_PREFIX + workflowPhase.getName())
            .withRollback(true)
            .withServiceId(workflowPhase.getServiceId())
            .withComputeProviderId(workflowPhase.getComputeProviderId())
            .withInfraMappingName(workflowPhase.getInfraMappingName())
            .withPhaseNameForRollback(workflowPhase.getName())
            .withDeploymentType(workflowPhase.getDeploymentType())
            .withInfraMappingId(workflowPhase.getInfraMappingId())
            .addPhaseStep(aPhaseStep(ROUTE_UPDATE, Constants.ROUTE_UPDATE)
                              .withPhaseStepNameForRollback(Constants.ROUTE_UPDATE)
                              .addStep(aGraphNode()
                                           .withId(generateUuid())
                                           .withType(KUBERNETES_SWAP_SERVICE_SELECTORS.name())
                                           .withName(Constants.KUBERNETES_SWAP_SERVICES_PRIMARY_STAGE)
                                           .withProperties(defaultRouteUpdateProperties)
                                           .build())
                              .withRollback(true)
                              .build())
            .addPhaseStep(aPhaseStep(CONTAINER_DEPLOY, Constants.DEPLOY_CONTAINERS)
                              .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                              .withStatusForRollback(ExecutionStatus.SUCCESS)
                              .withRollback(true)
                              .build());
    if (serviceSetupRequired) {
      workflowPhaseBuilder.addPhaseStep(aPhaseStep(CONTAINER_SETUP, Constants.SETUP_CONTAINER)
                                            .withPhaseStepNameForRollback(Constants.SETUP_CONTAINER)
                                            .withStatusForRollback(ExecutionStatus.SUCCESS)
                                            .withRollback(true)
                                            .build());
    }

    // When we rolling back the verification steps the same criterie to run if deployment is needed should be used
    workflowPhaseBuilder
        .addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                          .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        .addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build());
    return workflowPhaseBuilder.build();
  }

  public WorkflowPhase generateRollbackSetupWorkflowPhase(WorkflowPhase workflowPhase) {
    return aWorkflowPhase()
        .withName(Constants.ROLLBACK_PREFIX + workflowPhase.getName())
        .withRollback(true)
        .withDaemonSet(workflowPhase.isDaemonSet())
        .withStatefulSet(workflowPhase.isStatefulSet())
        .withServiceId(workflowPhase.getServiceId())
        .withComputeProviderId(workflowPhase.getComputeProviderId())
        .withInfraMappingName(workflowPhase.getInfraMappingName())
        .withPhaseNameForRollback(workflowPhase.getName())
        .withDeploymentType(workflowPhase.getDeploymentType())
        .withInfraMappingId(workflowPhase.getInfraMappingId())
        .addPhaseStep(aPhaseStep(CONTAINER_SETUP, Constants.SETUP_CONTAINER)
                          .addStep(aGraphNode()
                                       .withId(generateUuid())
                                       .withType(KUBERNETES_SETUP_ROLLBACK.name())
                                       .withName(Constants.ROLLBACK_KUBERNETES_SETUP)
                                       .withRollback(true)
                                       .build())
                          .withPhaseStepNameForRollback(Constants.SETUP_CONTAINER)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        // When we rolling back the verification steps the same criterie to run if deployment is needed should be used
        .addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                          .withPhaseStepNameForRollback(Constants.SETUP_CONTAINER)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        .addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build())
        .build();
  }

  private Map<CommandType, List<Command>> getCommandTypeListMap(Service service) {
    Map<CommandType, List<Command>> commandMap = new HashMap<>();
    List<ServiceCommand> serviceCommands = service.getServiceCommands();
    if (serviceCommands == null) {
      return commandMap;
    }
    for (ServiceCommand sc : serviceCommands) {
      if (sc.getCommand() == null || sc.getCommand().getCommandType() == null) {
        continue;
      }
      commandMap.computeIfAbsent(sc.getCommand().getCommandType(), k -> new ArrayList<>()).add(sc.getCommand());
    }
    return commandMap;
  }

  private List<GraphNode> commandNodes(Map<CommandType, List<Command>> commandMap, CommandType commandType) {
    return commandNodes(commandMap, commandType, false);
  }

  private List<GraphNode> commandNodes(
      Map<CommandType, List<Command>> commandMap, CommandType commandType, boolean rollback) {
    List<GraphNode> nodes = new ArrayList<>();

    List<Command> commands = commandMap.get(commandType);
    if (commands == null) {
      return nodes;
    }

    for (Command command : commands) {
      nodes.add(aGraphNode()
                    .withId(generateUuid())
                    .withType(COMMAND.name())
                    .withName(command.getName())
                    .addProperty("commandName", command.getName())
                    .withRollback(rollback)
                    .build());
    }
    return nodes;
  }

  /***
   * Populates the workflow level data to Phase. It Validates the service and inframapping for Basics and Multi
   * Service deployment. Resets Node selection if environment or inframapping changed.
   * @param orchestrationWorkflow
   * @param templateExpressions
   * @param appId
   * @param serviceId
   * @param inframappingId
   * @param envChanged
   * @param inframappingChanged
   * @return OrchestrationWorkflow
   */
  public OrchestrationWorkflow propagateWorkflowDataToPhases(OrchestrationWorkflow orchestrationWorkflow,
      List<TemplateExpression> templateExpressions, String appId, String serviceId, String inframappingId,
      boolean envChanged, boolean inframappingChanged) {
    if (orchestrationWorkflow != null) {
      OrchestrationWorkflowType orchestrationWorkflowType = orchestrationWorkflow.getOrchestrationWorkflowType();
      if (orchestrationWorkflowType.equals(BASIC) || orchestrationWorkflowType.equals(ROLLING)
          || orchestrationWorkflowType.equals(BLUE_GREEN)) {
        handleBasicWorkflow((CanaryOrchestrationWorkflow) orchestrationWorkflow, templateExpressions, appId, serviceId,
            inframappingId, envChanged, inframappingChanged);
      } else if (orchestrationWorkflowType.equals(MULTI_SERVICE) || orchestrationWorkflowType.equals(CANARY)) {
        handleCanaryOrMultiServiceWorkflow(
            orchestrationWorkflow, templateExpressions, appId, envChanged, inframappingChanged);
      }
    }
    return orchestrationWorkflow;
  }

  public void handleBasicWorkflow(CanaryOrchestrationWorkflow canaryOrchestrationWorkflow,
      List<TemplateExpression> templateExpressions, String appId, String serviceId, String inframappingId,
      boolean envChanged, boolean inframappingChanged) {
    TemplateExpression envExpression =
        WorkflowServiceTemplateHelper.getTemplateExpression(templateExpressions, "envId");
    if (envExpression != null) {
      canaryOrchestrationWorkflow.addToUserVariables(asList(envExpression));
    }
    if (canaryOrchestrationWorkflow.getWorkflowPhases() != null) {
      for (WorkflowPhase phase : canaryOrchestrationWorkflow.getWorkflowPhases()) {
        WorkflowServiceTemplateHelper.setTemplateExpresssionsToPhase(templateExpressions, phase);
        validateServiceCompatibility(appId, serviceId, phase.getServiceId());
        if (serviceId != null) {
          phase.setServiceId(serviceId);
        }
        setInframappingDetails(appId, inframappingId, phase, envChanged, inframappingChanged);
        if (inframappingChanged || envChanged) {
          resetNodeSelection(phase);
        }
      }
    }
    Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap = canaryOrchestrationWorkflow.getRollbackWorkflowPhaseIdMap();
    if (rollbackWorkflowPhaseIdMap != null) {
      rollbackWorkflowPhaseIdMap.values().forEach(phase -> {
        if (serviceId != null) {
          phase.setServiceId(serviceId);
        }
        setInframappingDetails(appId, inframappingId, phase, envChanged, inframappingChanged);
      });
    }
  }

  /**
   * sets inframapping and cloud provider details along with deployment type
   *
   * @param inframappingId
   * @param phase
   */
  private void setInframappingDetails(
      String appId, String inframappingId, WorkflowPhase phase, boolean envChanged, boolean infraChanged) {
    if (inframappingId != null) {
      if (!inframappingId.equals(phase.getInfraMappingId())) {
        phase.setInfraMappingId(inframappingId);
        InfrastructureMapping infrastructureMapping =
            infrastructureMappingService.get(appId, phase.getInfraMappingId());
        notNullCheck("InfraMapping", infrastructureMapping, USER);
        phase.setComputeProviderId(infrastructureMapping.getComputeProviderSettingId());
        phase.setInfraMappingName(infrastructureMapping.getName());
        phase.setDeploymentType(DeploymentType.valueOf(infrastructureMapping.getDeploymentType()));
        resetNodeSelection(phase);
      }
    } else if (envChanged && !infraChanged) {
      unsetInfraMappingDetails(phase);
    }
  }

  private void handleCanaryOrMultiServiceWorkflow(OrchestrationWorkflow orchestrationWorkflow,
      List<TemplateExpression> templateExpressions, String appId, boolean envChanged, boolean inframappingChanged) {
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
    canaryOrchestrationWorkflow.addToUserVariables(templateExpressions);
    // If envId changed nullify the infraMapping Ids
    if (canaryOrchestrationWorkflow.getWorkflowPhases() != null) {
      for (WorkflowPhase phase : canaryOrchestrationWorkflow.getWorkflowPhases()) {
        if (envChanged) {
          unsetInfraMappingDetails(phase);
          resetNodeSelection(phase);
        }
        if (inframappingChanged) {
          resetNodeSelection(phase);
        }
        // If environment templatized, then templatize infra automatically
        List<TemplateExpression> phaseTemplateExpressions = phase.getTemplateExpressions();
        if (phaseTemplateExpressions == null) {
          phaseTemplateExpressions = new ArrayList<>();
        }
        if (WorkflowServiceTemplateHelper.isEnvironmentTemplatized(templateExpressions)
            && !WorkflowServiceTemplateHelper.isInfraTemplatized(phaseTemplateExpressions)) {
          Service service = serviceResourceService.get(appId, phase.getServiceId(), false);
          notNullCheck("Service", service, USER);
          WorkflowServiceTemplateHelper.templatizeServiceInfra(
              orchestrationWorkflow, phase, phaseTemplateExpressions, service);
        }

        phase.setTemplateExpressions(phaseTemplateExpressions);
      }
    }
    Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap = canaryOrchestrationWorkflow.getRollbackWorkflowPhaseIdMap();
    if (rollbackWorkflowPhaseIdMap != null) {
      rollbackWorkflowPhaseIdMap.values().forEach(phase -> {
        if (envChanged) {
          unsetInfraMappingDetails(phase);
          resetNodeSelection(phase);
        }
        if (inframappingChanged) {
          resetNodeSelection(phase);
        }
      });
    }
  }

  /**
   * Validates service compatibility
   *
   * @param appId
   * @param serviceId
   * @param oldServiceId
   */
  public void validateServiceCompatibility(String appId, String serviceId, String oldServiceId) {
    if (serviceId == null || oldServiceId == null || serviceId.equals(oldServiceId)) {
      return;
    }
    Service oldService = serviceResourceService.get(appId, oldServiceId, false);
    if (oldService == null) {
      // As service has been deleted, compatibility check does not make sense here
      return;
    }

    Service newService = serviceResourceService.get(appId, serviceId, false);
    notNullCheck("service", newService, USER);
    if (oldService.getArtifactType() != null && !oldService.getArtifactType().equals(newService.getArtifactType())) {
      throw new InvalidRequestException(
          "Service [" + newService.getName() + "] is not compatible with the service [" + oldService.getName() + "]",
          USER);
    }
  }

  /**
   * Resets node selection if environment of infra changed
   *
   * @param phase
   */
  public void resetNodeSelection(WorkflowPhase phase) {
    // Update the node selection
    if (phase.getPhaseSteps() == null) {
      return;
    }
    phase.getPhaseSteps()
        .stream()
        .filter(phaseStep -> phaseStep.getPhaseStepType().equals(INFRASTRUCTURE_NODE))
        .map(PhaseStep::getSteps)
        .filter(Objects::nonNull)
        .flatMap(Collection::stream)
        .filter(step -> step.getType().equals(DC_NODE_SELECT.name()) || step.getType().equals(AWS_NODE_SELECT.name()))
        .map(GraphNode::getProperties)
        .filter(properties -> (Boolean) properties.get("specificHosts"))
        .forEach(properties -> {
          properties.put("specificHosts", Boolean.FALSE);
          properties.remove("hostNames");
        });
  }

  public List<Service> getResolvedServices(Workflow workflow, Map<String, String> workflowVariables) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    if (orchestrationWorkflow.isServiceTemplatized()) {
      List<Variable> userVariables = orchestrationWorkflow.getUserVariables();
      List<String> serviceNames = new ArrayList<>();
      if (isNotEmpty(userVariables)) {
        serviceNames = getEntityNames(userVariables, SERVICE);
      }
      List<String> serviceIds = getTemplatizedIds(workflowVariables, serviceNames);
      List<String> templatizedServiceIds = orchestrationWorkflow.getTemplatizedServiceIds();
      List<String> workflowServiceIds = orchestrationWorkflow.getServiceIds();
      if (workflowServiceIds != null) {
        workflowServiceIds.stream()
            .filter(serviceId -> !templatizedServiceIds.contains(serviceId))
            .forEach(serviceIds::add);
      }
      return serviceResourceService.fetchServicesByUuids(workflow.getAppId(), serviceIds);
    } else {
      return workflow.getServices();
    }
  }

  private List<String> getTemplatizedIds(Map<String, String> workflowVariables, List<String> entityNames) {
    List<String> entityIds = new ArrayList<>();
    if (workflowVariables != null) {
      for (Entry<String, String> entry : workflowVariables.entrySet()) {
        String variableName = entry.getKey();
        if (entityNames.contains(variableName)) {
          entityIds.add(workflowVariables.get(variableName));
        }
      }
    }
    return entityIds;
  }

  public List<InfrastructureMapping> getResolvedInfraMappings(
      Workflow workflow, Map<String, String> workflowVariables) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    if (orchestrationWorkflow == null) {
      return new ArrayList<>();
    }
    if (orchestrationWorkflow.isInfraMappingTemplatized()) {
      return resolvedTemplateInfraMappings(workflow, workflowVariables);
    }
    return infrastructureMappingService.getInfraStructureMappingsByUuids(
        workflow.getAppId(), orchestrationWorkflow.getInfraMappingIds());
  }

  public List<String> getResolvedInfraMappingIds(Workflow workflow, Map<String, String> workflowVariables) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    if (orchestrationWorkflow.isInfraMappingTemplatized()) {
      return resolveInfraMappingIds(workflow, workflowVariables);
    } else {
      return orchestrationWorkflow.getInfraMappingIds();
    }
  }

  private List<InfrastructureMapping> resolvedTemplateInfraMappings(
      Workflow workflow, Map<String, String> workflowVariables) {
    List<String> infraMappingIds = resolveInfraMappingIds(workflow, workflowVariables);
    return infrastructureMappingService.getInfraStructureMappingsByUuids(workflow.getAppId(), infraMappingIds);
  }

  private List<String> resolveInfraMappingIds(Workflow workflow, Map<String, String> workflowVariables) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    List<Variable> userVariables = orchestrationWorkflow.getUserVariables();
    List<String> infraMappingNames = new ArrayList<>();
    if (userVariables != null) {
      infraMappingNames = getEntityNames(userVariables, INFRASTRUCTURE_MAPPING);
    }
    List<String> infraMappingIds = getTemplatizedIds(workflowVariables, infraMappingNames);
    List<String> templatizedInfraMappingIds = orchestrationWorkflow.getTemplatizedInfraMappingIds();
    List<String> workflowInframappingIds = orchestrationWorkflow.getInfraMappingIds();
    if (workflowInframappingIds != null) {
      workflowInframappingIds.stream()
          .filter(infraMappingId -> !templatizedInfraMappingIds.contains(infraMappingId))
          .forEach(infraMappingIds::add);
    }
    return infraMappingIds;
  }

  private List<String> getEntityNames(List<Variable> userVariables, EntityType entityType) {
    return userVariables.stream()
        .filter(variable -> entityType.equals(variable.getEntityType()))
        .map(Variable::getName)
        .collect(toList());
  }

  public void unsetInfraMappingDetails(WorkflowPhase phase) {
    phase.setComputeProviderId(null);
    phase.setInfraMappingId(null);
    phase.setInfraMappingName(null);
    // phase.setDeploymentType(null);
  }
}

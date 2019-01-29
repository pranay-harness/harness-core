package software.wings.service.impl.workflow;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
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
import static software.wings.beans.PhaseStepType.ECS_UPDATE_LISTENER_BG;
import static software.wings.beans.PhaseStepType.ECS_UPDATE_ROUTE_53_DNS_WEIGHT;
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
import static software.wings.common.Constants.CHANGE_ROUTE53_DNS_WEIGHTS;
import static software.wings.common.Constants.ECS_DAEMON_SCHEDULING_STRATEGY;
import static software.wings.common.Constants.ECS_ROUTE53_DNS_WEIGHTS;
import static software.wings.common.Constants.ECS_SWAP_TARGET_GROUPS;
import static software.wings.common.Constants.ECS_SWAP_TARGET_GROUPS_ROLLBACK;
import static software.wings.common.Constants.PRIMARY_SERVICE_NAME_EXPRESSION;
import static software.wings.common.Constants.ROLLBACK_AUTOSCALING_GROUP_ROUTE;
import static software.wings.common.Constants.ROLLBACK_ECS_ROUTE53_DNS_WEIGHTS;
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
import static software.wings.sm.StateType.ECS_BG_SERVICE_SETUP;
import static software.wings.sm.StateType.ECS_BG_SERVICE_SETUP_ROUTE53;
import static software.wings.sm.StateType.ECS_DAEMON_SERVICE_SETUP;
import static software.wings.sm.StateType.ECS_LISTENER_UPDATE;
import static software.wings.sm.StateType.ECS_ROUTE53_DNS_WEIGHT_UPDATE;
import static software.wings.sm.StateType.ECS_SERVICE_DEPLOY;
import static software.wings.sm.StateType.ECS_SERVICE_ROLLBACK;
import static software.wings.sm.StateType.ECS_SERVICE_SETUP;
import static software.wings.sm.StateType.ECS_SERVICE_SETUP_ROLLBACK;
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

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscaler;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.expression.ExpressionEvaluator;
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
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.common.Constants;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.StateType;
import software.wings.sm.states.AwsCodeDeployState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
  @Inject private ExpressionEvaluator expressionEvaluator;

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

  public List<DeploymentType> obtainDeploymentTypes(OrchestrationWorkflow orchestrationWorkflow) {
    if (orchestrationWorkflow instanceof CanaryOrchestrationWorkflow) {
      List<WorkflowPhase> workflowPhases = ((CanaryOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhases();
      if (isNotEmpty(workflowPhases)) {
        return workflowPhases.stream()
            .map(WorkflowPhase::getDeploymentType)
            .filter(Objects::nonNull)
            .distinct()
            .collect(toList());
      }
    }
    return new ArrayList<>();
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
      preDeploymentSteps.getSteps().add(GraphNode.builder().type(ARTIFACT_CHECK.name()).name("Artifact Check").build());
      return true;
    }
  }

  public String obtainTemplatedEnvironmentId(Workflow workflow, Map<String, String> workflowVariables) {
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
    return null;
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
        "Workflow [" + workflow.getName() + "] environment parameterized. However, the value not supplied", USER);
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

    DeploymentType deploymentType =
        serviceResourceService.getDeploymentType(infrastructureMapping, null, infrastructureMapping.getServiceId());
    workflowPhase.setDeploymentType(deploymentType);
  }

  public void generateNewWorkflowPhaseStepsForAWSAmiBlueGreen(
      String appId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

    if (serviceSetupRequired) {
      InfrastructureMapping infraMapping = infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
      if (infraMapping instanceof AwsAmiInfrastructureMapping) {
        Map<String, Object> defaultData = newHashMap();
        defaultData.put("maxInstances", 10);
        defaultData.put("autoScalingSteadyStateTimeout", 10);
        defaultData.put("blueGreen", true);
        phaseSteps.add(aPhaseStep(AMI_AUTOSCALING_GROUP_SETUP, Constants.SETUP_AUTOSCALING_GROUP)
                           .addStep(GraphNode.builder()
                                        .id(generateUuid())
                                        .type(AWS_AMI_SERVICE_SETUP.name())
                                        .name("AWS AutoScaling Group Setup")
                                        .properties(defaultData)
                                        .build())
                           .build());
      }
    }
    phaseSteps.add(aPhaseStep(AMI_DEPLOY_AUTOSCALING_GROUP, Constants.DEPLOY_SERVICE)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(AWS_AMI_SERVICE_DEPLOY.name())
                                    .name(Constants.UPGRADE_AUTOSCALING_GROUP)
                                    .build())
                       .build());

    phaseSteps.add(
        aPhaseStep(VERIFY_SERVICE, VERIFY_STAGING).addAllSteps(commandNodes(commandMap, CommandType.VERIFY)).build());

    Map<String, Object> defaultDataSwitchRoutes = newHashMap();
    defaultDataSwitchRoutes.put("downsizeOldAsg", true);
    phaseSteps.add(aPhaseStep(AMI_SWITCH_AUTOSCALING_GROUP_ROUTES, SWAP_AUTOSCALING_GROUP_ROUTE)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(AWS_AMI_SWITCH_ROUTES.name())
                                    .name(UPGRADE_AUTOSCALING_GROUP_ROUTE)
                                    .properties(defaultDataSwitchRoutes)
                                    .build())
                       .build());

    phaseSteps.add(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  public void generateNewWorkflowPhaseStepsForAWSAmi(
      String appId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

    if (serviceSetupRequired) {
      InfrastructureMapping infraMapping = infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
      if (infraMapping instanceof AwsAmiInfrastructureMapping) {
        Map<String, Object> defaultData = new HashMap<>();
        defaultData.put("maxInstances", 10);
        defaultData.put("autoScalingSteadyStateTimeout", 10);
        defaultData.put("blueGreen", false);
        phaseSteps.add(aPhaseStep(AMI_AUTOSCALING_GROUP_SETUP, Constants.SETUP_AUTOSCALING_GROUP)
                           .addStep(GraphNode.builder()
                                        .id(generateUuid())
                                        .type(AWS_AMI_SERVICE_SETUP.name())
                                        .name("AWS AutoScaling Group Setup")
                                        .properties(defaultData)
                                        .build())
                           .build());
      }
    }
    phaseSteps.add(aPhaseStep(AMI_DEPLOY_AUTOSCALING_GROUP, Constants.DEPLOY_SERVICE)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(AWS_AMI_SERVICE_DEPLOY.name())
                                    .name(Constants.UPGRADE_AUTOSCALING_GROUP)
                                    .build())
                       .build());

    phaseSteps.add(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                       .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                       .build());

    phaseSteps.add(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  public void generateNewWorkflowPhaseStepsForAWSLambda(String appId, String envId, WorkflowPhase workflowPhase) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

    phaseSteps.add(aPhaseStep(PREPARE_STEPS, Constants.PREPARE_STEPS).build());

    phaseSteps.add(
        aPhaseStep(DEPLOY_AWS_LAMBDA, Constants.DEPLOY_SERVICE)
            .addStep(
                GraphNode.builder().id(generateUuid()).type(AWS_LAMBDA_STATE.name()).name(Constants.AWS_LAMBDA).build())
            .build());

    phaseSteps.add(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                       .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                       .build());

    phaseSteps.add(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  public void generateNewWorkflowPhaseStepsForAWSCodeDeploy(String appId, WorkflowPhase workflowPhase) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

    phaseSteps.add(aPhaseStep(PREPARE_STEPS, Constants.PREPARE_STEPS).build());

    Map<String, String> stateDefaults = getStateDefaults(appId, service.getUuid(), AWS_CODEDEPLOY_STATE);
    GraphNodeBuilder node =
        GraphNode.builder().id(generateUuid()).type(AWS_CODEDEPLOY_STATE.name()).name(Constants.AWS_CODE_DEPLOY);

    if (isNotEmpty(stateDefaults)) {
      Map<String, Object> properties = new HashMap<>();
      if (isNotBlank(stateDefaults.get("bucket"))) {
        properties.put("bucket", stateDefaults.get("bucket"));
      }
      if (isNotBlank(stateDefaults.get("key"))) {
        properties.put("key", stateDefaults.get("key"));
      }
      if (isNotBlank(stateDefaults.get("bundleType"))) {
        properties.put("bundleType", stateDefaults.get("bundleType"));
      }
      node.properties(properties);
    }
    phaseSteps.add(aPhaseStep(DEPLOY_AWSCODEDEPLOY, Constants.DEPLOY_SERVICE).addStep(node.build()).build());

    phaseSteps.add(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                       .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                       .build());

    phaseSteps.add(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
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

  public void generateNewWorkflowPhaseStepsForECS(String appId, WorkflowPhase workflowPhase,
      boolean serviceSetupRequired, OrchestrationWorkflowType orchestrationWorkflowType) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

    boolean isDaemonEcsWorkflow = isDaemonSchedulingStrategy(appId, workflowPhase, orchestrationWorkflowType);
    if (serviceSetupRequired) {
      if (isDaemonEcsWorkflow) {
        phaseSteps.add(aPhaseStep(CONTAINER_SETUP, Constants.SETUP_CONTAINER)
                           .addStep(GraphNode.builder()
                                        .id(generateUuid())
                                        .type(ECS_DAEMON_SERVICE_SETUP.name())
                                        .name(Constants.ECS_DAEMON_SERVICE_SETUP)
                                        .build())
                           .build());
      } else {
        phaseSteps.add(aPhaseStep(CONTAINER_SETUP, Constants.SETUP_CONTAINER)
                           .addStep(GraphNode.builder()
                                        .id(generateUuid())
                                        .type(ECS_SERVICE_SETUP.name())
                                        .name(Constants.ECS_SERVICE_SETUP)
                                        .build())
                           .build());
      }
    }

    if (!isDaemonEcsWorkflow) {
      phaseSteps.add(aPhaseStep(CONTAINER_DEPLOY, Constants.DEPLOY_CONTAINERS)
                         .addStep(GraphNode.builder()
                                      .id(generateUuid())
                                      .type(ECS_SERVICE_DEPLOY.name())
                                      .name(Constants.UPGRADE_CONTAINERS)
                                      .build())
                         .build());
    }

    phaseSteps.add(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                       .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                       .build());

    phaseSteps.add(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  private Map<String, Object> getDefaultDeployPropertyMapForEcsBG() {
    Map<String, Object> deployProperties = newHashMap();
    deployProperties.put("instanceUnitType", "PERCENTAGE");
    deployProperties.put("instanceCount", 100);
    deployProperties.put("downsizeInstanceUnitType", "PERCENTAGE");
    deployProperties.put("downsizeInstanceCount", 100);
    return deployProperties;
  }

  private void addDeployAndVerifyPhaseStepForEcsBG(
      List<PhaseStep> phaseSteps, Map<CommandType, List<Command>> commandMap) {
    phaseSteps.add(aPhaseStep(CONTAINER_DEPLOY, Constants.DEPLOY_CONTAINERS)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(ECS_SERVICE_DEPLOY.name())
                                    .name(Constants.UPGRADE_CONTAINERS)
                                    .properties(getDefaultDeployPropertyMapForEcsBG())
                                    .build())
                       .build());
    phaseSteps.add(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                       .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                       .build());
  }

  public void generateNewWorkflowPhaseStepsForECSBlueGreenRoute53(
      String appId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();
    if (serviceSetupRequired) {
      Map<String, Object> setupProperties = newHashMap();
      setupProperties.put("resizeStrategy", "RESIZE_NEW_FIRST");
      phaseSteps.add(aPhaseStep(CONTAINER_SETUP, Constants.SETUP_CONTAINER)
                         .addStep(GraphNode.builder()
                                      .id(generateUuid())
                                      .type(ECS_BG_SERVICE_SETUP_ROUTE53.name())
                                      .name(Constants.ECS_BG_SERVICE_SETUP_ROUTE_53)
                                      .properties(setupProperties)
                                      .build())
                         .build());
    }

    addDeployAndVerifyPhaseStepForEcsBG(phaseSteps, commandMap);

    Map<String, Object> defaultDataSwitchRoutes = newHashMap();
    defaultDataSwitchRoutes.put("downsizeOldService", true);
    defaultDataSwitchRoutes.put("oldServiceDNSWeight", 0);
    defaultDataSwitchRoutes.put("newServiceDNSWeight", 100);
    defaultDataSwitchRoutes.put("recordTTL", 60);
    phaseSteps.add(aPhaseStep(ECS_UPDATE_ROUTE_53_DNS_WEIGHT, ECS_ROUTE53_DNS_WEIGHTS)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(ECS_ROUTE53_DNS_WEIGHT_UPDATE.name())
                                    .name(CHANGE_ROUTE53_DNS_WEIGHTS)
                                    .properties(defaultDataSwitchRoutes)
                                    .build())
                       .build());

    phaseSteps.add(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  public void generateNewWorkflowPhaseStepsForECSBlueGreen(
      String appId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

    if (serviceSetupRequired) {
      Map<String, Object> defaultSetupProperties = newHashMap();
      defaultSetupProperties.put("resizeStrategy", "RESIZE_NEW_FIRST");
      defaultSetupProperties.put("useLoadBalancer", true);

      phaseSteps.add(aPhaseStep(CONTAINER_SETUP, Constants.SETUP_CONTAINER)
                         .addStep(GraphNode.builder()
                                      .id(generateUuid())
                                      .type(ECS_BG_SERVICE_SETUP.name())
                                      .name(Constants.ECS_BG_SERVICE_SETUP_ELB)
                                      .properties(defaultSetupProperties)
                                      .build())
                         .build());
    }

    addDeployAndVerifyPhaseStepForEcsBG(phaseSteps, commandMap);

    Map<String, Object> defaultDataSwitchRoutes = newHashMap();
    defaultDataSwitchRoutes.put("downsizeOldService", true);
    phaseSteps.add(aPhaseStep(ECS_UPDATE_LISTENER_BG, ECS_SWAP_TARGET_GROUPS)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(ECS_LISTENER_UPDATE.name())
                                    .name(ECS_SWAP_TARGET_GROUPS)
                                    .properties(defaultDataSwitchRoutes)
                                    .build())
                       .build());

    phaseSteps.add(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  boolean isDaemonSchedulingStrategy(
      String appId, WorkflowPhase workflowPhase, OrchestrationWorkflowType orchestrationWorkflowType) {
    boolean isDaemonSchedulingStrategy = false;

    // DaemonSchedulingStrategy is only allowed for Basic workflow
    if (OrchestrationWorkflowType.BASIC.equals(orchestrationWorkflowType)) {
      String serviceId = workflowPhase.getServiceId();
      EcsServiceSpecification serviceSpecification =
          serviceResourceService.getEcsServiceSpecification(appId, serviceId);

      if (serviceSpecification != null) {
        if (isEmpty(serviceSpecification.getServiceSpecJson())) {
          isDaemonSchedulingStrategy =
              ECS_DAEMON_SCHEDULING_STRATEGY.equals(serviceSpecification.getSchedulingStrategy());
        } else {
          Pattern pattern = Pattern.compile("\"schedulingStrategy\":\\s*\"DAEMON\"\\s*", Pattern.CASE_INSENSITIVE);
          Matcher matcher = pattern.matcher(serviceSpecification.getServiceSpecJson());
          isDaemonSchedulingStrategy = matcher.find();
        }
      }
    }

    return isDaemonSchedulingStrategy;
  }

  private WorkflowPhaseBuilder rollbackWorkflow(WorkflowPhase workflowPhase) {
    return aWorkflowPhase()
        .name(Constants.ROLLBACK_PREFIX + workflowPhase.getName())
        .deploymentType(workflowPhase.getDeploymentType())
        .rollback(true)
        .phaseNameForRollback(workflowPhase.getName())
        .serviceId(workflowPhase.getServiceId())
        .computeProviderId(workflowPhase.getComputeProviderId())
        .infraMappingId(workflowPhase.getInfraMappingId())
        .infraMappingName(workflowPhase.getInfraMappingName());
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForPCFBlueGreen(
      WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    if (workflowPhase.isDaemonSet() || workflowPhase.isStatefulSet()) {
      throw new InvalidRequestException("DaemonSet and StatefulSet are not supported with Blue/Green Deployment", USER);
    }

    Map<String, Object> defaultRouteUpdateProperties = new HashMap<>();
    defaultRouteUpdateProperties.put("service1", PRIMARY_SERVICE_NAME_EXPRESSION);
    defaultRouteUpdateProperties.put("service2", STAGE_SERVICE_NAME_EXPRESSION);

    return rollbackWorkflow(workflowPhase)
        .phaseSteps(asList(aPhaseStep(PCF_SWICH_ROUTES, Constants.PCF_BG_MAP_ROUTE)
                               .addStep(GraphNode.builder()
                                            .id(generateUuid())
                                            .type(PCF_BG_MAP_ROUTE.name())
                                            .name(Constants.PCF_BG_SWAP_ROUTE)
                                            .properties(defaultRouteUpdateProperties)
                                            .rollback(true)
                                            .build())
                               .withPhaseStepNameForRollback(Constants.PCF_BG_MAP_ROUTE)
                               .withStatusForRollback(SUCCESS)
                               .withRollback(true)
                               .build(),

            // When we rolling back the verification steps
            // the same criteria to run if deployment is needed should be used
            aPhaseStep(PhaseStepType.PCF_RESIZE, Constants.DEPLOY)
                .addStep(GraphNode.builder()
                             .id(generateUuid())
                             .type(PCF_ROLLBACK.name())
                             .name(Constants.PCF_ROLLBACK)
                             .rollback(true)
                             .build())
                .withPhaseStepNameForRollback(Constants.DEPLOY)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                .withPhaseStepNameForRollback(Constants.PCF_RESIZE)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build()))
        .build();
  }

  public void generateNewWorkflowPhaseStepsForPCFBlueGreen(
      String appId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    if (workflowPhase.isDaemonSet() || workflowPhase.isStatefulSet()) {
      throw new InvalidRequestException("DaemonSet and StatefulSet are not supported with Blue/Green Deployment", USER);
    }

    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

    // SETUP
    if (serviceSetupRequired) {
      Map<String, Object> defaultSetupProperties = new HashMap<>();
      defaultSetupProperties.put("blueGreen", true);
      defaultSetupProperties.put("resizeStrategy", "RESIZE_NEW_FIRST");
      defaultSetupProperties.put("route", "${" + Constants.INFRA_TEMP_ROUTE_PCF + "}");

      phaseSteps.add(aPhaseStep(PCF_SETUP, Constants.SETUP)
                         .addStep(GraphNode.builder()
                                      .id(generateUuid())
                                      .type(PCF_SETUP.name())
                                      .name(Constants.PCF_SETUP)
                                      .properties(defaultSetupProperties)
                                      .build())
                         .build());
    }

    // RESIZE
    Map<String, Object> defaultUpgradeStageContainerProperties = new HashMap<>();
    defaultUpgradeStageContainerProperties.put("instanceUnitType", "PERCENTAGE");
    defaultUpgradeStageContainerProperties.put("instanceCount", 100);
    defaultUpgradeStageContainerProperties.put("downsizeInstanceUnitType", "PERCENTAGE");
    defaultUpgradeStageContainerProperties.put("downsizeInstanceCount", 100);

    phaseSteps.add(aPhaseStep(PCF_RESIZE, Constants.DEPLOY)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(PCF_RESIZE.name())
                                    .name(Constants.PCF_RESIZE)
                                    .properties(defaultUpgradeStageContainerProperties)
                                    .build())
                       .build());

    // Verify
    phaseSteps.add(
        aPhaseStep(VERIFY_SERVICE, VERIFY_STAGING).addAllSteps(commandNodes(commandMap, CommandType.VERIFY)).build());

    // Swap Routes
    Map<String, Object> defaultRouteUpdateProperties = new HashMap<>();
    defaultRouteUpdateProperties.put("downsizeOldApps", false);
    phaseSteps.add(aPhaseStep(PCF_SWICH_ROUTES, Constants.PCF_BG_MAP_ROUTE)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(PCF_BG_MAP_ROUTE.name())
                                    .name(Constants.PCF_BG_SWAP_ROUTE)
                                    .properties(defaultRouteUpdateProperties)
                                    .build())
                       .build());

    // Wrap up
    phaseSteps.add(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  public void generateNewWorkflowPhaseStepsForPCF(String appId, String envId, WorkflowPhase workflowPhase,
      boolean serviceSetupRequired, OrchestrationWorkflowType orchestrationWorkflowType) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

    if (serviceSetupRequired) {
      Map<String, Object> defaultProperties = new HashMap<>();
      defaultProperties.put("blueGreen", false);
      defaultProperties.put("resizeStrategy", "DOWNSIZE_OLD_FIRST");
      defaultProperties.put("route", "${" + Constants.INFRA_ROUTE_PCF + "}");

      phaseSteps.add(aPhaseStep(PCF_SETUP, Constants.SETUP)
                         .addStep(GraphNode.builder()
                                      .id(generateUuid())
                                      .type(PCF_SETUP.name())
                                      .name(Constants.PCF_SETUP)
                                      .properties(defaultProperties)
                                      .build())
                         .build());
    }

    phaseSteps.add(
        aPhaseStep(PCF_RESIZE, Constants.DEPLOY)
            .addStep(GraphNode.builder().id(generateUuid()).type(PCF_RESIZE.name()).name(Constants.PCF_RESIZE).build())
            .build());

    phaseSteps.add(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                       .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                       .build());

    phaseSteps.add(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  public void generateNewWorkflowPhaseStepsForHelm(
      String appId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

    phaseSteps.add(
        aPhaseStep(PhaseStepType.HELM_DEPLOY, Constants.DEPLOY_CONTAINERS)
            .addStep(
                GraphNode.builder().id(generateUuid()).type(HELM_DEPLOY.name()).name(Constants.HELM_DEPLOY).build())
            .build());
    phaseSteps.add(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                       .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                       .build());
    phaseSteps.add(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  public void generateNewWorkflowPhaseStepsForKubernetes(String appId, WorkflowPhase workflowPhase,
      boolean serviceSetupRequired, OrchestrationWorkflowType orchestrationWorkflowType) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

    if (serviceSetupRequired) {
      InfrastructureMapping infraMapping = infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
      if (infraMapping instanceof GcpKubernetesInfrastructureMapping
          && Constants.RUNTIME.equals(((GcpKubernetesInfrastructureMapping) infraMapping).getClusterName())) {
        phaseSteps.add(aPhaseStep(CLUSTER_SETUP, Constants.SETUP_CLUSTER)
                           .addStep(GraphNode.builder()
                                        .id(generateUuid())
                                        .type(GCP_CLUSTER_SETUP.name())
                                        .name("GCP Cluster Setup")
                                        .build())
                           .build());
      }

      Map<String, Object> defaultSetupProperties = new HashMap<>();
      defaultSetupProperties.put("replicationControllerName", "${app.name}-${service.name}-${env.name}");
      defaultSetupProperties.put("resizeStrategy", "RESIZE_NEW_FIRST");
      phaseSteps.add(aPhaseStep(CONTAINER_SETUP, Constants.SETUP_CONTAINER)
                         .addStep(GraphNode.builder()
                                      .id(generateUuid())
                                      .type(KUBERNETES_SETUP.name())
                                      .name(Constants.KUBERNETES_SERVICE_SETUP)
                                      .properties(defaultSetupProperties)
                                      .build())
                         .build());
    }

    if (!workflowPhase.isDaemonSet() && !workflowPhase.isStatefulSet()) {
      Map<String, Object> properties = new HashMap<>();
      if (BASIC.equals(orchestrationWorkflowType)) {
        // Setting instance count always 100 percent
        properties.put("instanceCount", "100");
      }
      phaseSteps.add(aPhaseStep(CONTAINER_DEPLOY, Constants.DEPLOY_CONTAINERS)
                         .addStep(GraphNode.builder()
                                      .id(generateUuid())
                                      .type(KUBERNETES_DEPLOY.name())
                                      .name(Constants.UPGRADE_CONTAINERS)
                                      .properties(properties)
                                      .build())
                         .build());
    }
    phaseSteps.add(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                       .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                       .build());
    phaseSteps.add(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  public void generateNewWorkflowPhaseStepsForKubernetesBlueGreen(
      String appId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    if (workflowPhase.isDaemonSet() || workflowPhase.isStatefulSet()) {
      throw new InvalidRequestException("DaemonSet and StatefulSet are not supported with Blue/Green Deployment", USER);
    }

    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

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

      phaseSteps.add(aPhaseStep(CONTAINER_SETUP, Constants.SETUP_CONTAINER)
                         .addStep(GraphNode.builder()
                                      .id(generateUuid())
                                      .type(KUBERNETES_SETUP.name())
                                      .name(Constants.KUBERNETES_SERVICE_SETUP_BLUEGREEN)
                                      .properties(defaultSetupProperties)
                                      .build())
                         .build());
    }

    Map<String, Object> defaultUpgradeStageContainerProperties = new HashMap<>();
    defaultUpgradeStageContainerProperties.put("instanceUnitType", "PERCENTAGE");
    defaultUpgradeStageContainerProperties.put("instanceCount", 100);
    defaultUpgradeStageContainerProperties.put("downsizeInstanceUnitType", "PERCENTAGE");
    defaultUpgradeStageContainerProperties.put("downsizeInstanceCount", 100);

    phaseSteps.add(aPhaseStep(CONTAINER_DEPLOY, Constants.DEPLOY_CONTAINERS)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(KUBERNETES_DEPLOY.name())
                                    .name(Constants.UPGRADE_CONTAINERS)
                                    .properties(defaultUpgradeStageContainerProperties)
                                    .build())
                       .build());

    phaseSteps.add(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_STAGE_SERVICE)
                       .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                       .build());

    Map<String, Object> defaultRouteUpdateProperties = new HashMap<>();
    defaultRouteUpdateProperties.put("service1", PRIMARY_SERVICE_NAME_EXPRESSION);
    defaultRouteUpdateProperties.put("service2", STAGE_SERVICE_NAME_EXPRESSION);
    phaseSteps.add(aPhaseStep(ROUTE_UPDATE, Constants.ROUTE_UPDATE)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(KUBERNETES_SWAP_SERVICE_SELECTORS.name())
                                    .name(Constants.KUBERNETES_SWAP_SERVICES_PRIMARY_STAGE)
                                    .properties(defaultRouteUpdateProperties)
                                    .build())
                       .build());

    phaseSteps.add(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
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

    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

    final PhaseStepBuilder infrastructurePhaseStepBuilder =
        aPhaseStep(INFRASTRUCTURE_NODE, Constants.INFRASTRUCTURE_NODE_NAME);

    infrastructurePhaseStepBuilder.addStep(GraphNode.builder()
                                               .type(stateType.name())
                                               .name(Constants.SELECT_NODE_NAME)
                                               .properties(ImmutableMap.<String, Object>builder()
                                                               .put("specificHosts", false)
                                                               .put("instanceCount", 1)
                                                               .put("excludeSelectedHostsFromFuturePhases", true)
                                                               .build())
                                               .build());

    phaseSteps.add(infrastructurePhaseStepBuilder.build());

    List<GraphNode> disableServiceSteps = commandNodes(commandMap, CommandType.DISABLE);
    List<GraphNode> enableServiceSteps = commandNodes(commandMap, CommandType.ENABLE);

    if (attachElbSteps(infrastructureMapping)) {
      disableServiceSteps.add(GraphNode.builder()
                                  .type(ELASTIC_LOAD_BALANCER.name())
                                  .name("Elastic Load Balancer")
                                  .properties(ImmutableMap.<String, Object>builder().put("operation", Disable).build())
                                  .build());
      enableServiceSteps.add(GraphNode.builder()
                                 .type(ELASTIC_LOAD_BALANCER.name())
                                 .name("Elastic Load Balancer")
                                 .properties(ImmutableMap.<String, Object>builder().put("operation", Enable).build())
                                 .build());
    }

    phaseSteps.add(aPhaseStep(DISABLE_SERVICE, Constants.DISABLE_SERVICE).addAllSteps(disableServiceSteps).build());

    phaseSteps.add(aPhaseStep(DEPLOY_SERVICE, Constants.DEPLOY_SERVICE)
                       .addAllSteps(commandNodes(commandMap, CommandType.INSTALL))
                       .build());

    phaseSteps.add(aPhaseStep(ENABLE_SERVICE, Constants.ENABLE_SERVICE).addAllSteps(enableServiceSteps).build());

    phaseSteps.add(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                       .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                       .build());

    phaseSteps.add(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  private boolean attachElbSteps(InfrastructureMapping infrastructureMapping) {
    return (infrastructureMapping instanceof PhysicalInfrastructureMappingBase
               && isNotBlank(((PhysicalInfrastructureMappingBase) infrastructureMapping).getLoadBalancerId()))
        || (infrastructureMapping instanceof AwsInfrastructureMapping
               && isNotBlank(((AwsInfrastructureMapping) infrastructureMapping).getLoadBalancerId()));
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForPCF(WorkflowPhase workflowPhase) {
    return rollbackWorkflow(workflowPhase)
        .phaseSteps(asList(aPhaseStep(PhaseStepType.PCF_RESIZE, Constants.DEPLOY)
                               .addStep(GraphNode.builder()
                                            .id(generateUuid())
                                            .type(PCF_ROLLBACK.name())
                                            .name(Constants.PCF_ROLLBACK)
                                            .rollback(true)
                                            .build())
                               .withPhaseStepNameForRollback(Constants.DEPLOY)
                               .withStatusForRollback(SUCCESS)
                               .withRollback(true)
                               .build(),
            // When we rolling back the verification steps
            // the same criteria to run if deployment is needed should be used
            aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build()))
        .build();
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForHelm(WorkflowPhase workflowPhase) {
    return rollbackWorkflow(workflowPhase)
        .phaseSteps(asList(aPhaseStep(PhaseStepType.HELM_DEPLOY, Constants.DEPLOY_CONTAINERS)
                               .addStep(GraphNode.builder()
                                            .id(generateUuid())
                                            .type(HELM_ROLLBACK.name())
                                            .name(Constants.HELM_ROLLBACK)
                                            .rollback(true)
                                            .build())
                               .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                               .withStatusForRollback(SUCCESS)
                               .withRollback(true)
                               .build(),
            // When we rolling back the verification steps the same criterie to run if deployment is needed should be
            // used
            aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build()))
        .build();
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForAwsAmi(WorkflowPhase workflowPhase) {
    return rollbackWorkflow(workflowPhase)
        .phaseSteps(asList(aPhaseStep(AMI_DEPLOY_AUTOSCALING_GROUP, ROLLBACK_SERVICE)
                               .addStep(GraphNode.builder()
                                            .id(generateUuid())
                                            .type(AWS_AMI_SERVICE_ROLLBACK.name())
                                            .name(Constants.ROLLBACK_AWS_AMI_CLUSTER)
                                            .rollback(true)
                                            .build())
                               .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                               .withStatusForRollback(SUCCESS)
                               .withRollback(true)
                               .build(),
            aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                .withRollback(true)
                .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build()))
        .build();
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForAwsAmiBlueGreen(WorkflowPhase workflowPhase) {
    return rollbackWorkflow(workflowPhase)
        .phaseSteps(asList(aPhaseStep(AMI_SWITCH_AUTOSCALING_GROUP_ROUTES, ROLLBACK_SERVICE)
                               .addStep(GraphNode.builder()
                                            .id(generateUuid())
                                            .type(AWS_AMI_ROLLBACK_SWITCH_ROUTES.name())
                                            .name(ROLLBACK_AUTOSCALING_GROUP_ROUTE)
                                            .rollback(true)
                                            .build())
                               .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                               .withStatusForRollback(SUCCESS)
                               .withRollback(true)
                               .build(),
            aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                .withRollback(true)
                .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build()))
        .build();
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForAwsLambda(WorkflowPhase workflowPhase) {
    return rollbackWorkflow(workflowPhase)
        .phaseSteps(asList(aPhaseStep(DEPLOY_AWS_LAMBDA, Constants.DEPLOY_SERVICE)
                               .addStep(GraphNode.builder()
                                            .id(generateUuid())
                                            .type(AWS_LAMBDA_ROLLBACK.name())
                                            .name(Constants.ROLLBACK_AWS_LAMBDA)
                                            .rollback(true)
                                            .build())
                               .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                               .withStatusForRollback(SUCCESS)
                               .withRollback(true)
                               .build(),
            // Verificanion is not exactly rollbacking operation. It should be executed if deployment is needed
            aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build()))
        .build();
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForEcs(
      String appId, WorkflowPhase workflowPhase, OrchestrationWorkflowType orchestrationWorkflowType) {
    List<PhaseStep> phaseSteps = new ArrayList<>();
    if (isDaemonSchedulingStrategy(appId, workflowPhase, orchestrationWorkflowType)) {
      // For Daemon ECS workflow, need to add Setup rollback state
      phaseSteps.add(aPhaseStep(CONTAINER_SETUP, Constants.SETUP_CONTAINER)
                         .addStep(GraphNode.builder()
                                      .id(generateUuid())
                                      .type(ECS_SERVICE_SETUP_ROLLBACK.name())
                                      .name(Constants.ROLLBACK_CONTAINERS)
                                      .rollback(true)
                                      .build())
                         .withPhaseStepNameForRollback(Constants.SETUP_CONTAINER)
                         .withStatusForRollback(SUCCESS)
                         .withRollback(true)
                         .build());
    } else {
      phaseSteps.add(aPhaseStep(CONTAINER_DEPLOY, Constants.DEPLOY_CONTAINERS)
                         .addStep(GraphNode.builder()
                                      .id(generateUuid())
                                      .type(ECS_SERVICE_ROLLBACK.name())
                                      .name(Constants.ROLLBACK_CONTAINERS)
                                      .rollback(true)
                                      .build())
                         .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                         .withStatusForRollback(SUCCESS)
                         .withRollback(true)
                         .build());
    }

    // Verification
    phaseSteps.add(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                       .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                       .withStatusForRollback(SUCCESS)
                       .withRollback(true)
                       .build());
    phaseSteps.add(aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build());

    return rollbackWorkflow(workflowPhase).phaseSteps(phaseSteps).build();
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForEcsBlueGreenRoute53(
      String appId, WorkflowPhase workflowPhase, OrchestrationWorkflowType orchestrationWorkflowType) {
    return rollbackWorkflow(workflowPhase)
        .phaseSteps(asList(aPhaseStep(ECS_UPDATE_ROUTE_53_DNS_WEIGHT, ECS_ROUTE53_DNS_WEIGHTS)
                               .addStep(GraphNode.builder()
                                            .id(generateUuid())
                                            .type(StateType.ECS_ROUTE53_DNS_WEIGHT_UPDATE_ROLLBACK.name())
                                            .name(ROLLBACK_ECS_ROUTE53_DNS_WEIGHTS)
                                            .rollback(true)
                                            .build())
                               .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                               .withStatusForRollback(SUCCESS)
                               .withRollback(true)
                               .build(),
            aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                .withPhaseStepNameForRollback(Constants.VERIFY_SERVICE)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build()))
        .build();
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForEcsBlueGreen(
      String appId, WorkflowPhase workflowPhase, OrchestrationWorkflowType orchestrationWorkflowType) {
    return rollbackWorkflow(workflowPhase)
        .phaseSteps(asList(aPhaseStep(ECS_UPDATE_LISTENER_BG, ECS_SWAP_TARGET_GROUPS)
                               .addStep(GraphNode.builder()
                                            .id(generateUuid())
                                            .type(StateType.ECS_LISTENER_UPDATE_ROLLBACK.name())
                                            .name(ECS_SWAP_TARGET_GROUPS_ROLLBACK)
                                            .rollback(true)
                                            .build())
                               .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                               .withStatusForRollback(SUCCESS)
                               .withRollback(true)
                               .build(),

            aPhaseStep(CONTAINER_DEPLOY, Constants.DEPLOY_CONTAINERS)
                .addStep(GraphNode.builder()
                             .id(generateUuid())
                             .type(ECS_SERVICE_ROLLBACK.name())
                             .name(Constants.ROLLBACK_CONTAINERS)
                             .rollback(true)
                             .build())
                .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            // Verification
            aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build()))
        .build();
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForAwsCodeDeploy(WorkflowPhase workflowPhase) {
    return rollbackWorkflow(workflowPhase)
        .phaseSteps(asList(aPhaseStep(DEPLOY_AWSCODEDEPLOY, Constants.DEPLOY_SERVICE)
                               .addStep(GraphNode.builder()
                                            .id(generateUuid())
                                            .type(AWS_CODEDEPLOY_ROLLBACK.name())
                                            .name(Constants.ROLLBACK_AWS_CODE_DEPLOY)
                                            .rollback(true)
                                            .build())
                               .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                               .withStatusForRollback(SUCCESS)
                               .withRollback(true)
                               .build(),
            // When we rolling back the verification steps the same criterie to run if deployment is needed should be
            // used
            aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build()))
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
      disableServiceSteps.add(GraphNode.builder()
                                  .type(ELASTIC_LOAD_BALANCER.name())
                                  .name("Elastic Load Balancer")
                                  .properties(ImmutableMap.<String, Object>builder().put("operation", Disable).build())
                                  .rollback(true)
                                  .build());
      enableServiceSteps.add(GraphNode.builder()
                                 .type(ELASTIC_LOAD_BALANCER.name())
                                 .name("Elastic Load Balancer")
                                 .properties(ImmutableMap.<String, Object>builder().put("operation", Enable).build())
                                 .rollback(true)
                                 .build());
    }

    return rollbackWorkflow(workflowPhase)
        .phaseSteps(asList(aPhaseStep(DISABLE_SERVICE, Constants.DISABLE_SERVICE)
                               .addAllSteps(disableServiceSteps)
                               .withPhaseStepNameForRollback(Constants.ENABLE_SERVICE)
                               .withStatusForRollback(SUCCESS)
                               .withRollback(true)
                               .build(),
            aPhaseStep(STOP_SERVICE, Constants.STOP_SERVICE)
                .addAllSteps(commandNodes(commandMap, CommandType.STOP, true))
                .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(DEPLOY_SERVICE, Constants.DEPLOY_SERVICE)
                .addAllSteps(commandNodes(commandMap, CommandType.INSTALL, true))
                .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(ENABLE_SERVICE, Constants.ENABLE_SERVICE)
                .addAllSteps(enableServiceSteps)
                .withPhaseStepNameForRollback(Constants.DISABLE_SERVICE)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            // When we rolling back the verification steps
            // the same criteria to run if deployment is needed should be used
            aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                .addAllSteps(commandNodes(commandMap, CommandType.VERIFY, true))
                .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build()))
        .build();
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForKubernetes(
      WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    if (workflowPhase.isDaemonSet() || workflowPhase.isStatefulSet()) {
      return generateRollbackSetupWorkflowPhase(workflowPhase);
    }

    List<PhaseStep> phaseSteps = new ArrayList<>();

    phaseSteps.add(aPhaseStep(CONTAINER_DEPLOY, Constants.DEPLOY_CONTAINERS)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(KUBERNETES_DEPLOY_ROLLBACK.name())
                                    .name(Constants.ROLLBACK_CONTAINERS)
                                    .rollback(true)
                                    .build())
                       .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                       .withStatusForRollback(SUCCESS)
                       .withRollback(true)
                       .build());
    if (serviceSetupRequired) {
      phaseSteps.add(aPhaseStep(CONTAINER_SETUP, Constants.SETUP_CONTAINER)
                         .addStep(GraphNode.builder()
                                      .id(generateUuid())
                                      .type(KUBERNETES_SETUP_ROLLBACK.name())
                                      .name(Constants.ROLLBACK_KUBERNETES_SETUP)
                                      .rollback(true)
                                      .build())
                         .withPhaseStepNameForRollback(Constants.SETUP_CONTAINER)
                         .withStatusForRollback(SUCCESS)
                         .withRollback(true)
                         .build());
    }

    // When we rolling back the verification steps the same criterie to run if deployment is needed should be used
    phaseSteps.add(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                       .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                       .withStatusForRollback(SUCCESS)
                       .withRollback(true)
                       .build());
    phaseSteps.add(aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build());

    return rollbackWorkflow(workflowPhase).phaseSteps(phaseSteps).build();
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForKubernetesBlueGreen(
      WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    if (workflowPhase.isDaemonSet() || workflowPhase.isStatefulSet()) {
      throw new InvalidRequestException("DaemonSet and StatefulSet are not supported with Blue/Green Deployment", USER);
    }

    Map<String, Object> defaultRouteUpdateProperties = new HashMap<>();
    defaultRouteUpdateProperties.put("service1", PRIMARY_SERVICE_NAME_EXPRESSION);
    defaultRouteUpdateProperties.put("service2", STAGE_SERVICE_NAME_EXPRESSION);

    List<PhaseStep> phaseSteps = new ArrayList<>();

    phaseSteps.add(aPhaseStep(ROUTE_UPDATE, Constants.ROUTE_UPDATE)
                       .withPhaseStepNameForRollback(Constants.ROUTE_UPDATE)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(KUBERNETES_SWAP_SERVICE_SELECTORS.name())
                                    .name(Constants.KUBERNETES_SWAP_SERVICES_PRIMARY_STAGE)
                                    .properties(defaultRouteUpdateProperties)
                                    .build())
                       .withRollback(true)
                       .build());
    phaseSteps.add(aPhaseStep(CONTAINER_DEPLOY, Constants.DEPLOY_CONTAINERS)
                       .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                       .withStatusForRollback(SUCCESS)
                       .withRollback(true)
                       .build());
    if (serviceSetupRequired) {
      phaseSteps.add(aPhaseStep(CONTAINER_SETUP, Constants.SETUP_CONTAINER)
                         .withPhaseStepNameForRollback(Constants.SETUP_CONTAINER)
                         .withStatusForRollback(SUCCESS)
                         .withRollback(true)
                         .build());
    }

    // When we rolling back the verification steps the same criterie to run if deployment is needed should be used
    phaseSteps.add(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                       .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                       .withStatusForRollback(SUCCESS)
                       .withRollback(true)
                       .build());
    phaseSteps.add(aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build());
    return rollbackWorkflow(workflowPhase).phaseSteps(phaseSteps).build();
  }

  public WorkflowPhase generateRollbackSetupWorkflowPhase(WorkflowPhase workflowPhase) {
    return rollbackWorkflow(workflowPhase)
        .daemonSet(workflowPhase.isDaemonSet())
        .statefulSet(workflowPhase.isStatefulSet())
        .phaseSteps(asList(aPhaseStep(CONTAINER_SETUP, Constants.SETUP_CONTAINER)
                               .addStep(GraphNode.builder()
                                            .id(generateUuid())
                                            .type(KUBERNETES_SETUP_ROLLBACK.name())
                                            .name(Constants.ROLLBACK_KUBERNETES_SETUP)
                                            .rollback(true)
                                            .build())
                               .withPhaseStepNameForRollback(Constants.SETUP_CONTAINER)
                               .withStatusForRollback(SUCCESS)
                               .withRollback(true)
                               .build(),
            // When we rolling back the verification steps the same criterie to run if deployment is needed should be
            // used
            aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                .withPhaseStepNameForRollback(Constants.SETUP_CONTAINER)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build()))
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
      nodes.add(GraphNode.builder()
                    .id(generateUuid())
                    .type(COMMAND.name())
                    .name(command.getName())
                    .properties(ImmutableMap.<String, Object>builder().put("commandName", command.getName()).build())
                    .rollback(rollback)
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

        DeploymentType deploymentType =
            serviceResourceService.getDeploymentType(infrastructureMapping, null, infrastructureMapping.getServiceId());
        phase.setDeploymentType(deploymentType);
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

  public boolean isExecutionForK8sV2Service(WorkflowExecution workflowExecution) {
    if (isNotEmpty(workflowExecution.getServiceIds())) {
      return isK8sV2Service(workflowExecution.getAppId(), workflowExecution.getServiceIds().get(0));
    }
    return false;
  }

  public boolean isOrchestrationWorkflowForK8sV2Service(
      String appId, CanaryOrchestrationWorkflow orchestrationWorkflow) {
    if (isNotEmpty(orchestrationWorkflow.getServiceIds())) {
      return isK8sV2Service(appId, orchestrationWorkflow.getServiceIds().get(0));
    }
    return false;
  }

  public boolean isK8sV2Service(String appId, String serviceId) {
    Service service = serviceResourceService.get(appId, serviceId);
    return service != null && service.isK8sV2();
  }

  public static Map<String, String> overrideWorkflowVariables(
      List<Variable> variables, Map<String, String> workflowStepVariables, Map<String, String> pipelineVariables) {
    Map<String, String> resolvedWorkflowVariables = new LinkedHashMap<>();
    if (isEmpty(variables)) {
      return new HashMap<>();
    }
    if (isEmpty(pipelineVariables)) {
      // No need to override the workflow variables return workflow variables
      return workflowStepVariables;
    }
    final Set<String> workflowVariableNames = variables.stream().map(Variable::getName).collect(Collectors.toSet());
    if (isNotEmpty(workflowStepVariables)) {
      for (Map.Entry<String, String> workflowVariableEntry : workflowStepVariables.entrySet()) {
        String workflowVariableName = workflowVariableEntry.getKey();
        if (workflowVariableNames.contains(workflowVariableName)) {
          String workflowVariableValue = workflowVariableEntry.getValue();
          String pipelineVariableName = ExpressionEvaluator.getName(workflowVariableValue);
          if (pipelineVariables.containsKey(pipelineVariableName)) {
            // Pipeline variable exists so that takes highest precedence
            resolvedWorkflowVariables.put(workflowVariableName, pipelineVariables.get(pipelineVariableName));
          } else if (pipelineVariables.containsKey(workflowVariableName)) {
            /// Pipeline variable overrides workflow variable
            resolvedWorkflowVariables.put(workflowVariableName, pipelineVariables.get(workflowVariableName));
          } else {
            // Use workflow variable as is
            resolvedWorkflowVariables.put(workflowVariableName, workflowVariableValue);
          }
        }
      }
    }

    // Add all missing workflow variables from pipeline variables
    for (String variableName : workflowVariableNames) {
      if (!resolvedWorkflowVariables.containsKey(variableName)) {
        // Check for pipeline variables
        if (pipelineVariables.containsKey(variableName)) {
          resolvedWorkflowVariables.put(variableName, pipelineVariables.get(variableName));
        }
      }
    }
    return resolvedWorkflowVariables;
  }
}

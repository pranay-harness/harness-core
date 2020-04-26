package software.wings.service.impl.workflow;

import static com.google.common.collect.Maps.newHashMap;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.beans.OrchestrationWorkflowType.BASIC;
import static io.harness.beans.OrchestrationWorkflowType.BLUE_GREEN;
import static io.harness.beans.OrchestrationWorkflowType.CANARY;
import static io.harness.beans.OrchestrationWorkflowType.MULTI_SERVICE;
import static io.harness.beans.OrchestrationWorkflowType.ROLLING;
import static io.harness.data.structure.CollectionUtils.trimmedLowercaseSet;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.api.DeploymentType.AMI;
import static software.wings.api.DeploymentType.AWS_CODEDEPLOY;
import static software.wings.api.DeploymentType.ECS;
import static software.wings.api.DeploymentType.HELM;
import static software.wings.api.DeploymentType.KUBERNETES;
import static software.wings.api.DeploymentType.PCF;
import static software.wings.beans.EntityType.INFRASTRUCTURE_DEFINITION;
import static software.wings.beans.EntityType.INFRASTRUCTURE_MAPPING;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.FeatureName.AWS_TRAFFIC_SHIFT;
import static software.wings.beans.InfrastructureMappingType.AWS_SSH;
import static software.wings.beans.InfrastructureMappingType.PCF_PCF;
import static software.wings.beans.InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH;
import static software.wings.beans.PhaseStep.PhaseStepBuilder;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.AMI_AUTOSCALING_GROUP_SETUP;
import static software.wings.beans.PhaseStepType.AMI_DEPLOY_AUTOSCALING_GROUP;
import static software.wings.beans.PhaseStepType.AMI_SWITCH_AUTOSCALING_GROUP_ROUTES;
import static software.wings.beans.PhaseStepType.CLUSTER_SETUP;
import static software.wings.beans.PhaseStepType.COLLECT_ARTIFACT;
import static software.wings.beans.PhaseStepType.CONTAINER_DEPLOY;
import static software.wings.beans.PhaseStepType.CONTAINER_SETUP;
import static software.wings.beans.PhaseStepType.DEPLOY_AWSCODEDEPLOY;
import static software.wings.beans.PhaseStepType.DEPLOY_AWS_LAMBDA;
import static software.wings.beans.PhaseStepType.ECS_UPDATE_LISTENER_BG;
import static software.wings.beans.PhaseStepType.ECS_UPDATE_ROUTE_53_DNS_WEIGHT;
import static software.wings.beans.PhaseStepType.INFRASTRUCTURE_NODE;
import static software.wings.beans.PhaseStepType.K8S_PHASE_STEP;
import static software.wings.beans.PhaseStepType.PCF_SWICH_ROUTES;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.settings.SettingValue.SettingVariableTypes.PHYSICAL_DATA_CENTER;
import static software.wings.sm.StateType.ARTIFACT_CHECK;
import static software.wings.sm.StateType.ARTIFACT_COLLECTION;
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
import static software.wings.sm.StateType.ECS_LISTENER_UPDATE;
import static software.wings.sm.StateType.ECS_ROUTE53_DNS_WEIGHT_UPDATE;
import static software.wings.sm.StateType.ECS_SERVICE_DEPLOY;
import static software.wings.sm.StateType.ECS_SERVICE_ROLLBACK;
import static software.wings.sm.StateType.ECS_SERVICE_SETUP_ROLLBACK;
import static software.wings.sm.StateType.ELASTIC_LOAD_BALANCER;
import static software.wings.sm.StateType.GCP_CLUSTER_SETUP;
import static software.wings.sm.StateType.KUBERNETES_DEPLOY;
import static software.wings.sm.StateType.KUBERNETES_DEPLOY_ROLLBACK;
import static software.wings.sm.StateType.KUBERNETES_SETUP;
import static software.wings.sm.StateType.KUBERNETES_SETUP_ROLLBACK;
import static software.wings.sm.StateType.KUBERNETES_SWAP_SERVICE_SELECTORS;
import static software.wings.sm.StateType.ROLLING_NODE_SELECT;
import static software.wings.sm.states.ElasticLoadBalancerState.Operation.Disable;
import static software.wings.sm.states.ElasticLoadBalancerState.Operation.Enable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscaler;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.expression.ExpressionEvaluator;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.DeploymentType;
import software.wings.beans.AmiDeploymentType;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.FeatureName;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.GraphNode;
import software.wings.beans.GraphNode.GraphNodeBuilder;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.PhysicalInfrastructureMappingBase;
import software.wings.beans.Service;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowCreationFlags;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.beans.workflow.StepSkipStrategy;
import software.wings.infra.AwsAmiInfrastructure;
import software.wings.infra.AwsInstanceInfrastructure;
import software.wings.infra.CloudProviderInfrastructure;
import software.wings.infra.GoogleKubernetesEngine;
import software.wings.infra.InfraMappingInfrastructureProvider;
import software.wings.infra.InfrastructureDefinition;
import software.wings.infra.PhysicalInfra;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureDefinitionService;
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
@Slf4j
public class WorkflowServiceHelper {
  public static final String RUNTIME = "RUNTIME";
  public static final String DISABLE_SERVICE = "Disable Service";
  public static final String ENABLE_SERVICE = "Enable Service";
  public static final String DEPLOY_SERVICE = "Deploy Service";
  public static final String ROLLBACK_SERVICE = "Rollback Service";
  public static final String STOP_SERVICE = "Stop Service";
  public static final String VERIFY_SERVICE = "Verify Service";
  public static final String VERIFY_STAGING = "Verify Staging";
  public static final String DEPLOY_CONTAINERS = "Deploy Containers";
  public static final String SETUP_CONTAINER = "Set up Container";
  public static final String SETUP_CLUSTER = "Setup Cluster";
  public static final String ECS_SERVICE_SETUP = "ECS Service Setup";
  public static final String ECS_DAEMON_SERVICE_SETUP = "ECS Daemon Service Setup";
  public static final String ECS_BG_SERVICE_SETUP_ELB = "Setup Load Balancer";
  public static final String ECS_BG_SERVICE_SETUP_ROUTE_53 = "Setup Route 53";
  public static final String ECS_DAEMON_SCHEDULING_STRATEGY = "DAEMON";
  public static final String CHANGE_ROUTE53_DNS_WEIGHTS = "Change Route 53 Weights";
  public static final String ECS_ROUTE53_DNS_WEIGHTS = "Swap Route 53 DNS";
  public static final String ECS_SWAP_TARGET_GROUPS = "Swap Target Groups";
  public static final String ECS_SWAP_TARGET_GROUPS_ROLLBACK = "Rollback Swap Target Groups";
  public static final String PRIMARY_SERVICE_NAME_EXPRESSION = "${PRIMARY_SERVICE_NAME}";
  public static final String ROLLBACK_AUTOSCALING_GROUP_ROUTE = "Rollback AutoScaling Group Route";
  public static final String ROLLBACK_ECS_ROUTE53_DNS_WEIGHTS = "Rollback Route 53 Weights";
  public static final String STAGE_SERVICE_NAME_EXPRESSION = "${STAGE_SERVICE_NAME}";
  public static final String SWAP_AUTOSCALING_GROUP_ROUTE = "Swap Routes";
  public static final String UPGRADE_AUTOSCALING_GROUP_ROUTE = "Switch AutoScaling Group Route";
  public static final String AWS_CODE_DEPLOY = "AWS CodeDeploy";
  public static final String UPGRADE_AUTOSCALING_GROUP = "Upgrade AutoScaling Group";
  public static final String WRAP_UP = "Wrap Up";
  public static final String PREPARE_STEPS = "Prepare Steps";
  public static final String UPGRADE_CONTAINERS = "Upgrade Containers";
  public static final String AWS_LAMBDA = "AWS Lambda";
  public static final String ROLLBACK_PREFIX = "Rollback ";
  public static final String PCF_BG_MAP_ROUTE = "Update Route";
  public static final String PCF_BG_SWAP_ROUTE = "Swap Routes";
  public static final String DEPLOY = "Deploy";
  public static final String PCF_ROLLBACK = "App Rollback";
  public static final String HELM_ROLLBACK = "Helm Rollback";
  public static final String ROLLBACK_AWS_AMI_CLUSTER = "Rollback AutoScaling Group";
  public static final String PCF_RESIZE = "App Resize";
  public static final String HELM_DEPLOY = "Helm Deploy";
  public static final String KUBERNETES_SERVICE_SETUP = "Kubernetes Service Setup";
  public static final String INFRA_TEMP_ROUTE_PCF = "infra.pcf.tempRoute";
  public static final String SETUP = "Setup";
  public static final String PCF_SETUP = "App Setup";
  public static final String PCF_PLUGIN = "CF Command";
  public static final String SPOTINST_SETUP = "Elastigroup Setup";
  public static final String SPOTINST_ALB_SHIFT_SETUP = "Elastigroup Alb Shift Setup";
  public static final String SPOTINST_DEPLOY = "Elastigroup Deploy";
  public static final String SPOTINST_ROLLBACK = "Elastigroup Rollback";
  public static final String SPOTINST_LISTENER_UPDATE_ROLLBACK = "Route Update Rollback";
  public static final String SPOTINST_SWAP_ROLLBACK = "Swap Production with Stage";
  public static final String SPOTINST_LISTENER_UPDATE = "Route Update";
  public static final String SPOTINST_SWAP = "Swap Production with Stage";
  public static final String KUBERNETES_SERVICE_SETUP_BLUEGREEN = "Blue/Green Service Setup";
  public static final String INFRA_ROUTE_PCF = "infra.pcf.route";
  public static final String VERIFY_STAGE_SERVICE = "Verify Stage Service";
  public static final String ROUTE_UPDATE = "Route Update";
  public static final String KUBERNETES_SWAP_SERVICES_PRIMARY_STAGE = "Swap Primary with Stage";
  public static final String INFRASTRUCTURE_NODE_NAME = "Prepare Infra";
  public static final String SELECT_NODE_NAME = "Select Nodes";
  public static final String ROLLBACK_AWS_LAMBDA = "Rollback AWS Lambda";
  public static final String ROLLBACK_CONTAINERS = "Rollback Containers";
  public static final String ROLLBACK_AWS_CODE_DEPLOY = "Rollback AWS CodeDeploy";
  public static final String ROLLBACK_KUBERNETES_SETUP = "Rollback Kubernetes Setup";
  public static final String JIRA = "Jira";
  public static final String ARTIFACT_COLLECTION_STEP = "Artifact Collection";
  public static final String ARTIFACT_CHECK_STEP = "Artifact Check";
  public static final String APPDYNAMICS = "AppDynamics";
  public static final String NEW_RELIC = "New Relic";
  public static final String INSTANA = "Instana";
  public static final String DYNATRACE = "Dynatrace";
  public static final String PROMETHEUS = "Prometheus";
  public static final String DATADOG_METRICS = "Datadog Metrics";
  public static final String STACKDRIVER = "Stackdriver";
  public static final String CLOUDWATCH = "CloudWatch";
  public static final String SCALYR = "Scalyr";
  public static final String BUG_SNAG = "Bugsnag";
  public static final String CUSTOM_METRICS = "Custom Metrics";
  public static final String DATADOG_LOG = "Datadog Log";
  public static final String ELK = "ELK";
  public static final String SPLUNK_V2 = "Splunkv2";
  public static final String STACKDRIVER_LOG = "Stackdriver Log";
  public static final String SUMO_LOGIC = "Sumo Logic";
  public static final String LOGZ = "LOGZ";
  public static final String CUSTOM_LOG_VERIFICATION = "Custom Log Verification";
  public static final String AWS_SELECT_NODES = "AWS Select Nodes";
  public static final String ELB = "Elastic Load Balancer";
  public static final String AZURE_SELECT_NODES = "Azure Select Nodes";
  public static final String ROLLBACK_ECS_SETUP = "Rollback ECS Setup";
  public static final String PCF_MAP_ROUTE_NAME = "Map Route";
  public static final String PCF_UNMAP_ROUTE_NAME = "Unmap Route";
  public static final String PROVISION_SHELL_SCRIPT = "Shell Script Provision";
  public static final String ROLLBACK_CLOUD_FORMATION = "CloudFormation Rollback Stack";
  public static final String ROLLBACK_TERRAFORM_NAME = "Terraform Rollback";
  public static final String ECS_STEADY_STATE_CHK = "ECS Steady State Check";
  public static final String KUBERNETES_STEADY_STATE_CHECK = "Steady State Check";
  public static final String ECS_UPGRADE_CONTAINERS = "ECS Upgrade Containers";
  public static final String ECS_ROLLBACK_CONTAINERS = "ECS Rollback Containers";
  public static final String KUBERNETES_UPGRADE_CONTAINERS = "Kubernetes Upgrade Containers";
  public static final String KUBERNETES_ROLLBACK_CONTAINERS = "Kubernetes Rollback Containers";
  public static final String GCP_CLUSTER_SETUP_NAME = "GCP Cluster Setup";
  public static final String SWAP_SERVICE_SELECTORS = "Swap Service Selectors";
  public static final String ROLLING_SELECT_NODES = "Rolling Select Nodes";
  public static final String CF_CREATE_STACK = "CloudFormation Create Stack";
  public static final String CF_DELETE_STACK = "CloudFormation Delete Stack";
  public static final String TERRAFORM_APPLY = "Terraform Apply";
  public static final String TERRAFORM_PROVISION = "Terraform Provision";
  public static final String TERRAFORM_DESTROY = "Terraform Destroy";
  public static final String SERVICENOW = "ServiceNow";
  public static final String EMAIL = "Email";
  public static final String BARRIER = "Barrier";
  public static final String RESOURCE_CONSTRAINT = "Resource Constraint";
  public static final String APPROVAL_NAME = "Approval";
  public static final String JENKINS = "Jenkins";
  public static final String BAMBOO = "Bamboo";
  public static final String SHELL_SCRIPT = "Shell Script";
  public static final String HTTP = "HTTP";
  public static final String NEW_RELIC_DEPLOYMENT_MARKER = "New Relic Deployment Marker";
  public static final String COMMAND_NAME = "Command";

  private static final String COLLECT_ARTIFACT_PHASE_STEP_NAME = "Collect Artifact";
  private static final String SETUP_AUTOSCALING_GROUP = "Setup AutoScaling Group";
  private static final String PROVISION_INFRASTRUCTURE = "Provision Infrastructure";
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
  @Inject private InfrastructureDefinitionService infrastructureDefinitionService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private ExpressionEvaluator expressionEvaluator;
  @Inject private AppService appService;
  @Inject private FeatureFlagService featureFlagService;

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
      throw new InvalidRequestException("Unable to generate Yaml String for Horizontal pod autoscalar", USER);
    }
  }

  public boolean workflowHasSshDeploymentPhase(CanaryOrchestrationWorkflow canaryOrchestrationWorkflow) {
    List<WorkflowPhase> workflowPhases = canaryOrchestrationWorkflow.getWorkflowPhases();
    if (isNotEmpty(workflowPhases)) {
      return workflowPhases.stream().anyMatch(workflowPhase -> DeploymentType.SSH == workflowPhase.getDeploymentType());
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

  public boolean needArtifactCheckStep(
      String appId, CanaryOrchestrationWorkflow canaryOrchestrationWorkflow, boolean infraRefactor) {
    List<WorkflowPhase> workflowPhases = canaryOrchestrationWorkflow.getWorkflowPhases();
    if (isNotEmpty(canaryOrchestrationWorkflow.getWorkflowPhases())) {
      if (infraRefactor) {
        List<String> infraDefinitionIds = workflowPhases.stream()
                                              .filter(workflowPhase -> workflowPhase.getInfraDefinitionId() != null)
                                              .map(WorkflowPhase::getInfraDefinitionId)
                                              .collect(toList());

        return infrastructureDefinitionService.getInfraStructureDefinitionByUuids(appId, infraDefinitionIds)
            .stream()
            .filter(infrastructureDefinition -> infrastructureDefinition.getInfrastructure() != null)
            .anyMatch((InfrastructureDefinition infra)
                          -> AWS_SSH.name().equals(infra.getInfrastructure().getInfrastructureType())
                    || PHYSICAL_DATA_CENTER_SSH.name().equals(infra.getInfrastructure().getInfrastructureType())
                    || PCF_PCF.name().equals(infra.getInfrastructure().getInfrastructureType()));
      } else {
        List<String> infraMappingIds = workflowPhases.stream()
                                           .filter(workflowPhase -> workflowPhase.getInfraMappingId() != null)
                                           .map(WorkflowPhase::getInfraMappingId)
                                           .collect(toList());

        if (isNotEmpty(infraMappingIds)) {
          return infrastructureMappingService.getInfraStructureMappingsByUuids(appId, infraMappingIds)
              .stream()
              .anyMatch((InfrastructureMapping infra)
                            -> AWS_SSH.name().equals(infra.getInfraMappingType())
                      || PHYSICAL_DATA_CENTER_SSH.name().equals(infra.getInfraMappingType())
                      || PCF_PCF.name().equals(infra.getInfraMappingType()));
        }
      }
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

  public String obtainEnvIdWithoutOrchestration(Workflow workflow, Map<String, String> workflowVariables) {
    final String envTemplatizedName = workflow.fetchEnvTemplatizedName();
    if (isEmpty(envTemplatizedName)) {
      return workflow.getEnvId();
    }
    if (isEmpty(workflowVariables)) {
      return null;
    }
    return workflowVariables.get(envTemplatizedName);
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
    throw new InvalidRequestException(
        "Workflow [" + workflow.getName() + "] environment parameterized. However, the value not supplied", USER);
  }

  public Set<String> getKeywords(Workflow workflow) {
    Set<String> keywords = workflow.generateKeywords();
    if (workflow.getEnvId() != null) {
      Environment environment = environmentService.get(workflow.getAppId(), workflow.getEnvId());
      if (environment != null) {
        keywords.add(environment.getName());
      }
    }
    return trimmedLowercaseSet(keywords);
  }

  public void setKeywords(Workflow workflow) {
    workflow.setDefaultVersion(1);
    List<String> keywords = new ArrayList<>(asList(workflow.getName(), workflow.getDescription(), workflow.getNotes()));
    if (workflow.getWorkflowType() != null) {
      keywords.add(workflow.getWorkflowType().name());
    }
    workflow.setKeywords(trimmedLowercaseSet(keywords));
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
      if (oldService.getArtifactType() != null && oldService.getArtifactType() != newService.getArtifactType()) {
        throw new InvalidRequestException("Target service  [" + oldService.getName()
                + " ] is not compatible with service [" + newService.getName() + "]",
            USER);
      }
    }
  }

  public void validateServiceAndInfraMapping(String appId, String serviceId, String infraMappingId) {
    // Validate if service Id is valid or not
    if (serviceId == null || infraMappingId == null) {
      return;
    }
    Service service = serviceResourceService.get(appId, serviceId, false);
    if (service == null) {
      throw new InvalidRequestException("Service [" + serviceId + "] does not exist", USER);
    }
    InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, infraMappingId);
    if (infrastructureMapping == null) {
      throw new InvalidRequestException("Service Infrastructure [" + infraMappingId + "] does not exist", USER);
    }
    if (!service.getUuid().equals(infrastructureMapping.getServiceId())) {
      throw new InvalidRequestException("Service Infrastructure [" + infrastructureMapping.getName()
              + "] not mapped to Service [" + service.getName() + "]",
          USER);
    }
  }

  public void validateServiceAndInfraDefinition(String appId, String serviceId, String infraDefinitionId) {
    if (serviceId == null || infraDefinitionId == null) {
      return;
    }
    Service service = serviceResourceService.get(appId, serviceId, false);
    if (service == null) {
      throw new InvalidRequestException("Service [" + serviceId + "] does not exist", USER);
    }
    InfrastructureDefinition infrastructureDefinition = infrastructureDefinitionService.get(appId, infraDefinitionId);
    if (infrastructureDefinition == null) {
      throw new InvalidRequestException("Service Infrastructure [" + infraDefinitionId + "] does not exist", USER);
    }

    // Ignoring validation for old Services where no deployment type is present
    if (service.getDeploymentType() != null) {
      if (service.getDeploymentType() != infrastructureDefinition.getDeploymentType()) {
        throw new InvalidRequestException(
            "Service [" + serviceId + "] Infrastructure Definition[" + infraDefinitionId + "] are not compatible",
            USER);
      }
      List<String> scopedServices = infrastructureDefinition.getScopedToServices();
      if (EmptyPredicate.isNotEmpty(scopedServices)) {
        if (!scopedServices.contains(serviceId)) {
          throw new InvalidRequestException(
              "Service [" + serviceId + "] Infrastructure Definition[" + infraDefinitionId + "] are not compatible",
              USER);
        }
      }
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

  public void setCloudProviderInfraRefactor(String appId, WorkflowPhase workflowPhase) {
    if (workflowPhase.checkInfraDefinitionTemplatized()) {
      return;
    }

    if (workflowPhase.getInfraDefinitionId() != null) {
      InfrastructureDefinition infrastructureDefinition =
          infrastructureDefinitionService.get(appId, workflowPhase.getInfraDefinitionId());
      workflowPhase.setComputeProviderId(infrastructureDefinition.getInfrastructure().getCloudProviderId());
      workflowPhase.setDeploymentType(infrastructureDefinition.getDeploymentType());
    } else {
      setCloudProvider(appId, workflowPhase);
    }
  }

  public void generateNewWorkflowPhaseStepsForSpotinst(
      String appId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();
    if (serviceSetupRequired) {
      InfrastructureMapping infraMapping;
      boolean spotInstInfra;
      if (featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, appService.getAccountIdByAppId(appId))) {
        InfrastructureDefinition infrastructureDefinition =
            infrastructureDefinitionService.get(appId, workflowPhase.getInfraDefinitionId());
        spotInstInfra = infrastructureDefinition.getInfrastructure() instanceof AwsAmiInfrastructure;
      } else {
        infraMapping = infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
        spotInstInfra = infraMapping instanceof AwsAmiInfrastructureMapping;
      }
      if (spotInstInfra) {
        Map<String, Object> defaultData = newHashMap();
        defaultData.put("blueGreen", false);
        phaseSteps.add(aPhaseStep(PhaseStepType.SPOTINST_SETUP, SPOTINST_SETUP)
                           .addStep(GraphNode.builder()
                                        .id(generateUuid())
                                        .type(StateType.SPOTINST_SETUP.name())
                                        .name(SPOTINST_SETUP)
                                        .properties(defaultData)
                                        .build())
                           .build());
      }
    }
    phaseSteps.add(aPhaseStep(PhaseStepType.SPOTINST_DEPLOY, SPOTINST_DEPLOY)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(StateType.SPOTINST_DEPLOY.name())
                                    .name(SPOTINST_DEPLOY)
                                    .build())
                       .build());

    Service service = serviceResourceService.getWithDetails(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);
    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_STAGING)
                       .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                       .build());

    phaseSteps.add(aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).build());
  }

  @VisibleForTesting
  void generateNewWorkflowPhaseStepsForSpotinstAlbTrafficShift(String appId, WorkflowPhase workflowPhase) {
    Service service = serviceResourceService.getWithDetails(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);
    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();
    phaseSteps.add(aPhaseStep(PhaseStepType.SPOTINST_SETUP, SPOTINST_ALB_SHIFT_SETUP)
                       .addStep(GraphNode.builder()
                                    .type(StateType.SPOTINST_ALB_SHIFT_SETUP.name())
                                    .name(SPOTINST_ALB_SHIFT_SETUP)
                                    .build())
                       .build());
    phaseSteps.add(
        aPhaseStep(PhaseStepType.SPOTINST_DEPLOY, SPOTINST_DEPLOY)
            .addStep(GraphNode.builder().type(StateType.SPOTINST_ALB_SHIFT_DEPLOY.name()).name(SPOTINST_DEPLOY).build())
            .build());
    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_STAGING)
                       .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                       .build());
    phaseSteps.add(
        aPhaseStep(PhaseStepType.SPOTINST_LISTENER_UPDATE, SPOTINST_LISTENER_UPDATE)
            .addStep(GraphNode.builder().type(StateType.SPOTINST_LISTENER_ALB_SHIFT.name()).name(SPOTINST_SWAP).build())
            .build());
    phaseSteps.add(aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).build());
  }

  public void generateNewWorkflowPhaseStepsForSpotInstBlueGreen(
      String appId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    Service service = serviceResourceService.getWithDetails(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);
    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();
    if (serviceSetupRequired) {
      InfrastructureMapping infraMapping;
      boolean spotInstInfra;
      if (featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, appService.getAccountIdByAppId(appId))) {
        InfrastructureDefinition infrastructureDefinition =
            infrastructureDefinitionService.get(appId, workflowPhase.getInfraDefinitionId());
        spotInstInfra = infrastructureDefinition.getInfrastructure() instanceof AwsAmiInfrastructure;
      } else {
        infraMapping = infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
        spotInstInfra = infraMapping instanceof AwsAmiInfrastructureMapping;
      }
      if (spotInstInfra) {
        Map<String, Object> defaultData = newHashMap();
        defaultData.put("blueGreen", true);
        phaseSteps.add(aPhaseStep(PhaseStepType.SPOTINST_SETUP, SPOTINST_SETUP)
                           .addStep(GraphNode.builder()
                                        .id(generateUuid())
                                        .type(StateType.SPOTINST_SETUP.name())
                                        .name(SPOTINST_SETUP)
                                        .properties(defaultData)
                                        .build())
                           .build());
      }
    }
    Map<String, Object> deployStateMap = newHashMap();
    deployStateMap.put("instanceUnitType", "PERCENTAGE");
    deployStateMap.put("instanceCount", 100);
    phaseSteps.add(aPhaseStep(PhaseStepType.SPOTINST_DEPLOY, SPOTINST_DEPLOY)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(StateType.SPOTINST_DEPLOY.name())
                                    .name(SPOTINST_DEPLOY)
                                    .properties(deployStateMap)
                                    .build())
                       .build());
    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_STAGING)
                       .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                       .build());
    Map<String, Object> defaultDataSwitchRoutes = newHashMap();
    defaultDataSwitchRoutes.put("downsizeOldElastiGroup", true);
    phaseSteps.add(aPhaseStep(PhaseStepType.SPOTINST_LISTENER_UPDATE, SPOTINST_LISTENER_UPDATE)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(StateType.SPOTINST_LISTENER_UPDATE.name())
                                    .name(SPOTINST_SWAP)
                                    .properties(defaultDataSwitchRoutes)
                                    .build())
                       .build());
    phaseSteps.add(aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).build());
  }

  public void generateNewWorkflowPhaseStepsForAWSAmiBlueGreen(
      String appId, WorkflowPhase workflowPhase, boolean serviceSetupRequired, boolean isDynamicInfrastructure) {
    Service service = serviceResourceService.getWithDetails(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

    if (isDynamicInfrastructure) {
      phaseSteps.add(aPhaseStep(PhaseStepType.PROVISION_INFRASTRUCTURE, PROVISION_INFRASTRUCTURE).build());
    }

    if (serviceSetupRequired) {
      InfrastructureMapping infraMapping;
      boolean awsAmiInfra;
      if (featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, appService.getAccountIdByAppId(appId))) {
        InfrastructureDefinition infrastructureDefinition =
            infrastructureDefinitionService.get(appId, workflowPhase.getInfraDefinitionId());
        awsAmiInfra = infrastructureDefinition.getInfrastructure() instanceof AwsAmiInfrastructure;
      } else {
        infraMapping = infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
        awsAmiInfra = infraMapping instanceof AwsAmiInfrastructureMapping;
      }
      if (awsAmiInfra) {
        Map<String, Object> defaultData = newHashMap();
        defaultData.put("maxInstances", 10);
        defaultData.put("autoScalingSteadyStateTimeout", 10);
        defaultData.put("blueGreen", true);
        phaseSteps.add(aPhaseStep(AMI_AUTOSCALING_GROUP_SETUP, SETUP_AUTOSCALING_GROUP)
                           .addStep(GraphNode.builder()
                                        .id(generateUuid())
                                        .type(AWS_AMI_SERVICE_SETUP.name())
                                        .name("AWS AutoScaling Group Setup")
                                        .properties(defaultData)
                                        .build())
                           .build());
      }
    }
    phaseSteps.add(aPhaseStep(AMI_DEPLOY_AUTOSCALING_GROUP, DEPLOY_SERVICE)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(AWS_AMI_SERVICE_DEPLOY.name())
                                    .name(UPGRADE_AUTOSCALING_GROUP)
                                    .build())
                       .build());

    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_STAGING)
                       .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                       .build());

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

    phaseSteps.add(aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).build());
  }

  public void generateNewWorkflowPhaseStepsForAWSAmi(String appId, WorkflowPhase workflowPhase,
      boolean serviceSetupRequired, boolean isDynamicInfrastructure,
      OrchestrationWorkflowType orchestrationWorkflowType) {
    Service service = serviceResourceService.getWithDetails(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);
    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

    if (isDynamicInfrastructure && BASIC == orchestrationWorkflowType) {
      phaseSteps.add(aPhaseStep(PhaseStepType.PROVISION_INFRASTRUCTURE, PROVISION_INFRASTRUCTURE).build());
    }

    if (serviceSetupRequired) {
      boolean isAwsAmiInfrastructure;
      if (featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, appService.getAccountIdByAppId(appId))) {
        InfrastructureDefinition infraDefinition =
            infrastructureDefinitionService.get(appId, workflowPhase.getInfraDefinitionId());
        isAwsAmiInfrastructure = infraDefinition.getInfrastructure() instanceof AwsAmiInfrastructure;
      } else {
        InfrastructureMapping infraMapping = infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
        isAwsAmiInfrastructure = infraMapping instanceof AwsAmiInfrastructureMapping;
      }
      if (isAwsAmiInfrastructure) {
        Map<String, Object> defaultData = new HashMap<>();
        defaultData.put("maxInstances", 10);
        defaultData.put("autoScalingSteadyStateTimeout", 10);
        defaultData.put("blueGreen", false);
        phaseSteps.add(aPhaseStep(AMI_AUTOSCALING_GROUP_SETUP, SETUP_AUTOSCALING_GROUP)
                           .addStep(GraphNode.builder()
                                        .id(generateUuid())
                                        .type(AWS_AMI_SERVICE_SETUP.name())
                                        .name("AWS AutoScaling Group Setup")
                                        .properties(defaultData)
                                        .build())
                           .build());
      }
    }
    phaseSteps.add(aPhaseStep(AMI_DEPLOY_AUTOSCALING_GROUP, DEPLOY_SERVICE)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(AWS_AMI_SERVICE_DEPLOY.name())
                                    .name(UPGRADE_AUTOSCALING_GROUP)
                                    .build())
                       .build());

    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                       .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                       .build());

    phaseSteps.add(aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).build());
  }

  public void generateNewWorkflowPhaseStepsForAWSLambda(String appId, WorkflowPhase workflowPhase) {
    Service service = serviceResourceService.getWithDetails(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

    phaseSteps.add(aPhaseStep(PhaseStepType.PREPARE_STEPS, PREPARE_STEPS).build());

    phaseSteps.add(
        aPhaseStep(DEPLOY_AWS_LAMBDA, DEPLOY_SERVICE)
            .addStep(GraphNode.builder().id(generateUuid()).type(AWS_LAMBDA_STATE.name()).name(AWS_LAMBDA).build())
            .build());

    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                       .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                       .build());

    phaseSteps.add(aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).build());
  }

  public void generateNewWorkflowPhaseStepsForAWSCodeDeploy(String appId, WorkflowPhase workflowPhase) {
    Service service = serviceResourceService.getWithDetails(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

    phaseSteps.add(aPhaseStep(PhaseStepType.PREPARE_STEPS, PREPARE_STEPS).build());

    Map<String, String> stateDefaults = getStateDefaults(appId, service.getUuid(), AWS_CODEDEPLOY_STATE);
    GraphNodeBuilder node =
        GraphNode.builder().id(generateUuid()).type(AWS_CODEDEPLOY_STATE.name()).name(AWS_CODE_DEPLOY);

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
    phaseSteps.add(aPhaseStep(DEPLOY_AWSCODEDEPLOY, DEPLOY_SERVICE).addStep(node.build()).build());

    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                       .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                       .build());

    phaseSteps.add(aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).build());
  }

  public Map<String, String> getStateDefaults(String appId, String serviceId, StateType stateType) {
    if (AWS_CODEDEPLOY_STATE == stateType) {
      List<ArtifactStream> artifactStreams = artifactStreamService.fetchArtifactStreamsForService(appId, serviceId);
      if (artifactStreams.stream().anyMatch(
              artifactStream -> ArtifactStreamType.AMAZON_S3.name().equals(artifactStream.getArtifactStreamType()))) {
        return AwsCodeDeployState.loadDefaults();
      }
    }
    return Collections.emptyMap();
  }

  public void generateNewWorkflowPhaseStepsForECS(String appId, WorkflowPhase workflowPhase,
      boolean serviceSetupRequired, OrchestrationWorkflowType orchestrationWorkflowType) {
    Service service = serviceResourceService.getWithDetails(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

    boolean isDaemonEcsWorkflow = isDaemonSchedulingStrategy(appId, workflowPhase, orchestrationWorkflowType);
    if (serviceSetupRequired) {
      if (isDaemonEcsWorkflow) {
        phaseSteps.add(aPhaseStep(CONTAINER_SETUP, SETUP_CONTAINER)
                           .addStep(GraphNode.builder()
                                        .id(generateUuid())
                                        .type(StateType.ECS_DAEMON_SERVICE_SETUP.name())
                                        .name(ECS_DAEMON_SERVICE_SETUP)
                                        .build())
                           .build());
      } else {
        phaseSteps.add(aPhaseStep(CONTAINER_SETUP, SETUP_CONTAINER)
                           .addStep(GraphNode.builder()
                                        .id(generateUuid())
                                        .type(StateType.ECS_SERVICE_SETUP.name())
                                        .name(ECS_SERVICE_SETUP)
                                        .build())
                           .build());
      }
    }

    if (!isDaemonEcsWorkflow) {
      phaseSteps.add(aPhaseStep(CONTAINER_DEPLOY, DEPLOY_CONTAINERS)
                         .addStep(GraphNode.builder()
                                      .id(generateUuid())
                                      .type(ECS_SERVICE_DEPLOY.name())
                                      .name(UPGRADE_CONTAINERS)
                                      .build())
                         .build());
    }

    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                       .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                       .build());

    phaseSteps.add(aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).build());
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
    phaseSteps.add(aPhaseStep(CONTAINER_DEPLOY, DEPLOY_CONTAINERS)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(ECS_SERVICE_DEPLOY.name())
                                    .name(UPGRADE_CONTAINERS)
                                    .properties(getDefaultDeployPropertyMapForEcsBG())
                                    .build())
                       .build());
    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                       .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                       .build());
  }

  public void generateNewWorkflowPhaseStepsForECSBlueGreenRoute53(
      String appId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    Service service = serviceResourceService.getWithDetails(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();
    if (serviceSetupRequired) {
      Map<String, Object> setupProperties = newHashMap();
      setupProperties.put("resizeStrategy", "RESIZE_NEW_FIRST");
      phaseSteps.add(aPhaseStep(CONTAINER_SETUP, SETUP_CONTAINER)
                         .addStep(GraphNode.builder()
                                      .id(generateUuid())
                                      .type(ECS_BG_SERVICE_SETUP_ROUTE53.name())
                                      .name(ECS_BG_SERVICE_SETUP_ROUTE_53)
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

    phaseSteps.add(aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).build());
  }

  public void generateNewWorkflowPhaseStepsForECSBlueGreen(
      String appId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    Service service = serviceResourceService.getWithDetails(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

    if (serviceSetupRequired) {
      Map<String, Object> defaultSetupProperties = newHashMap();
      defaultSetupProperties.put("resizeStrategy", "RESIZE_NEW_FIRST");
      defaultSetupProperties.put("useLoadBalancer", true);

      phaseSteps.add(aPhaseStep(CONTAINER_SETUP, SETUP_CONTAINER)
                         .addStep(GraphNode.builder()
                                      .id(generateUuid())
                                      .type(ECS_BG_SERVICE_SETUP.name())
                                      .name(ECS_BG_SERVICE_SETUP_ELB)
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

    phaseSteps.add(aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).build());
  }

  boolean isDaemonSchedulingStrategy(
      String appId, WorkflowPhase workflowPhase, OrchestrationWorkflowType orchestrationWorkflowType) {
    boolean isDaemonSchedulingStrategy = false;

    // DaemonSchedulingStrategy is only allowed for Basic workflow
    if (OrchestrationWorkflowType.BASIC == orchestrationWorkflowType) {
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
    WorkflowPhaseBuilder workflowPhaseBuilder = aWorkflowPhase()
                                                    .name(ROLLBACK_PREFIX + workflowPhase.getName())
                                                    .deploymentType(workflowPhase.getDeploymentType())
                                                    .rollback(true)
                                                    .phaseNameForRollback(workflowPhase.getName())
                                                    .serviceId(workflowPhase.getServiceId())
                                                    .computeProviderId(workflowPhase.getComputeProviderId())
                                                    .infraMappingId(workflowPhase.getInfraMappingId())
                                                    .infraMappingName(workflowPhase.getInfraMappingName());
    if (isNotBlank(workflowPhase.getInfraDefinitionId())) {
      workflowPhaseBuilder.infraDefinitionId(workflowPhase.getInfraDefinitionId());
    }
    return workflowPhaseBuilder;
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
        .phaseSteps(asList(aPhaseStep(PCF_SWICH_ROUTES, PCF_BG_MAP_ROUTE)
                               .addStep(GraphNode.builder()
                                            .id(generateUuid())
                                            .type(StateType.PCF_BG_MAP_ROUTE.name())
                                            .name(PCF_BG_SWAP_ROUTE)
                                            .properties(defaultRouteUpdateProperties)
                                            .rollback(true)
                                            .build())
                               .withPhaseStepNameForRollback(PCF_BG_MAP_ROUTE)
                               .withStatusForRollback(SUCCESS)
                               .withRollback(true)
                               .build(),

            // When we rolling back the verification steps
            // the same criteria to run if deployment is needed should be used
            aPhaseStep(PhaseStepType.PCF_RESIZE, DEPLOY)
                .addStep(GraphNode.builder()
                             .id(generateUuid())
                             .type(StateType.PCF_ROLLBACK.name())
                             .name(PCF_ROLLBACK)
                             .rollback(true)
                             .build())
                .withPhaseStepNameForRollback(DEPLOY)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                .withPhaseStepNameForRollback(PCF_RESIZE)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).withRollback(true).build()))
        .build();
  }

  public void generateNewWorkflowPhaseStepsForPCFBlueGreen(
      String appId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    if (workflowPhase.isDaemonSet() || workflowPhase.isStatefulSet()) {
      throw new InvalidRequestException("DaemonSet and StatefulSet are not supported with Blue/Green Deployment", USER);
    }

    Service service = serviceResourceService.getWithDetails(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

    // SETUP
    if (serviceSetupRequired) {
      Map<String, Object> defaultSetupProperties = new HashMap<>();
      defaultSetupProperties.put("blueGreen", true);
      defaultSetupProperties.put("isWorkflowV2", true);
      defaultSetupProperties.put("resizeStrategy", "RESIZE_NEW_FIRST");

      phaseSteps.add(aPhaseStep(PhaseStepType.PCF_SETUP, SETUP)
                         .addStep(GraphNode.builder()
                                      .id(generateUuid())
                                      .type(StateType.PCF_SETUP.name())
                                      .name(PCF_SETUP)
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

    phaseSteps.add(aPhaseStep(PhaseStepType.PCF_RESIZE, DEPLOY)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(StateType.PCF_RESIZE.name())
                                    .name(PCF_RESIZE)
                                    .properties(defaultUpgradeStageContainerProperties)
                                    .build())
                       .build());

    // Verify
    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_STAGING)
                       .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                       .build());

    // Swap Routes
    Map<String, Object> defaultRouteUpdateProperties = new HashMap<>();
    defaultRouteUpdateProperties.put("downsizeOldApps", false);
    phaseSteps.add(aPhaseStep(PCF_SWICH_ROUTES, PCF_BG_MAP_ROUTE)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(StateType.PCF_BG_MAP_ROUTE.name())
                                    .name(PCF_BG_SWAP_ROUTE)
                                    .properties(defaultRouteUpdateProperties)
                                    .build())
                       .build());

    // Wrap up
    phaseSteps.add(aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).build());
  }

  public void generateNewWorkflowPhaseStepsForPCF(
      String appId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    Service service = serviceResourceService.getWithDetails(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

    if (serviceSetupRequired) {
      Map<String, Object> defaultProperties = new HashMap<>();
      defaultProperties.put("blueGreen", false);
      defaultProperties.put("isWorkflowV2", true);
      defaultProperties.put("resizeStrategy", "DOWNSIZE_OLD_FIRST");

      phaseSteps.add(aPhaseStep(PhaseStepType.PCF_SETUP, SETUP)
                         .addStep(GraphNode.builder()
                                      .id(generateUuid())
                                      .type(StateType.PCF_SETUP.name())
                                      .name(PCF_SETUP)
                                      .properties(defaultProperties)
                                      .build())
                         .build());
    }

    phaseSteps.add(
        aPhaseStep(PhaseStepType.PCF_RESIZE, DEPLOY)
            .addStep(GraphNode.builder().id(generateUuid()).type(StateType.PCF_RESIZE.name()).name(PCF_RESIZE).build())
            .build());

    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                       .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                       .build());

    phaseSteps.add(aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).build());
  }

  public void generateNewWorkflowPhaseStepsForHelm(String appId, WorkflowPhase workflowPhase) {
    Service service = serviceResourceService.getWithDetails(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

    phaseSteps.add(
        aPhaseStep(PhaseStepType.HELM_DEPLOY, DEPLOY_CONTAINERS)
            .addStep(
                GraphNode.builder().id(generateUuid()).type(StateType.HELM_DEPLOY.name()).name(HELM_DEPLOY).build())
            .build());
    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                       .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                       .build());
    phaseSteps.add(aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).build());
  }

  public void generateNewWorkflowPhaseStepsForKubernetes(String appId, WorkflowPhase workflowPhase,
      boolean serviceSetupRequired, OrchestrationWorkflowType orchestrationWorkflowType) {
    Service service = serviceResourceService.getWithDetails(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

    if (serviceSetupRequired) {
      // TODO => (Prashant) check if the RUNTIME is for provisioner
      boolean isGcpInfra;
      if (featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, appService.getAccountIdByAppId(appId))) {
        InfrastructureDefinition infrastructureDefinition =
            infrastructureDefinitionService.get(appId, workflowPhase.getInfraDefinitionId());
        isGcpInfra = infrastructureDefinition.getInfrastructure() instanceof GoogleKubernetesEngine
            && RUNTIME.equals(((GoogleKubernetesEngine) infrastructureDefinition.getInfrastructure()).getClusterName());
      } else {
        InfrastructureMapping infraMapping = infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
        isGcpInfra = infraMapping instanceof GcpKubernetesInfrastructureMapping
            && RUNTIME.equals(((GcpKubernetesInfrastructureMapping) infraMapping).getClusterName());
      }
      if (isGcpInfra) {
        phaseSteps.add(aPhaseStep(CLUSTER_SETUP, SETUP_CLUSTER)
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
      phaseSteps.add(aPhaseStep(CONTAINER_SETUP, SETUP_CONTAINER)
                         .addStep(GraphNode.builder()
                                      .id(generateUuid())
                                      .type(KUBERNETES_SETUP.name())
                                      .name(KUBERNETES_SERVICE_SETUP)
                                      .properties(defaultSetupProperties)
                                      .build())
                         .build());
    }

    if (!workflowPhase.isDaemonSet() && !workflowPhase.isStatefulSet()) {
      Map<String, Object> properties = new HashMap<>();
      if (BASIC == orchestrationWorkflowType) {
        // Setting instance count always 100 percent
        properties.put("instanceCount", "100");
      }
      phaseSteps.add(aPhaseStep(CONTAINER_DEPLOY, DEPLOY_CONTAINERS)
                         .addStep(GraphNode.builder()
                                      .id(generateUuid())
                                      .type(KUBERNETES_DEPLOY.name())
                                      .name(UPGRADE_CONTAINERS)
                                      .properties(properties)
                                      .build())
                         .build());
    }
    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                       .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                       .build());
    phaseSteps.add(aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).build());
  }

  public void generateNewWorkflowPhaseStepsForKubernetesBlueGreen(
      String appId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    if (workflowPhase.isDaemonSet() || workflowPhase.isStatefulSet()) {
      throw new InvalidRequestException("DaemonSet and StatefulSet are not supported with Blue/Green Deployment", USER);
    }

    Service service = serviceResourceService.getWithDetails(appId, workflowPhase.getServiceId());
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

      phaseSteps.add(aPhaseStep(CONTAINER_SETUP, SETUP_CONTAINER)
                         .addStep(GraphNode.builder()
                                      .id(generateUuid())
                                      .type(KUBERNETES_SETUP.name())
                                      .name(KUBERNETES_SERVICE_SETUP_BLUEGREEN)
                                      .properties(defaultSetupProperties)
                                      .build())
                         .build());
    }

    Map<String, Object> defaultUpgradeStageContainerProperties = new HashMap<>();
    defaultUpgradeStageContainerProperties.put("instanceUnitType", "PERCENTAGE");
    defaultUpgradeStageContainerProperties.put("instanceCount", 100);
    defaultUpgradeStageContainerProperties.put("downsizeInstanceUnitType", "PERCENTAGE");
    defaultUpgradeStageContainerProperties.put("downsizeInstanceCount", 100);

    phaseSteps.add(aPhaseStep(CONTAINER_DEPLOY, DEPLOY_CONTAINERS)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(KUBERNETES_DEPLOY.name())
                                    .name(UPGRADE_CONTAINERS)
                                    .properties(defaultUpgradeStageContainerProperties)
                                    .build())
                       .build());

    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_STAGE_SERVICE)
                       .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                       .build());

    Map<String, Object> defaultRouteUpdateProperties = new HashMap<>();
    defaultRouteUpdateProperties.put("service1", PRIMARY_SERVICE_NAME_EXPRESSION);
    defaultRouteUpdateProperties.put("service2", STAGE_SERVICE_NAME_EXPRESSION);
    phaseSteps.add(aPhaseStep(PhaseStepType.ROUTE_UPDATE, ROUTE_UPDATE)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(KUBERNETES_SWAP_SERVICE_SELECTORS.name())
                                    .name(KUBERNETES_SWAP_SERVICES_PRIMARY_STAGE)
                                    .properties(defaultRouteUpdateProperties)
                                    .build())
                       .build());

    phaseSteps.add(aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).build());
  }

  public void generateNewWorkflowPhaseStepsForSSH(
      String appId, WorkflowPhase workflowPhase, OrchestrationWorkflowType orchestrationWorkflowType) {
    // For DC only - for other types it has to be customized
    String computeProviderType;
    boolean attachElbSteps;
    boolean infraRefactor =
        featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, appService.getAccountIdByAppId(appId));
    if (infraRefactor) {
      InfrastructureDefinition infrastructureDefinition =
          infrastructureDefinitionService.get(appId, workflowPhase.getInfraDefinitionId());
      computeProviderType = infrastructureDefinition.getCloudProviderType().name();
      attachElbSteps = attachElbSteps(infrastructureDefinition.getInfrastructure());
    } else {
      InfrastructureMapping infrastructureMapping =
          infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
      computeProviderType = infrastructureMapping.getComputeProviderType();
      attachElbSteps = attachElbSteps(infrastructureMapping);
    }

    StateType stateType;
    if (orchestrationWorkflowType == ROLLING) {
      stateType = ROLLING_NODE_SELECT;
    } else {
      stateType = computeProviderType.equals(PHYSICAL_DATA_CENTER.name()) ? DC_NODE_SELECT : AWS_NODE_SELECT;
    }

    if (!asList(ROLLING_NODE_SELECT, DC_NODE_SELECT, AWS_NODE_SELECT).contains(stateType)) {
      throw new InvalidRequestException("Unsupported state type: " + stateType, USER);
    }

    Service service = serviceResourceService.getWithDetails(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

    final PhaseStepBuilder infrastructurePhaseStepBuilder = aPhaseStep(INFRASTRUCTURE_NODE, INFRASTRUCTURE_NODE_NAME);

    infrastructurePhaseStepBuilder.addStep(GraphNode.builder()
                                               .type(stateType.name())
                                               .name(SELECT_NODE_NAME)
                                               .properties(ImmutableMap.<String, Object>builder()
                                                               .put("specificHosts", false)
                                                               .put("instanceCount", 1)
                                                               .put("excludeSelectedHostsFromFuturePhases", true)
                                                               .build())
                                               .build());

    phaseSteps.add(infrastructurePhaseStepBuilder.build());

    List<GraphNode> disableServiceSteps = commandNodes(commandMap, CommandType.DISABLE);
    List<GraphNode> enableServiceSteps = commandNodes(commandMap, CommandType.ENABLE);

    if (attachElbSteps) {
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

    phaseSteps.add(aPhaseStep(PhaseStepType.DISABLE_SERVICE, DISABLE_SERVICE).addAllSteps(disableServiceSteps).build());

    phaseSteps.add(aPhaseStep(PhaseStepType.DEPLOY_SERVICE, DEPLOY_SERVICE)
                       .addAllSteps(commandNodes(commandMap, CommandType.INSTALL))
                       .build());

    phaseSteps.add(aPhaseStep(PhaseStepType.ENABLE_SERVICE, ENABLE_SERVICE).addAllSteps(enableServiceSteps).build());

    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                       .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                       .build());

    phaseSteps.add(aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).build());
  }

  private boolean attachElbSteps(InfrastructureMapping infrastructureMapping) {
    return (infrastructureMapping instanceof PhysicalInfrastructureMappingBase
               && isNotBlank(((PhysicalInfrastructureMappingBase) infrastructureMapping).getLoadBalancerId()))
        || (infrastructureMapping instanceof AwsInfrastructureMapping
               && isNotBlank(((AwsInfrastructureMapping) infrastructureMapping).getLoadBalancerId()));
  }

  private boolean attachElbSteps(CloudProviderInfrastructure infrastructure) {
    return (infrastructure instanceof PhysicalInfra && isNotBlank(((PhysicalInfra) infrastructure).getLoadBalancerId()))
        || (infrastructure instanceof AwsInstanceInfrastructure
               && isNotBlank(((AwsInstanceInfrastructure) infrastructure).getLoadBalancerId()));
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForPCF(WorkflowPhase workflowPhase) {
    return rollbackWorkflow(workflowPhase)
        .phaseSteps(asList(aPhaseStep(PhaseStepType.PCF_RESIZE, DEPLOY)
                               .addStep(GraphNode.builder()
                                            .id(generateUuid())
                                            .type(StateType.PCF_ROLLBACK.name())
                                            .name(PCF_ROLLBACK)
                                            .rollback(true)
                                            .build())
                               .withPhaseStepNameForRollback(DEPLOY)
                               .withStatusForRollback(SUCCESS)
                               .withRollback(true)
                               .build(),
            // When we rolling back the verification steps
            // the same criteria to run if deployment is needed should be used
            aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                .withPhaseStepNameForRollback(DEPLOY_CONTAINERS)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).withRollback(true).build()))
        .build();
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForHelm(WorkflowPhase workflowPhase) {
    return rollbackWorkflow(workflowPhase)
        .phaseSteps(asList(aPhaseStep(PhaseStepType.HELM_DEPLOY, DEPLOY_CONTAINERS)
                               .addStep(GraphNode.builder()
                                            .id(generateUuid())
                                            .type(StateType.HELM_ROLLBACK.name())
                                            .name(HELM_ROLLBACK)
                                            .rollback(true)
                                            .build())
                               .withPhaseStepNameForRollback(DEPLOY_CONTAINERS)
                               .withStatusForRollback(SUCCESS)
                               .withRollback(true)
                               .build(),
            // When we rolling back the verification steps the same criterie to run if deployment is needed should be
            // used
            aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                .withPhaseStepNameForRollback(DEPLOY_CONTAINERS)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).withRollback(true).build()))
        .build();
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForAwsAmi(WorkflowPhase workflowPhase) {
    return rollbackWorkflow(workflowPhase)
        .phaseSteps(asList(aPhaseStep(AMI_DEPLOY_AUTOSCALING_GROUP, ROLLBACK_SERVICE)
                               .addStep(GraphNode.builder()
                                            .id(generateUuid())
                                            .type(AWS_AMI_SERVICE_ROLLBACK.name())
                                            .name(ROLLBACK_AWS_AMI_CLUSTER)
                                            .rollback(true)
                                            .build())
                               .withPhaseStepNameForRollback(DEPLOY_SERVICE)
                               .withStatusForRollback(SUCCESS)
                               .withRollback(true)
                               .build(),
            aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                .withRollback(true)
                .withPhaseStepNameForRollback(DEPLOY_SERVICE)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).withRollback(true).build()))
        .build();
  }

  @VisibleForTesting
  WorkflowPhase generateRollbackWorkflowPhaseForSpotinstAlbTrafficShift(WorkflowPhase workflowPhase) {
    return rollbackWorkflow(workflowPhase)
        .phaseSteps(
            asList(aPhaseStep(PhaseStepType.SPOTINST_LISTENER_UPDATE_ROLLBACK, SPOTINST_LISTENER_UPDATE_ROLLBACK)
                       .addStep(GraphNode.builder()
                                    .type(StateType.SPOTINST_LISTENER_ALB_SHIFT_ROLLBACK.name())
                                    .name(SPOTINST_SWAP_ROLLBACK)
                                    .rollback(true)
                                    .build())
                       .withPhaseStepNameForRollback(DEPLOY_SERVICE)
                       .withStatusForRollback(SUCCESS)
                       .withRollback(true)
                       .build(),
                aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                    .withRollback(true)
                    .withPhaseStepNameForRollback(DEPLOY_SERVICE)
                    .withStatusForRollback(SUCCESS)
                    .withRollback(true)
                    .build(),
                aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).withRollback(true).build()))
        .build();
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForSpotinst(WorkflowPhase workflowPhase) {
    return rollbackWorkflow(workflowPhase)
        .phaseSteps(asList(aPhaseStep(PhaseStepType.SPOTINST_ROLLBACK, SPOTINST_ROLLBACK)
                               .addStep(GraphNode.builder()
                                            .id(generateUuid())
                                            .type(StateType.SPOTINST_ROLLBACK.name())
                                            .name(SPOTINST_ROLLBACK)
                                            .rollback(true)
                                            .build())
                               .withPhaseStepNameForRollback(DEPLOY_SERVICE)
                               .withStatusForRollback(SUCCESS)
                               .withRollback(true)
                               .build(),
            aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                .withRollback(true)
                .withPhaseStepNameForRollback(DEPLOY_SERVICE)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).withRollback(true).build()))
        .build();
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForSpotInstBlueGreen(WorkflowPhase workflowPhase) {
    return rollbackWorkflow(workflowPhase)
        .phaseSteps(
            asList(aPhaseStep(PhaseStepType.SPOTINST_LISTENER_UPDATE_ROLLBACK, SPOTINST_LISTENER_UPDATE_ROLLBACK)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(StateType.SPOTINST_LISTENER_UPDATE_ROLLBACK.name())
                                    .name(SPOTINST_SWAP_ROLLBACK)
                                    .rollback(true)
                                    .build())
                       .withPhaseStepNameForRollback(DEPLOY_SERVICE)
                       .withStatusForRollback(SUCCESS)
                       .withRollback(true)
                       .build(),
                aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                    .withRollback(true)
                    .withPhaseStepNameForRollback(DEPLOY_SERVICE)
                    .withStatusForRollback(SUCCESS)
                    .withRollback(true)
                    .build(),
                aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).withRollback(true).build()))
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
                               .withPhaseStepNameForRollback(DEPLOY_SERVICE)
                               .withStatusForRollback(SUCCESS)
                               .withRollback(true)
                               .build(),
            aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                .withRollback(true)
                .withPhaseStepNameForRollback(DEPLOY_SERVICE)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).withRollback(true).build()))
        .build();
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForAwsLambda(WorkflowPhase workflowPhase) {
    return rollbackWorkflow(workflowPhase)
        .phaseSteps(asList(aPhaseStep(DEPLOY_AWS_LAMBDA, DEPLOY_SERVICE)
                               .addStep(GraphNode.builder()
                                            .id(generateUuid())
                                            .type(AWS_LAMBDA_ROLLBACK.name())
                                            .name(ROLLBACK_AWS_LAMBDA)
                                            .rollback(true)
                                            .build())
                               .withPhaseStepNameForRollback(DEPLOY_SERVICE)
                               .withStatusForRollback(SUCCESS)
                               .withRollback(true)
                               .build(),
            // Verificanion is not exactly rollbacking operation. It should be executed if deployment is needed
            aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                .withPhaseStepNameForRollback(DEPLOY_SERVICE)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).withRollback(true).build()))
        .build();
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForEcs(
      String appId, WorkflowPhase workflowPhase, OrchestrationWorkflowType orchestrationWorkflowType) {
    List<PhaseStep> phaseSteps = new ArrayList<>();
    if (isDaemonSchedulingStrategy(appId, workflowPhase, orchestrationWorkflowType)) {
      // For Daemon ECS workflow, need to add Setup rollback state
      phaseSteps.add(aPhaseStep(CONTAINER_SETUP, SETUP_CONTAINER)
                         .addStep(GraphNode.builder()
                                      .id(generateUuid())
                                      .type(ECS_SERVICE_SETUP_ROLLBACK.name())
                                      .name(ROLLBACK_CONTAINERS)
                                      .rollback(true)
                                      .build())
                         .withPhaseStepNameForRollback(SETUP_CONTAINER)
                         .withStatusForRollback(SUCCESS)
                         .withRollback(true)
                         .build());
    } else {
      phaseSteps.add(aPhaseStep(CONTAINER_DEPLOY, DEPLOY_CONTAINERS)
                         .addStep(GraphNode.builder()
                                      .id(generateUuid())
                                      .type(ECS_SERVICE_ROLLBACK.name())
                                      .name(ROLLBACK_CONTAINERS)
                                      .rollback(true)
                                      .build())
                         .withPhaseStepNameForRollback(DEPLOY_CONTAINERS)
                         .withStatusForRollback(SUCCESS)
                         .withRollback(true)
                         .build());
    }

    // Verification
    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                       .withPhaseStepNameForRollback(DEPLOY_CONTAINERS)
                       .withStatusForRollback(SUCCESS)
                       .withRollback(true)
                       .build());
    phaseSteps.add(aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).withRollback(true).build());

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
                               .withPhaseStepNameForRollback(DEPLOY_CONTAINERS)
                               .withStatusForRollback(SUCCESS)
                               .withRollback(true)
                               .build(),
            aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                .withPhaseStepNameForRollback(VERIFY_SERVICE)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).withRollback(true).build()))
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
                               .withPhaseStepNameForRollback(DEPLOY_CONTAINERS)
                               .withStatusForRollback(SUCCESS)
                               .withRollback(true)
                               .build(),

            aPhaseStep(CONTAINER_DEPLOY, DEPLOY_CONTAINERS)
                .addStep(GraphNode.builder()
                             .id(generateUuid())
                             .type(ECS_SERVICE_ROLLBACK.name())
                             .name(ROLLBACK_CONTAINERS)
                             .rollback(true)
                             .build())
                .withPhaseStepNameForRollback(DEPLOY_CONTAINERS)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            // Verification
            aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                .withPhaseStepNameForRollback(DEPLOY_CONTAINERS)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).withRollback(true).build()))
        .build();
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForAwsCodeDeploy(WorkflowPhase workflowPhase) {
    return rollbackWorkflow(workflowPhase)
        .phaseSteps(asList(aPhaseStep(DEPLOY_AWSCODEDEPLOY, DEPLOY_SERVICE)
                               .addStep(GraphNode.builder()
                                            .id(generateUuid())
                                            .type(AWS_CODEDEPLOY_ROLLBACK.name())
                                            .name(ROLLBACK_AWS_CODE_DEPLOY)
                                            .rollback(true)
                                            .build())
                               .withPhaseStepNameForRollback(DEPLOY_SERVICE)
                               .withStatusForRollback(SUCCESS)
                               .withRollback(true)
                               .build(),
            // When we rolling back the verification steps the same criterie to run if deployment is needed should be
            // used
            aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                .withPhaseStepNameForRollback(DEPLOY_SERVICE)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).withRollback(true).build()))
        .build();
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForSSH(String appId, WorkflowPhase workflowPhase) {
    Service service = serviceResourceService.getWithDetails(appId, workflowPhase.getServiceId());
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
        .phaseSteps(asList(aPhaseStep(PhaseStepType.DISABLE_SERVICE, DISABLE_SERVICE)
                               .addAllSteps(disableServiceSteps)
                               .withPhaseStepNameForRollback(ENABLE_SERVICE)
                               .withStatusForRollback(SUCCESS)
                               .withRollback(true)
                               .build(),
            aPhaseStep(PhaseStepType.STOP_SERVICE, STOP_SERVICE)
                .addAllSteps(commandNodes(commandMap, CommandType.STOP, true))
                .withPhaseStepNameForRollback(DEPLOY_SERVICE)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(PhaseStepType.DEPLOY_SERVICE, DEPLOY_SERVICE)
                .addAllSteps(commandNodes(commandMap, CommandType.INSTALL, true))
                .withPhaseStepNameForRollback(DEPLOY_SERVICE)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(PhaseStepType.ENABLE_SERVICE, ENABLE_SERVICE)
                .addAllSteps(enableServiceSteps)
                .withPhaseStepNameForRollback(DISABLE_SERVICE)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            // When we rolling back the verification steps
            // the same criteria to run if deployment is needed should be used
            aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                .addAllSteps(commandNodes(commandMap, CommandType.VERIFY, true))
                .withPhaseStepNameForRollback(DEPLOY_SERVICE)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).withRollback(true).build()))
        .build();
  }

  public WorkflowPhase generateRollbackWorkflowPhaseForKubernetes(
      WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    if (workflowPhase.isDaemonSet() || workflowPhase.isStatefulSet()) {
      return generateRollbackSetupWorkflowPhase(workflowPhase);
    }

    List<PhaseStep> phaseSteps = new ArrayList<>();

    phaseSteps.add(aPhaseStep(CONTAINER_DEPLOY, DEPLOY_CONTAINERS)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(KUBERNETES_DEPLOY_ROLLBACK.name())
                                    .name(ROLLBACK_CONTAINERS)
                                    .rollback(true)
                                    .build())
                       .withPhaseStepNameForRollback(DEPLOY_CONTAINERS)
                       .withStatusForRollback(SUCCESS)
                       .withRollback(true)
                       .build());
    if (serviceSetupRequired) {
      phaseSteps.add(aPhaseStep(CONTAINER_SETUP, SETUP_CONTAINER)
                         .addStep(GraphNode.builder()
                                      .id(generateUuid())
                                      .type(KUBERNETES_SETUP_ROLLBACK.name())
                                      .name(ROLLBACK_KUBERNETES_SETUP)
                                      .rollback(true)
                                      .build())
                         .withPhaseStepNameForRollback(SETUP_CONTAINER)
                         .withStatusForRollback(SUCCESS)
                         .withRollback(true)
                         .build());
    }

    // When we rolling back the verification steps the same criterie to run if deployment is needed should be used
    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                       .withPhaseStepNameForRollback(DEPLOY_CONTAINERS)
                       .withStatusForRollback(SUCCESS)
                       .withRollback(true)
                       .build());
    phaseSteps.add(aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).withRollback(true).build());

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

    phaseSteps.add(aPhaseStep(PhaseStepType.ROUTE_UPDATE, ROUTE_UPDATE)
                       .withPhaseStepNameForRollback(ROUTE_UPDATE)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(KUBERNETES_SWAP_SERVICE_SELECTORS.name())
                                    .name(KUBERNETES_SWAP_SERVICES_PRIMARY_STAGE)
                                    .properties(defaultRouteUpdateProperties)
                                    .rollback(true)
                                    .build())
                       .withRollback(true)
                       .build());
    phaseSteps.add(aPhaseStep(CONTAINER_DEPLOY, DEPLOY_CONTAINERS)
                       .withPhaseStepNameForRollback(DEPLOY_CONTAINERS)
                       .withStatusForRollback(SUCCESS)
                       .withRollback(true)
                       .build());
    if (serviceSetupRequired) {
      phaseSteps.add(aPhaseStep(CONTAINER_SETUP, SETUP_CONTAINER)
                         .withPhaseStepNameForRollback(SETUP_CONTAINER)
                         .withStatusForRollback(SUCCESS)
                         .withRollback(true)
                         .build());
    }

    // When we rolling back the verification steps the same criterie to run if deployment is needed should be used
    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                       .withPhaseStepNameForRollback(DEPLOY_CONTAINERS)
                       .withStatusForRollback(SUCCESS)
                       .withRollback(true)
                       .build());
    phaseSteps.add(aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).withRollback(true).build());
    return rollbackWorkflow(workflowPhase).phaseSteps(phaseSteps).build();
  }

  public WorkflowPhase generateRollbackSetupWorkflowPhase(WorkflowPhase workflowPhase) {
    return rollbackWorkflow(workflowPhase)
        .daemonSet(workflowPhase.isDaemonSet())
        .statefulSet(workflowPhase.isStatefulSet())
        .phaseSteps(asList(aPhaseStep(CONTAINER_SETUP, SETUP_CONTAINER)
                               .addStep(GraphNode.builder()
                                            .id(generateUuid())
                                            .type(KUBERNETES_SETUP_ROLLBACK.name())
                                            .name(ROLLBACK_KUBERNETES_SETUP)
                                            .rollback(true)
                                            .build())
                               .withPhaseStepNameForRollback(SETUP_CONTAINER)
                               .withStatusForRollback(SUCCESS)
                               .withRollback(true)
                               .build(),
            // When we rolling back the verification steps the same criterie to run if deployment is needed should be
            // used
            aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                .withPhaseStepNameForRollback(SETUP_CONTAINER)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).withRollback(true).build()))
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
   * @param infraId
   * @param envChanged
   * @param infraChanged
   * @param migration
   * @return OrchestrationWorkflow
   */
  public OrchestrationWorkflow propagateWorkflowDataToPhases(OrchestrationWorkflow orchestrationWorkflow,
      List<TemplateExpression> templateExpressions, String appId, String serviceId, String infraId, boolean envChanged,
      boolean infraChanged, boolean migration) {
    if (orchestrationWorkflow != null) {
      OrchestrationWorkflowType orchestrationWorkflowType = orchestrationWorkflow.getOrchestrationWorkflowType();
      if (orchestrationWorkflowType == BASIC || orchestrationWorkflowType == ROLLING
          || orchestrationWorkflowType == BLUE_GREEN) {
        handleBasicWorkflow((CanaryOrchestrationWorkflow) orchestrationWorkflow, templateExpressions, appId, serviceId,
            infraId, envChanged, infraChanged);
      } else if (orchestrationWorkflowType == MULTI_SERVICE || orchestrationWorkflowType == CANARY) {
        handleCanaryOrMultiServiceWorkflow(
            orchestrationWorkflow, templateExpressions, appId, envChanged, infraChanged, migration);
      }
    }
    return orchestrationWorkflow;
  }

  public void handleBasicWorkflow(CanaryOrchestrationWorkflow canaryOrchestrationWorkflow,
      List<TemplateExpression> templateExpressions, String appId, String serviceId, String infraId, boolean envChanged,
      boolean infraChanged) {
    String accountId = appService.getAccountIdByAppId(appId);
    boolean infraRefactor = featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, accountId);
    TemplateExpression envExpression =
        WorkflowServiceTemplateHelper.getTemplateExpression(templateExpressions, "envId");
    if (envExpression != null) {
      canaryOrchestrationWorkflow.addToUserVariables(asList(envExpression));
    }
    if (canaryOrchestrationWorkflow.getWorkflowPhases() != null) {
      for (WorkflowPhase phase : canaryOrchestrationWorkflow.getWorkflowPhases()) {
        WorkflowServiceTemplateHelper.setTemplateExpresssionsToPhase(templateExpressions, phase);
        if (!phase.checkServiceTemplatized()) {
          validateServiceCompatibility(appId, serviceId, phase.getServiceId());
        }
        if (serviceId != null) {
          phase.setServiceId(serviceId);
        }
        if (infraRefactor) {
          setInfraDefinitionDetails(appId, infraId, phase, envChanged, infraChanged);
        } else {
          setInframappingDetails(appId, infraId, phase, envChanged, infraChanged);
        }
        if (infraChanged || envChanged) {
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
        if (infraRefactor) {
          setInfraDefinitionDetails(appId, infraId, phase, envChanged, infraChanged);
        } else {
          setInframappingDetails(appId, infraId, phase, envChanged, infraChanged);
        }
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

  private void setInfraDefinitionDetails(
      String appId, String infraDefinitionId, WorkflowPhase phase, boolean envChanged, boolean infraChanged) {
    if (infraDefinitionId != null) {
      if (!infraDefinitionId.equals(phase.getInfraDefinitionId())) {
        phase.setInfraDefinitionId(infraDefinitionId);
        InfrastructureDefinition infrastructureDefinition =
            infrastructureDefinitionService.get(appId, phase.getInfraDefinitionId());
        notNullCheck("Infra Structure Does Not exist", infrastructureDefinition, USER);
        phase.setComputeProviderId(infrastructureDefinition.getInfrastructure().getCloudProviderId());
        phase.setDeploymentType(infrastructureDefinition.getDeploymentType());
        resetNodeSelection(phase);
      }
    } else if (envChanged && !infraChanged) {
      unsetInfraDefinitionsDetails(phase);
    }
  }

  private void handleCanaryOrMultiServiceWorkflow(OrchestrationWorkflow orchestrationWorkflow,
      List<TemplateExpression> templateExpressions, String appId, boolean envChanged, boolean infraChanged,
      boolean migration) {
    boolean infraRefactor =
        featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, appService.getAccountIdByAppId(appId));
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
    canaryOrchestrationWorkflow.addToUserVariables(templateExpressions);
    // If envId changed nullify the infraMapping Ids
    if (canaryOrchestrationWorkflow.getWorkflowPhases() != null) {
      for (WorkflowPhase phase : canaryOrchestrationWorkflow.getWorkflowPhases()) {
        if (envChanged && !infraRefactor) {
          unsetInfraMappingDetails(phase);
          resetNodeSelection(phase);
        }
        if (infraChanged) {
          resetNodeSelection(phase);
        }
        // If environment templatized, then templatize infra automatically
        List<TemplateExpression> phaseTemplateExpressions = phase.getTemplateExpressions();
        if (phaseTemplateExpressions == null) {
          phaseTemplateExpressions = new ArrayList<>();
        }

        boolean envTemplatized = WorkflowServiceTemplateHelper.isEnvironmentTemplatized(templateExpressions);

        if (envTemplatized && !migration) {
          if (infraRefactor && !WorkflowServiceTemplateHelper.isInfraDefinitionTemplatized(phaseTemplateExpressions)) {
            Service service = serviceResourceService.get(appId, phase.getServiceId(), false);
            notNullCheck("Service", service, USER);
            WorkflowServiceTemplateHelper.templatizeInfraDefinition(
                orchestrationWorkflow, phase, phaseTemplateExpressions, service);
          } else if (!infraRefactor && !WorkflowServiceTemplateHelper.isInfraTemplatized(phaseTemplateExpressions)) {
            Service service = serviceResourceService.get(appId, phase.getServiceId(), false);
            notNullCheck("Service", service, USER);
            WorkflowServiceTemplateHelper.templatizeServiceInfra(
                orchestrationWorkflow, phase, phaseTemplateExpressions, service);
          }
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
        if (infraChanged) {
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
    Service oldService = null;
    if (EmptyPredicate.isNotEmpty(oldServiceId)) {
      oldService = serviceResourceService.get(appId, oldServiceId, false);
    }
    if (oldService == null) {
      // As service has been deleted, compatibility check does not make sense here
      return;
    }

    Service newService = serviceResourceService.get(appId, serviceId, false);
    notNullCheck("service", newService, USER);
    if (oldService.getArtifactType() != null && oldService.getArtifactType() != newService.getArtifactType()) {
      throw new InvalidRequestException(
          "Service [" + newService.getName() + "] is not compatible with the service [" + oldService.getName() + "]",
          USER);
    }

    if (oldService.isK8sV2() != newService.isK8sV2()) {
      throw new InvalidRequestException("Service [" + newService.getName() + "] is not compatible with the service ["
              + oldService.getName() + "] due to different kubernetes version",
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
        .filter(phaseStep -> phaseStep.getPhaseStepType() == INFRASTRUCTURE_NODE)
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
    return entityIds.stream().distinct().collect(toList());
  }

  public List<InfrastructureMapping> getResolvedInfraMappings(
      Workflow workflow, Map<String, String> workflowVariables) {
    if (workflow == null || workflow.getOrchestrationWorkflow() == null) {
      return new ArrayList<>();
    }
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    if (orchestrationWorkflow.isInfraMappingTemplatized()) {
      return resolvedTemplateInfraMappings(workflow, workflowVariables);
    }
    return infrastructureMappingService.getInfraStructureMappingsByUuids(
        workflow.getAppId(), orchestrationWorkflow.getInfraMappingIds());
  }

  public List<InfrastructureDefinition> getResolvedInfraDefinitions(
      Workflow workflow, Map<String, String> workflowVariables) {
    if (workflow == null || workflow.getOrchestrationWorkflow() == null) {
      return new ArrayList<>();
    }
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    if (orchestrationWorkflow.isInfraDefinitionTemplatized()) {
      return resolvedTemplateInfraDefinitions(workflow, workflowVariables);
    }
    return infrastructureDefinitionService.getInfraStructureDefinitionByUuids(
        workflow.getAppId(), orchestrationWorkflow.getInfraDefinitionIds());
  }

  public List<String> getResolvedInfraMappingIds(Workflow workflow, Map<String, String> workflowVariables) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    if (orchestrationWorkflow.isInfraMappingTemplatized()) {
      return resolveInfraMappingIds(workflow, workflowVariables);
    } else {
      return orchestrationWorkflow.getInfraMappingIds();
    }
  }

  public List<String> getResolvedInfraDefinitionIds(Workflow workflow, Map<String, String> workflowVariables) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    if (orchestrationWorkflow.isInfraDefinitionTemplatized()) {
      return resolveInfraDefinitionIds(workflow, workflowVariables);
    } else {
      return orchestrationWorkflow.getInfraDefinitionIds();
    }
  }

  private List<InfrastructureMapping> resolvedTemplateInfraMappings(
      Workflow workflow, Map<String, String> workflowVariables) {
    List<String> infraMappingIds = resolveInfraMappingIds(workflow, workflowVariables);
    return infrastructureMappingService.getInfraStructureMappingsByUuids(workflow.getAppId(), infraMappingIds);
  }

  private List<InfrastructureDefinition> resolvedTemplateInfraDefinitions(
      Workflow workflow, Map<String, String> workflowVariables) {
    List<String> infrDefinitionIds = resolveInfraDefinitionIds(workflow, workflowVariables);
    return infrastructureDefinitionService.getInfraStructureDefinitionByUuids(workflow.getAppId(), infrDefinitionIds);
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
    List<String> workflowInfraMappingIds = orchestrationWorkflow.getInfraMappingIds();
    if (workflowInfraMappingIds != null) {
      workflowInfraMappingIds.stream()
          .filter(infraMappingId -> !templatizedInfraMappingIds.contains(infraMappingId))
          .forEach(infraMappingIds::add);
    }
    return infraMappingIds;
  }

  private List<String> resolveInfraDefinitionIds(Workflow workflow, Map<String, String> workflowVariables) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    List<Variable> userVariables = orchestrationWorkflow.getUserVariables();
    List<String> infraDefinitionNames = new ArrayList<>();
    if (userVariables != null) {
      infraDefinitionNames = getEntityNames(userVariables, INFRASTRUCTURE_DEFINITION);
    }
    List<String> infraDefinitionIds = getTemplatizedIds(workflowVariables, infraDefinitionNames);
    List<String> templatizedInfraDefinitionIds = orchestrationWorkflow.getTemplatizedInfraDefinitionIds();
    List<String> workflowDefinitionIds = orchestrationWorkflow.getInfraDefinitionIds();
    if (workflowDefinitionIds != null) {
      workflowDefinitionIds.stream()
          .filter(infraDefinitionId -> !templatizedInfraDefinitionIds.contains(infraDefinitionId))
          .forEach(infraDefinitionIds::add);
    }
    return infraDefinitionIds;
  }

  private List<String> getEntityNames(List<Variable> userVariables, EntityType entityType) {
    return userVariables.stream()
        .filter(variable -> entityType == variable.obtainEntityType())
        .map(Variable::getName)
        .collect(toList());
  }

  public void unsetInfraMappingDetails(WorkflowPhase phase) {
    phase.setComputeProviderId(null);
    phase.setInfraMappingId(null);
    phase.setInfraMappingName(null);
    // phase.setDeploymentType(null);
  }

  public void unsetInfraDefinitionsDetails(WorkflowPhase phase) {
    phase.setComputeProviderId(null);
    phase.setInfraDefinitionId(null);
    phase.setInfraDefinitionName(null);
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
    if (isNotEmpty(orchestrationWorkflow.getWorkflowPhases())) {
      WorkflowPhase firstPhase = orchestrationWorkflow.getWorkflowPhases().get(0);
      if (isNotEmpty(firstPhase.getPhaseSteps())) {
        PhaseStep firstPhaseStep = firstPhase.getPhaseSteps().get(0);
        return firstPhaseStep.getPhaseStepType() == K8S_PHASE_STEP;
      }
    }

    if (isNotEmpty(orchestrationWorkflow.getServiceIds())) {
      return isK8sV2Service(appId, orchestrationWorkflow.getServiceIds().get(0));
    }
    return false;
  }

  public boolean isK8sV2Service(String appId, String serviceId) {
    Service service = serviceResourceService.get(appId, serviceId, false);
    return service != null && service.isK8sV2();
  }

  public static void checkWorkflowVariablesOverrides(String stageElementName, List<Variable> variables,
      Map<String, String> workflowStepVariables, Map<String, String> pipelineVariables) {
    if (isEmpty(variables)) {
      return;
    }

    List<Variable> requiredVariables =
        variables.stream().filter(variable -> variable.isMandatory() && !variable.isFixed()).collect(toList());
    if (isEmpty(requiredVariables)) {
      return;
    }

    for (Variable variable : requiredVariables) {
      boolean isEntity = variable.obtainEntityType() != null;
      String workflowVariableValue = extractMapValue(workflowStepVariables, variable.getName());
      String finalValue;
      if (isEmpty(workflowVariableValue)) {
        finalValue = extractMapValue(pipelineVariables, variable.getName());
      } else {
        // Non entity variables can contain expressions like `${workflow.variables.var1}`. If entity variables contain
        // such a pattern, we throw an error.
        if (ExpressionEvaluator.matchesVariablePattern(workflowVariableValue) && !workflowVariableValue.contains(".")) {
          String pipelineVariableName = ExpressionEvaluator.getName(workflowVariableValue);
          finalValue = extractMapValue(pipelineVariables, pipelineVariableName);
        } else {
          finalValue = workflowVariableValue;
        }
      }

      String prefix = isEntity ? "Templatized" : "Required";
      if (isEmpty(finalValue)) {
        throw new InvalidRequestException(
            format("%s variable %s is not set for stage %s", prefix, variable.getName(), stageElementName));
      } else if (ExpressionEvaluator.matchesVariablePattern(finalValue) && (isEntity || !finalValue.contains("."))) {
        throw new InvalidRequestException(format("%s variable %s for stage %s cannot be left as an expression", prefix,
            variable.getName(), stageElementName));
      }
    }
  }

  private static String extractMapValue(Map<String, String> map, String key) {
    return map == null ? null : map.getOrDefault(key, null);
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

  public void generateNewWorkflowPhaseSteps(String appId, String envId, WorkflowPhase workflowPhase,
      boolean serviceRepeat, OrchestrationWorkflowType orchestrationWorkflowType, WorkflowCreationFlags creationFlags) {
    DeploymentType deploymentType = workflowPhase.getDeploymentType();
    String accountId = appService.getAccountIdByAppId(appId);
    boolean infraMappingRefactorEnabled = featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, accountId);
    boolean isDynamicInfrastructure;
    if (infraMappingRefactorEnabled) {
      isDynamicInfrastructure =
          infrastructureDefinitionService.isDynamicInfrastructure(appId, workflowPhase.getInfraDefinitionId());
    } else {
      isDynamicInfrastructure = isDynamicInfrastructure(appId, workflowPhase.getInfraMappingId());
    }

    if (deploymentType == ECS) {
      if (orchestrationWorkflowType == OrchestrationWorkflowType.BLUE_GREEN) {
        if (creationFlags != null && creationFlags.isEcsBgDnsType()) {
          generateNewWorkflowPhaseStepsForECSBlueGreenRoute53(appId, workflowPhase, !serviceRepeat);
        } else {
          generateNewWorkflowPhaseStepsForECSBlueGreen(appId, workflowPhase, !serviceRepeat);
        }
      } else {
        generateNewWorkflowPhaseStepsForECS(appId, workflowPhase, !serviceRepeat, orchestrationWorkflowType);
      }
    } else if (deploymentType == KUBERNETES) {
      if (orchestrationWorkflowType == OrchestrationWorkflowType.BLUE_GREEN) {
        generateNewWorkflowPhaseStepsForKubernetesBlueGreen(appId, workflowPhase, !serviceRepeat);
      } else {
        generateNewWorkflowPhaseStepsForKubernetes(appId, workflowPhase, !serviceRepeat, orchestrationWorkflowType);
      }
    } else if (deploymentType == HELM) {
      generateNewWorkflowPhaseStepsForHelm(appId, workflowPhase);
    } else if (deploymentType == AWS_CODEDEPLOY) {
      generateNewWorkflowPhaseStepsForAWSCodeDeploy(appId, workflowPhase);
    } else if (deploymentType == DeploymentType.AWS_LAMBDA) {
      generateNewWorkflowPhaseStepsForAWSLambda(appId, workflowPhase);
    } else if (deploymentType == AMI) {
      if (isSpotInstTypeInfra(infraMappingRefactorEnabled, workflowPhase.getInfraDefinitionId(),
              workflowPhase.getInfraMappingId(), appId)) {
        if (BLUE_GREEN == orchestrationWorkflowType) {
          generateNewWfPhaseStepsForSpotinstBg(creationFlags, accountId, appId, workflowPhase, serviceRepeat);
        } else {
          generateNewWorkflowPhaseStepsForSpotinst(appId, workflowPhase, !serviceRepeat);
        }
      } else {
        if (BLUE_GREEN == orchestrationWorkflowType) {
          generateNewWorkflowPhaseStepsForAWSAmiBlueGreen(
              appId, workflowPhase, !serviceRepeat, isDynamicInfrastructure);
        } else {
          generateNewWorkflowPhaseStepsForAWSAmi(
              appId, workflowPhase, !serviceRepeat, isDynamicInfrastructure, orchestrationWorkflowType);
        }
      }
    } else if (deploymentType == PCF) {
      if (orchestrationWorkflowType == OrchestrationWorkflowType.BLUE_GREEN) {
        generateNewWorkflowPhaseStepsForPCFBlueGreen(appId, workflowPhase, !serviceRepeat);
      } else {
        generateNewWorkflowPhaseStepsForPCF(appId, workflowPhase, !serviceRepeat);
      }
    } else {
      generateNewWorkflowPhaseStepsForSSH(appId, workflowPhase, orchestrationWorkflowType);
    }
  }

  private void generateNewWfPhaseStepsForSpotinstBg(WorkflowCreationFlags creationFlags, String accountId, String appId,
      WorkflowPhase workflowPhase, boolean serviceRepeat) {
    if (isAlbTrafficShiftType(creationFlags, accountId)) {
      generateNewWorkflowPhaseStepsForSpotinstAlbTrafficShift(appId, workflowPhase);
    } else {
      generateNewWorkflowPhaseStepsForSpotInstBlueGreen(appId, workflowPhase, !serviceRepeat);
    }
  }

  private boolean isAlbTrafficShiftType(WorkflowCreationFlags flags, String accountId) {
    return featureFlagService.isEnabled(AWS_TRAFFIC_SHIFT, accountId) && flags != null
        && flags.isAwsTrafficShiftAlbType();
  }

  public WorkflowPhase generateRollbackWorkflowPhase(String appId, WorkflowPhase workflowPhase,
      boolean serviceSetupRequired, OrchestrationWorkflowType orchestrationWorkflowType,
      WorkflowCreationFlags creationFlags) {
    DeploymentType deploymentType = workflowPhase.getDeploymentType();
    if (deploymentType == ECS) {
      if (orchestrationWorkflowType == OrchestrationWorkflowType.BLUE_GREEN) {
        if (creationFlags != null && creationFlags.isEcsBgDnsType()) {
          return generateRollbackWorkflowPhaseForEcsBlueGreenRoute53(appId, workflowPhase, orchestrationWorkflowType);
        } else {
          return generateRollbackWorkflowPhaseForEcsBlueGreen(appId, workflowPhase, orchestrationWorkflowType);
        }
      } else {
        return generateRollbackWorkflowPhaseForEcs(appId, workflowPhase, orchestrationWorkflowType);
      }
    } else if (deploymentType == KUBERNETES) {
      if (orchestrationWorkflowType == OrchestrationWorkflowType.BLUE_GREEN) {
        return generateRollbackWorkflowPhaseForKubernetesBlueGreen(workflowPhase, serviceSetupRequired);
      } else {
        return generateRollbackWorkflowPhaseForKubernetes(workflowPhase, serviceSetupRequired);
      }
    } else if (deploymentType == AWS_CODEDEPLOY) {
      return generateRollbackWorkflowPhaseForAwsCodeDeploy(workflowPhase);
    } else if (deploymentType == DeploymentType.AWS_LAMBDA) {
      return generateRollbackWorkflowPhaseForAwsLambda(workflowPhase);
    } else if (deploymentType == AMI) {
      String accountId = appService.getAccountIdByAppId(appId);
      if (isSpotInstTypeInfra(featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, accountId),
              workflowPhase.getInfraDefinitionId(), workflowPhase.getInfraMappingId(), appId)) {
        if (BLUE_GREEN == orchestrationWorkflowType) {
          return generateRollbackBgPhaseForSpotinstBg(workflowPhase, creationFlags, accountId);
        } else {
          return generateRollbackWorkflowPhaseForSpotinst(workflowPhase);
        }
      } else {
        if (BLUE_GREEN == orchestrationWorkflowType) {
          return generateRollbackWorkflowPhaseForAwsAmiBlueGreen(workflowPhase);
        } else {
          return generateRollbackWorkflowPhaseForAwsAmi(workflowPhase);
        }
      }
    } else if (deploymentType == HELM) {
      return generateRollbackWorkflowPhaseForHelm(workflowPhase);
    } else if (deploymentType == PCF) {
      if (orchestrationWorkflowType == OrchestrationWorkflowType.BLUE_GREEN) {
        return generateRollbackWorkflowPhaseForPCFBlueGreen(workflowPhase, serviceSetupRequired);
      } else {
        return generateRollbackWorkflowPhaseForPCF(workflowPhase);
      }
    } else {
      return generateRollbackWorkflowPhaseForSSH(appId, workflowPhase);
    }
  }

  private WorkflowPhase generateRollbackBgPhaseForSpotinstBg(
      WorkflowPhase workflowPhase, WorkflowCreationFlags creationFlags, String accountId) {
    if (isAlbTrafficShiftType(creationFlags, accountId)) {
      return generateRollbackWorkflowPhaseForSpotinstAlbTrafficShift(workflowPhase);
    } else {
      return generateRollbackWorkflowPhaseForSpotInstBlueGreen(workflowPhase);
    }
  }

  public void generateNewWorkflowPhaseStepsForArtifactCollection(WorkflowPhase workflowPhase) {
    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

    phaseSteps.add(aPhaseStep(PhaseStepType.PREPARE_STEPS, WorkflowServiceHelper.PREPARE_STEPS).build());

    phaseSteps.add(aPhaseStep(COLLECT_ARTIFACT, COLLECT_ARTIFACT_PHASE_STEP_NAME)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(ARTIFACT_COLLECTION.name())
                                    .name(ARTIFACT_COLLECTION_STEP)
                                    .build())
                       .build());
    phaseSteps.add(aPhaseStep(PhaseStepType.WRAP_UP, WorkflowServiceHelper.WRAP_UP).build());
  }

  private boolean isDynamicInfrastructure(String appId, String infrastructureMappingId) {
    InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, infrastructureMappingId);
    if (infrastructureMapping == null) {
      return false;
    }
    return isNotEmpty(infrastructureMapping.getProvisionerId());
  }

  private boolean isSpotInstTypeInfra(
      boolean infraMappingRefactorEnabled, String infraDefinitionId, String infraMappingId, String appId) {
    if (infraMappingRefactorEnabled) {
      InfrastructureDefinition infrastructureDefinition = infrastructureDefinitionService.get(appId, infraDefinitionId);
      InfraMappingInfrastructureProvider infrastructure = infrastructureDefinition.getInfrastructure();
      return infrastructure instanceof AwsAmiInfrastructure
          && AmiDeploymentType.SPOTINST == ((AwsAmiInfrastructure) infrastructure).getAmiDeploymentType();
    } else {
      return false;
    }
  }

  public WorkflowPhase getWorkflowPhase(Workflow workflow, String phaseId) {
    if (isBlank(phaseId)) {
      throw new InvalidRequestException("Phase Id not provided");
    }
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    return orchestrationWorkflow.getWorkflowPhases()
        .stream()
        .filter(workflowPhase -> workflowPhase.getUuid().equals(phaseId))
        .findFirst()
        .orElse(null);
  }

  public String getServiceIdFromPhase(WorkflowPhase workflowPhase) {
    if (workflowPhase != null) {
      //  in the future once service commands moved to template library, return serviceId only if service is not
      //  templatized in workflow phase
      return workflowPhase.getServiceId();
    }
    return null;
  }

  /**
   * Get service commands - exclude service that has internal commands
   * @param workflowPhase
   * @param appId
   * @param svcId
   * @return
   */
  public List<String> getServiceCommands(WorkflowPhase workflowPhase, String appId, String svcId) {
    if (svcId != null && workflowPhase != null) {
      // Read Service commands only for SSH Deployment Type PDC, AWS and WINRM
      DeploymentType deploymentType = workflowPhase.getDeploymentType();
      if ((deploymentType == null || DeploymentType.SSH == deploymentType || DeploymentType.WINRM == deploymentType)
          && serviceResourceService.exist(appId, svcId)) {
        return serviceResourceService.getServiceCommands(appId, svcId)
            .stream()
            .map(serviceCommand -> serviceCommand.getCommand().getName())
            .distinct()
            .collect(Collectors.toList());
      }
    }
    return new ArrayList<>();
  }

  public static void cleanupStepSkipStrategies(PhaseStep phaseStep) {
    if (phaseStep == null || isEmpty(phaseStep.getStepSkipStrategies())) {
      return;
    }

    Set<String> stepIds = phaseStep.getSteps() == null
        ? Collections.emptySet()
        : phaseStep.getSteps().stream().map(GraphNode::getId).collect(Collectors.toSet());
    phaseStep.setStepSkipStrategies(
        phaseStep.getStepSkipStrategies()
            .stream()
            .filter(stepSkipStrategy -> {
              if (stepSkipStrategy.getScope() == StepSkipStrategy.Scope.ALL_STEPS) {
                return true;
              }

              if (isNotEmpty(stepSkipStrategy.getStepIds())) {
                stepSkipStrategy.setStepIds(
                    stepSkipStrategy.getStepIds().stream().filter(stepIds::contains).collect(toList()));
              }

              return isNotEmpty(stepSkipStrategy.getStepIds());
            })
            .collect(toList()));
  }

  public static void cleanupPhaseStepSkipStrategies(WorkflowPhase workflowPhase) {
    if (workflowPhase == null || isEmpty(workflowPhase.getPhaseSteps())) {
      return;
    }

    for (PhaseStep phaseStep : workflowPhase.getPhaseSteps()) {
      cleanupStepSkipStrategies(phaseStep);
    }
  }

  public static void cleanupWorkflowStepSkipStrategies(OrchestrationWorkflow orchestrationWorkflow) {
    if (!(orchestrationWorkflow instanceof CanaryOrchestrationWorkflow)) {
      return;
    }

    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
    cleanupStepSkipStrategies(canaryOrchestrationWorkflow.getPreDeploymentSteps());
    cleanupStepSkipStrategies(canaryOrchestrationWorkflow.getPostDeploymentSteps());
    if (isEmpty(canaryOrchestrationWorkflow.getWorkflowPhases())) {
      return;
    }

    for (WorkflowPhase workflowPhase : canaryOrchestrationWorkflow.getWorkflowPhases()) {
      cleanupPhaseStepSkipStrategies(workflowPhase);
    }
  }
}

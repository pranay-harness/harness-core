package software.wings.service.impl.yaml.handler;

import static software.wings.common.Constants.ECS_SERVICE_SPEC;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.ConfigFile;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.LambdaSpecification;
import software.wings.beans.NotificationGroup;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.container.EcsContainerTask;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.beans.container.PcfServiceSpecification;
import software.wings.beans.container.UserDataSpecification;
import software.wings.beans.yaml.YamlConstants;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.app.ApplicationYamlHandler;
import software.wings.service.impl.yaml.handler.artifactstream.ArtifactStreamYamlHandler;
import software.wings.service.impl.yaml.handler.command.CommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.CommandYamlHandler;
import software.wings.service.impl.yaml.handler.configfile.ConfigFileOverrideYamlHandler;
import software.wings.service.impl.yaml.handler.configfile.ConfigFileYamlHandler;
import software.wings.service.impl.yaml.handler.defaults.DefaultVariablesYamlHandler;
import software.wings.service.impl.yaml.handler.deploymentspec.DeploymentSpecificationYamlHandler;
import software.wings.service.impl.yaml.handler.deploymentspec.container.ContainerDefinitionYamlHandler;
import software.wings.service.impl.yaml.handler.deploymentspec.container.LogConfigurationYamlHandler;
import software.wings.service.impl.yaml.handler.deploymentspec.container.PortMappingYamlHandler;
import software.wings.service.impl.yaml.handler.deploymentspec.container.StorageConfigurationYamlHandler;
import software.wings.service.impl.yaml.handler.deploymentspec.lambda.DefaultSpecificationYamlHandler;
import software.wings.service.impl.yaml.handler.deploymentspec.lambda.FunctionSpecificationYamlHandler;
import software.wings.service.impl.yaml.handler.environment.EnvironmentYamlHandler;
import software.wings.service.impl.yaml.handler.inframapping.InfraMappingYamlHandler;
import software.wings.service.impl.yaml.handler.infraprovisioner.InfrastructureProvisionerYamlHandler;
import software.wings.service.impl.yaml.handler.notification.NotificationGroupYamlHandler;
import software.wings.service.impl.yaml.handler.notification.NotificationRulesYamlHandler;
import software.wings.service.impl.yaml.handler.service.ServiceYamlHandler;
import software.wings.service.impl.yaml.handler.setting.artifactserver.ArtifactServerYamlHandler;
import software.wings.service.impl.yaml.handler.setting.cloudprovider.CloudProviderYamlHandler;
import software.wings.service.impl.yaml.handler.setting.collaborationprovider.CollaborationProviderYamlHandler;
import software.wings.service.impl.yaml.handler.setting.loadbalancer.ElasticLoadBalancerConfigYamlHandler;
import software.wings.service.impl.yaml.handler.setting.verificationprovider.VerificationProviderYamlHandler;
import software.wings.service.impl.yaml.handler.template.TemplateExpressionYamlHandler;
import software.wings.service.impl.yaml.handler.variable.VariableYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.FailureStrategyYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.PhaseStepYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.PipelineStageYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.PipelineYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.StepYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.WorkflowPhaseYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.WorkflowYamlHandler;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author rktummala on 10/19/17
 */
@Singleton
public class YamlHandlerFactory {
  private static final Logger logger = LoggerFactory.getLogger(YamlHandlerFactory.class);

  private static final Set<String> nonLeafEntities = new HashSet(obtainNonLeafEntities());
  private static final Set<String> leafEntities = new HashSet<>(obtainLeafEntities());

  @Inject private Map<String, ArtifactStreamYamlHandler> artifactStreamHelperMap;
  @Inject private Map<String, InfraMappingYamlHandler> infraMappingHelperMap;
  @Inject private Map<String, WorkflowYamlHandler> workflowYamlHelperMap;
  @Inject private Map<String, InfrastructureProvisionerYamlHandler> provisionerYamlHandlerMap;
  @Inject private Map<String, CommandUnitYamlHandler> commandUnitYamlHandlerMap;
  @Inject private Map<String, DeploymentSpecificationYamlHandler> deploymentSpecYamlHandlerMap;
  @Inject private Map<String, ArtifactServerYamlHandler> artifactServerYamlHelperMap;
  @Inject private Map<String, VerificationProviderYamlHandler> verificationProviderYamlHelperMap;
  @Inject private Map<String, InfrastructureProvisionerYamlHandler> infrastructureProvisionerYamlHandler;
  @Inject private Map<String, CollaborationProviderYamlHandler> collaborationProviderYamlHelperMap;
  @Inject private Map<String, CloudProviderYamlHandler> cloudProviderYamlHelperMap;

  @Inject private ApplicationYamlHandler applicationYamlHandler;
  @Inject private EnvironmentYamlHandler environmentYamlHandler;
  @Inject private ServiceYamlHandler serviceYamlHandler;
  @Inject private ConfigFileYamlHandler configFileYamlHandler;
  @Inject private ConfigFileOverrideYamlHandler configFileOverrideYamlHandler;
  @Inject private CommandYamlHandler commandYamlHandler;
  @Inject private NameValuePairYamlHandler nameValuePairYamlHandler;
  @Inject private PhaseStepYamlHandler phaseStepYamlHandler;
  @Inject private StepYamlHandler stepYamlHandler;
  @Inject private WorkflowPhaseYamlHandler workflowPhaseYamlHandler;
  @Inject private TemplateExpressionYamlHandler templateExpressionYamlHandler;
  @Inject private VariableYamlHandler variableYamlHandler;
  @Inject private NotificationRulesYamlHandler notificationRulesYamlHandler;
  @Inject private NotificationGroupYamlHandler notificationGroupYamlHandler;
  @Inject private FailureStrategyYamlHandler failureStrategyYamlHandler;
  @Inject private PipelineYamlHandler pipelineYamlHandler;
  @Inject private PipelineStageYamlHandler pipelineStageYamlHandler;
  @Inject private ContainerDefinitionYamlHandler containerDefinitionYamlHandler;
  @Inject private PortMappingYamlHandler portMappingYamlHandler;
  @Inject private StorageConfigurationYamlHandler storageConfigurationYamlHandler;
  @Inject private LogConfigurationYamlHandler logConfigurationYamlHandler;
  @Inject private DefaultSpecificationYamlHandler defaultSpecificationYamlHandler;
  @Inject private FunctionSpecificationYamlHandler functionSpecificationYamlHandler;
  @Inject private ElasticLoadBalancerConfigYamlHandler elbConfigYamlHandler;
  @Inject private DefaultVariablesYamlHandler defaultsYamlHandler;

  public <T extends BaseYamlHandler> T getYamlHandler(YamlType yamlType) {
    return getYamlHandler(yamlType, null);
  }

  public <T extends BaseYamlHandler> T getYamlHandler(YamlType yamlType, String subType) {
    Object yamlHandler = null;
    switch (yamlType) {
      case CLOUD_PROVIDER:
        yamlHandler = cloudProviderYamlHelperMap.get(subType);
        break;
      case ARTIFACT_SERVER:
        yamlHandler = artifactServerYamlHelperMap.get(subType);
        break;
      case COLLABORATION_PROVIDER:
        yamlHandler = collaborationProviderYamlHelperMap.get(subType);
        break;
      case LOADBALANCER_PROVIDER:
        yamlHandler = elbConfigYamlHandler;
        break;
      case VERIFICATION_PROVIDER:
        yamlHandler = verificationProviderYamlHelperMap.get(subType);
        break;
      case APPLICATION:
        yamlHandler = applicationYamlHandler;
        break;
      case SERVICE:
        yamlHandler = serviceYamlHandler;
        break;
      case ARTIFACT_STREAM:
        yamlHandler = artifactStreamHelperMap.get(subType);
        break;
      case COMMAND:
        yamlHandler = commandYamlHandler;
        break;
      case COMMAND_UNIT:
        yamlHandler = commandUnitYamlHandlerMap.get(subType);
        break;
      case CONFIG_FILE:
        yamlHandler = configFileYamlHandler;
        break;
      case ENVIRONMENT:
        yamlHandler = environmentYamlHandler;
        break;
      case CONFIG_FILE_OVERRIDE:
        yamlHandler = configFileOverrideYamlHandler;
        break;
      case INFRA_MAPPING:
        yamlHandler = infraMappingHelperMap.get(subType);
        break;
      case PIPELINE:
        yamlHandler = pipelineYamlHandler;
        break;
      case PIPELINE_STAGE:
        yamlHandler = pipelineStageYamlHandler;
        break;
      case WORKFLOW:
        yamlHandler = workflowYamlHelperMap.get(subType);
        break;
      case PROVISIONER:
        yamlHandler = provisionerYamlHandlerMap.get(subType);
        break;
      case NAME_VALUE_PAIR:
        yamlHandler = nameValuePairYamlHandler;
        break;
      case PHASE:
        yamlHandler = workflowPhaseYamlHandler;
        break;
      case PHASE_STEP:
        yamlHandler = phaseStepYamlHandler;
        break;
      case STEP:
        yamlHandler = stepYamlHandler;
        break;
      case TEMPLATE_EXPRESSION:
        yamlHandler = templateExpressionYamlHandler;
        break;
      case VARIABLE:
        yamlHandler = variableYamlHandler;
        break;
      case NOTIFICATION_RULE:
        yamlHandler = notificationRulesYamlHandler;
        break;
      case NOTIFICATION_GROUP:
        yamlHandler = notificationGroupYamlHandler;
        break;
      case FAILURE_STRATEGY:
        yamlHandler = failureStrategyYamlHandler;
        break;
      case CONTAINER_DEFINITION:
        yamlHandler = containerDefinitionYamlHandler;
        break;
      case DEPLOYMENT_SPECIFICATION:
        yamlHandler = deploymentSpecYamlHandlerMap.get(subType);
        break;
      case PORT_MAPPING:
        yamlHandler = portMappingYamlHandler;
        break;
      case STORAGE_CONFIGURATION:
        yamlHandler = storageConfigurationYamlHandler;
        break;
      case LOG_CONFIGURATION:
        yamlHandler = logConfigurationYamlHandler;
        break;
      case DEFAULT_SPECIFICATION:
        yamlHandler = defaultSpecificationYamlHandler;
        break;
      case FUNCTION_SPECIFICATION:
        yamlHandler = functionSpecificationYamlHandler;
        break;
      case ACCOUNT_DEFAULTS:
      case APPLICATION_DEFAULTS:
        yamlHandler = defaultsYamlHandler;
        break;
      default:
        break;
    }

    if (yamlHandler == null) {
      String msg = "Yaml handler not found for yaml type: " + yamlType + " and subType: " + subType;
      logger.error(msg);
      throw new WingsException(msg);
    }

    return (T) yamlHandler;
  }

  public <T> YamlType obtainEntityYamlType(T entity) {
    if (entity instanceof Environment) {
      return YamlType.ENVIRONMENT;
    } else if (entity instanceof NotificationGroup) {
      return YamlType.NOTIFICATION_GROUP;
    } else if (entity instanceof Pipeline) {
      return YamlType.PIPELINE;
    } else if (entity instanceof Application) {
      return YamlType.APPLICATION;
    } else if (entity instanceof InfrastructureMapping) {
      return YamlType.INFRA_MAPPING;
    } else if (entity instanceof Workflow) {
      return YamlType.WORKFLOW;
    } else if (entity instanceof InfrastructureProvisioner) {
      return YamlType.PROVISIONER;
    } else if (entity instanceof Service) {
      return YamlType.SERVICE;
    } else if (entity instanceof HelmChartSpecification) {
      return YamlType.DEPLOYMENT_SPECIFICATION;
    } else if (entity instanceof PcfServiceSpecification) {
      return YamlType.DEPLOYMENT_SPECIFICATION;
    } else if (entity instanceof LambdaSpecification) {
      return YamlType.DEPLOYMENT_SPECIFICATION;
    } else if (entity instanceof UserDataSpecification) {
      return YamlType.DEPLOYMENT_SPECIFICATION;
    } else if (entity instanceof EcsContainerTask) {
      return YamlType.DEPLOYMENT_SPECIFICATION;
    } else if (entity instanceof KubernetesContainerTask) {
      return YamlType.DEPLOYMENT_SPECIFICATION;
    } else if (entity instanceof EcsServiceSpecification) {
      return YamlType.DEPLOYMENT_SPECIFICATION;
    }

    throw new InvalidRequestException(
        "Unhandled case while getting yaml type for entity type " + entity.getClass().getSimpleName());
  }

  public <T> String obtainEntityName(T entity) {
    if (entity instanceof Environment) {
      return ((Environment) entity).getName();
    } else if (entity instanceof NotificationGroup) {
      return ((NotificationGroup) entity).getName();
    } else if (entity instanceof Pipeline) {
      return ((Pipeline) entity).getName();
    } else if (entity instanceof Application) {
      return ((Application) entity).getName();
    } else if (entity instanceof InfrastructureMapping) {
      return ((InfrastructureMapping) entity).getName();
    } else if (entity instanceof Workflow) {
      return ((Workflow) entity).getName();
    } else if (entity instanceof InfrastructureProvisioner) {
      return ((InfrastructureProvisioner) entity).getName();
    } else if (entity instanceof ArtifactStream) {
      return ((ArtifactStream) entity).getName();
    } else if (entity instanceof Service) {
      return ((Service) entity).getName();
    } else if (entity instanceof HelmChartSpecification) {
      return YamlConstants.HELM_CHART_YAML_FILE_NAME;
    } else if (entity instanceof PcfServiceSpecification) {
      return YamlConstants.PCF_MANIFEST_YAML_FILE_NAME;
    } else if (entity instanceof LambdaSpecification) {
      return YamlConstants.LAMBDA_SPEC_YAML_FILE_NAME;
    } else if (entity instanceof UserDataSpecification) {
      return YamlConstants.USER_DATA_SPEC_YAML_FILE_NAME;
    } else if (entity instanceof EcsContainerTask) {
      return YamlConstants.ECS_CONTAINER_TASK_YAML_FILE_NAME;
    } else if (entity instanceof EcsServiceSpecification) {
      return YamlConstants.ECS_SERVICE_SPEC_YAML_FILE_NAME;
    } else if (entity instanceof KubernetesContainerTask) {
      return YamlConstants.KUBERNETES_CONTAINER_TASK_YAML_FILE_NAME;
    } else if (entity instanceof ConfigFile) {
      return ((ConfigFile) entity).getRelativeFilePath();
    } else if (entity instanceof SettingAttribute) {
      return ((SettingAttribute) entity).getName();
    } else if (entity instanceof ServiceCommand) {
      return ((ServiceCommand) entity).getName();
    }

    throw new InvalidRequestException(
        "Unhandled case while getting yaml name for entity type " + entity.getClass().getSimpleName());
  }

  public <T> String obtainYamlFileName(T entity) {
    String entityName = entity.getClass().getSimpleName();

    if (nonLeafEntities.contains(entityName)) {
      return YamlConstants.INDEX;
    } else if (leafEntities.contains(entityName)) {
      return obtainEntityName(entity);
    }

    throw new InvalidRequestException(
        "Unhandled entity while getting yaml file name " + entity.getClass().getSimpleName());
  }

  public <T> boolean isNonLeafEntity(T entity) {
    String entityName = entity.getClass().getSimpleName();

    if (nonLeafEntities.contains(entityName)) {
      return true;
    } else if (leafEntities.contains(entityName)) {
      return false;
    }

    throw new InvalidRequestException("Unhandled case while verifying if its a leaf or non leaf entity for entity type"
        + entity.getClass().getSimpleName());
  }

  public <T> String obtainYamlHandlerSubtype(T entity) {
    if (entity instanceof Workflow) {
      return ((Workflow) entity).getOrchestrationWorkflow().getOrchestrationWorkflowType().name();
    } else if (entity instanceof InfrastructureMapping) {
      return ((InfrastructureMapping) entity).getInfraMappingType();
    } else if (entity instanceof InfrastructureProvisioner) {
      return ((InfrastructureProvisioner) entity).getInfrastructureProvisionerType();
    } else if (entity instanceof HelmChartSpecification) {
      return DeploymentType.HELM.name();
    } else if (entity instanceof PcfServiceSpecification) {
      return DeploymentType.PCF.name();
    } else if (entity instanceof LambdaSpecification) {
      return DeploymentType.AWS_LAMBDA.name();
    } else if (entity instanceof UserDataSpecification) {
      return DeploymentType.AMI.name();
    } else if (entity instanceof EcsContainerTask) {
      return DeploymentType.ECS.name();
    } else if (entity instanceof EcsServiceSpecification) {
      return ECS_SERVICE_SPEC;
    } else if (entity instanceof KubernetesContainerTask) {
      return DeploymentType.KUBERNETES.name();
    }

    return null;
  }

  private static List<String> obtainNonLeafEntities() {
    return Lists.newArrayList("Environment", "Application", "Service");
  }

  private static List<String> obtainLeafEntities() {
    return Lists.newArrayList("NotificationGroup", "Pipeline", "InfrastructureMapping", "PhysicalInfrastructureMapping",
        "PhysicalInfrastructureMappingWinRm", "", "Workflow", "PcfInfrastructureMapping",
        "GcpKubernetesInfrastructureMapping", "EcsInfrastructureMapping", "DirectKubernetesInfrastructureMapping",
        "CodeDeployInfrastructureMapping", "AzureKubernetesInfrastructureMapping", "AwsLambdaInfraStructureMapping",
        "AwsInfrastructureMapping", "AwsAmiInfrastructureMapping", "ArtifactStream", "InfrastructureProvisioner",
        "TerraformInfrastructureProvisioner", "CloudFormationInfrastructureProvisioner", "JenkinsArtifactStream",
        "NexusArtifactStream", "GcsArtifactStream", "GcrArtifactStream", "EcrArtifactStream", "DockerArtifactStream",
        "BambooArtifactStream", "ArtifactoryArtifactStream", "AmiArtifactStream", "AmazonS3ArtifactStream",
        "AcrArtifactStream", "HelmChartSpecification", "EcsServiceSpecification", "PcfServiceSpecification",
        "LambdaSpecification", "UserDataSpecification", "EcsContainerTask", "KubernetesContainerTask", "ConfigFile",
        "SettingAttribute", "ServiceCommand");
  }
}

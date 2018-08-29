package software.wings.service.impl.yaml.handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Environment;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
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

import java.util.Map;

/**
 * @author rktummala on 10/19/17
 */
@Singleton
public class YamlHandlerFactory {
  private static final Logger logger = LoggerFactory.getLogger(YamlHandlerFactory.class);

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
    } else {
      throw new InvalidRequestException(
          "Unhandled case while getting yaml type for entity type " + entity.getClass().getSimpleName());
    }
  }

  public <T> String obtainEntityName(T entity) {
    if (entity instanceof Environment) {
      return ((Environment) entity).getName();
    } else {
      throw new InvalidRequestException(
          "Unhandled case while getting yaml name for entity type " + entity.getClass().getSimpleName());
    }
  }
}

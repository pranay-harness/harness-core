package software.wings.service.impl.yaml.handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

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
  @Inject private Map<String, ArtifactStreamYamlHandler> artifactStreamHelperMap;
  @Inject private Map<String, InfraMappingYamlHandler> infraMappingHelperMap;
  @Inject private Map<String, WorkflowYamlHandler> workflowYamlHelperMap;
  @Inject private Map<String, CommandUnitYamlHandler> commandUnitYamlHandlerMap;
  @Inject private Map<String, DeploymentSpecificationYamlHandler> deploymentSpecYamlHandlerMap;
  @Inject private Map<String, ArtifactServerYamlHandler> artifactServerYamlHelperMap;
  @Inject private Map<String, VerificationProviderYamlHandler> verificationProviderYamlHelperMap;
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
    switch (yamlType) {
      case CLOUD_PROVIDER:
        return (T) cloudProviderYamlHelperMap.get(subType);
      case ARTIFACT_SERVER:
        return (T) artifactServerYamlHelperMap.get(subType);
      case COLLABORATION_PROVIDER:
        return (T) collaborationProviderYamlHelperMap.get(subType);
      case LOADBALANCER_PROVIDER:
        return (T) elbConfigYamlHandler;
      case VERIFICATION_PROVIDER:
        return (T) verificationProviderYamlHelperMap.get(subType);
      case APPLICATION:
        return (T) applicationYamlHandler;
      case SERVICE:
        return (T) serviceYamlHandler;
      case ARTIFACT_STREAM:
        return (T) artifactStreamHelperMap.get(subType);
      case COMMAND:
        return (T) commandYamlHandler;
      case COMMAND_UNIT:
        return (T) commandUnitYamlHandlerMap.get(subType);
      case CONFIG_FILE:
        return (T) configFileYamlHandler;
      case ENVIRONMENT:
        return (T) environmentYamlHandler;
      case CONFIG_FILE_OVERRIDE:
        return (T) configFileOverrideYamlHandler;
      case INFRA_MAPPING:
        return (T) infraMappingHelperMap.get(subType);
      case PIPELINE:
        return (T) pipelineYamlHandler;
      case PIPELINE_STAGE:
        return (T) pipelineStageYamlHandler;
      case WORKFLOW:
        return (T) workflowYamlHelperMap.get(subType);
      case NAME_VALUE_PAIR:
        return (T) nameValuePairYamlHandler;
      case PHASE:
        return (T) workflowPhaseYamlHandler;
      case PHASE_STEP:
        return (T) phaseStepYamlHandler;
      case STEP:
        return (T) stepYamlHandler;
      case TEMPLATE_EXPRESSION:
        return (T) templateExpressionYamlHandler;
      case VARIABLE:
        return (T) variableYamlHandler;
      case NOTIFICATION_RULE:
        return (T) notificationRulesYamlHandler;
      case NOTIFICATION_GROUP:
        return (T) notificationGroupYamlHandler;
      case FAILURE_STRATEGY:
        return (T) failureStrategyYamlHandler;
      case CONTAINER_DEFINITION:
        return (T) containerDefinitionYamlHandler;
      case DEPLOYMENT_SPECIFICATION:
        return (T) deploymentSpecYamlHandlerMap.get(subType);
      case PORT_MAPPING:
        return (T) portMappingYamlHandler;
      case STORAGE_CONFIGURATION:
        return (T) storageConfigurationYamlHandler;
      case LOG_CONFIGURATION:
        return (T) logConfigurationYamlHandler;
      case DEFAULT_SPECIFICATION:
        return (T) defaultSpecificationYamlHandler;
      case FUNCTION_SPECIFICATION:
        return (T) functionSpecificationYamlHandler;
      case ACCOUNT_DEFAULTS:
      case APPLICATION_DEFAULTS:
        return (T) defaultsYamlHandler;
      default:
        break;
    }

    return null;
  }
}

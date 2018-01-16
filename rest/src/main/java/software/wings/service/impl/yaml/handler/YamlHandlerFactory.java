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

  // TODO change the return type to generics so that we don't have to explicitly downcast
  public BaseYamlHandler getYamlHandler(YamlType yamlType, String subType) {
    switch (yamlType) {
      case CLOUD_PROVIDER:
        return cloudProviderYamlHelperMap.get(subType);
      case ARTIFACT_SERVER:
        return artifactServerYamlHelperMap.get(subType);
      case COLLABORATION_PROVIDER:
        return collaborationProviderYamlHelperMap.get(subType);
      case LOADBALANCER_PROVIDER:
        return elbConfigYamlHandler;
      case VERIFICATION_PROVIDER:
        return verificationProviderYamlHelperMap.get(subType);
      case APPLICATION:
        return applicationYamlHandler;
      case SERVICE:
        return serviceYamlHandler;
      case ARTIFACT_STREAM:
        return artifactStreamHelperMap.get(subType);
      case COMMAND:
        return commandYamlHandler;
      case COMMAND_UNIT:
        return commandUnitYamlHandlerMap.get(subType);
      case CONFIG_FILE:
        return configFileYamlHandler;
      case ENVIRONMENT:
        return environmentYamlHandler;
      case CONFIG_FILE_OVERRIDE:
        return configFileOverrideYamlHandler;
      case INFRA_MAPPING:
        return infraMappingHelperMap.get(subType);
      case PIPELINE:
        return pipelineYamlHandler;
      case PIPELINE_STAGE:
        return pipelineStageYamlHandler;
      case WORKFLOW:
        return workflowYamlHelperMap.get(subType);
      case NAME_VALUE_PAIR:
        return nameValuePairYamlHandler;
      case PHASE:
        return workflowPhaseYamlHandler;
      case PHASE_STEP:
        return phaseStepYamlHandler;
      case STEP:
        return stepYamlHandler;
      case TEMPLATE_EXPRESSION:
        return templateExpressionYamlHandler;
      case VARIABLE:
        return variableYamlHandler;
      case NOTIFICATION_RULE:
        return notificationRulesYamlHandler;
      case NOTIFICATION_GROUP:
        return notificationGroupYamlHandler;
      case FAILURE_STRATEGY:
        return failureStrategyYamlHandler;
      case CONTAINER_DEFINITION:
        return containerDefinitionYamlHandler;
      case DEPLOYMENT_SPECIFICATION:
        return deploymentSpecYamlHandlerMap.get(subType);
      case PORT_MAPPING:
        return portMappingYamlHandler;
      case STORAGE_CONFIGURATION:
        return storageConfigurationYamlHandler;
      case LOG_CONFIGURATION:
        return logConfigurationYamlHandler;
      case DEFAULT_SPECIFICATION:
        return defaultSpecificationYamlHandler;
      case FUNCTION_SPECIFICATION:
        return functionSpecificationYamlHandler;
      default:
        break;
    }

    return null;
  }
}

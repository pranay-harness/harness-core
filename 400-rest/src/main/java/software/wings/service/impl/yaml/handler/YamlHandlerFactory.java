/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;

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
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.Workflow;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.container.EcsContainerTask;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.beans.container.PcfServiceSpecification;
import software.wings.beans.container.UserDataSpecification;
import software.wings.beans.governance.GovernanceConfig;
import software.wings.beans.template.Template;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.TriggerArtifactVariable;
import software.wings.beans.trigger.WebhookSource.WebhookEvent;
import software.wings.beans.yaml.YamlConstants;
import software.wings.beans.yaml.YamlType;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.impl.yaml.handler.CloudProviderInfrastructure.CloudProviderInfrastructureYamlHandler;
import software.wings.service.impl.yaml.handler.InfraDefinition.InfrastructureDefinitionYamlHandler;
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
import software.wings.service.impl.yaml.handler.eventConfig.EventConfigYamlHandler;
import software.wings.service.impl.yaml.handler.governance.ApplicationFilterYamlHandler;
import software.wings.service.impl.yaml.handler.governance.CustomAppFilterYamlHandler;
import software.wings.service.impl.yaml.handler.governance.CustomEnvFilterYamlHandler;
import software.wings.service.impl.yaml.handler.governance.EnvironmentFilterYamlHandler;
import software.wings.service.impl.yaml.handler.governance.GovernanceConfigYamlHandler;
import software.wings.service.impl.yaml.handler.governance.GovernanceFreezeConfigYamlHandler;
import software.wings.service.impl.yaml.handler.governance.ServiceFilterYamlHandler;
import software.wings.service.impl.yaml.handler.governance.TimeRangeBasedFreezeConfigYamlHandler;
import software.wings.service.impl.yaml.handler.inframapping.InfraMappingYamlHandler;
import software.wings.service.impl.yaml.handler.infraprovisioner.InfrastructureProvisionerYamlHandler;
import software.wings.service.impl.yaml.handler.notification.NotificationGroupYamlHandler;
import software.wings.service.impl.yaml.handler.notification.NotificationRulesYamlHandler;
import software.wings.service.impl.yaml.handler.service.ApplicationManifestYamlHandler;
import software.wings.service.impl.yaml.handler.service.ManifestFileYamlHandler;
import software.wings.service.impl.yaml.handler.service.ServiceYamlHandler;
import software.wings.service.impl.yaml.handler.setting.artifactserver.ArtifactServerYamlHandler;
import software.wings.service.impl.yaml.handler.setting.artifactserver.AzureArtifactsYamlHandler;
import software.wings.service.impl.yaml.handler.setting.artifactserver.HelmRepoYamlHandler;
import software.wings.service.impl.yaml.handler.setting.cloudprovider.CloudProviderYamlHandler;
import software.wings.service.impl.yaml.handler.setting.collaborationprovider.CollaborationProviderYamlHandler;
import software.wings.service.impl.yaml.handler.setting.loadbalancer.ElasticLoadBalancerConfigYamlHandler;
import software.wings.service.impl.yaml.handler.setting.sourcerepoprovider.SourceRepoProviderYamlHandler;
import software.wings.service.impl.yaml.handler.setting.verificationprovider.VerificationProviderYamlHandler;
import software.wings.service.impl.yaml.handler.tag.HarnessTagYamlHandler;
import software.wings.service.impl.yaml.handler.template.TemplateExpressionYamlHandler;
import software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibraryYamlHandler;
import software.wings.service.impl.yaml.handler.trigger.ArtifactSelectionYamlHandler;
import software.wings.service.impl.yaml.handler.trigger.ManifestSelectionYamlHandler;
import software.wings.service.impl.yaml.handler.trigger.PayloadSourceYamlHandler;
import software.wings.service.impl.yaml.handler.trigger.TriggerConditionYamlHandler;
import software.wings.service.impl.yaml.handler.trigger.TriggerYamlHandler;
import software.wings.service.impl.yaml.handler.usagerestrictions.UsageRestrictionsYamlHandler;
import software.wings.service.impl.yaml.handler.variable.VariableYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.FailureStrategyYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.PhaseStepYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.PipelineStageYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.PipelineYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.StepSkipStrategyYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.StepYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.WorkflowPhaseYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.WorkflowYamlHandler;
import software.wings.verification.CVConfiguration;
import software.wings.verification.CVConfigurationYamlHandler;
import software.wings.yaml.templatelibrary.TemplateYamlConfig;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * @author rktummala on 10/19/17
 */
@Singleton
@Slf4j
public class YamlHandlerFactory {
  public static final String ECS_SERVICE_SPEC = "ECS_SERVICE_SPEC";
  private static final Set<String> nonLeafEntities = new HashSet(obtainNonLeafEntities());
  private static final Set<String> nonLeafEntitiesWithFeatureFlag = new HashSet(obtainNonLeafEntitiesWithFeatureFlag());
  private static final Set<String> entitiesWithActualFiles = new HashSet(obtainUseRealFileEntities());
  private static final Set<String> leafEntities = new HashSet<>(obtainLeafEntities());
  private static final Set<String> leafEntitiesWithFeatureFlag = new HashSet<>(obtainLeafEntitiesWithFeatureFlag());

  @Inject private Map<String, ArtifactStreamYamlHandler> artifactStreamHelperMap;
  @Inject private Map<String, InfraMappingYamlHandler> infraMappingHelperMap;
  @Inject private Map<String, WorkflowYamlHandler> workflowYamlHelperMap;
  @Inject private Map<String, TriggerConditionYamlHandler> triggerYamlHelperMapBinder;
  @Inject private Map<String, PayloadSourceYamlHandler> payloadSourceMapBinder;
  @Inject private Map<String, InfrastructureProvisionerYamlHandler> provisionerYamlHandlerMap;
  @Inject private Map<String, CommandUnitYamlHandler> commandUnitYamlHandlerMap;
  @Inject private Map<String, DeploymentSpecificationYamlHandler> deploymentSpecYamlHandlerMap;
  @Inject private Map<String, ArtifactServerYamlHandler> artifactServerYamlHelperMap;
  @Inject private Map<String, VerificationProviderYamlHandler> verificationProviderYamlHelperMap;
  @Inject private Map<String, CVConfigurationYamlHandler> cvConfigurationYamlHelperMap;
  @Inject private Map<String, InfrastructureProvisionerYamlHandler> infrastructureProvisionerYamlHandler;
  @Inject private Map<String, CollaborationProviderYamlHandler> collaborationProviderYamlHelperMap;
  @Inject private Map<String, SourceRepoProviderYamlHandler> sourceRepoProviderYamlHelperMap;
  @Inject private Map<String, CloudProviderYamlHandler> cloudProviderYamlHelperMap;
  @Inject private Map<String, HelmRepoYamlHandler> helmRepoYamlHelperMap;
  @Inject private Map<String, AzureArtifactsYamlHandler> azureArtifactsYamlHandlerMap;
  @Inject private Map<String, CloudProviderInfrastructureYamlHandler> cloudProviderInfrastructureYamlHandlerMap;
  @Inject private Map<String, TemplateLibraryYamlHandler> templateLibraryYamlHandlerMap;
  @Inject private Map<String, GovernanceFreezeConfigYamlHandler> governanceFreezeConfigYamlHandlerMap;
  @Inject private Map<String, ApplicationFilterYamlHandler> applicationFilterYamlHandlerMap;
  @Inject private Map<String, EnvironmentFilterYamlHandler> environmentFilterYamlHandlerMap;

  @Inject private ApplicationYamlHandler applicationYamlHandler;
  @Inject private TriggerYamlHandler triggerYamlHandler;
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
  @Inject private StepSkipStrategyYamlHandler stepSkipStrategyYamlHandler;
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
  @Inject private ArtifactSelectionYamlHandler artifactSelectionYamlHandler;
  @Inject private ApplicationManifestYamlHandler applicationManifestYamlHandler;
  @Inject private ManifestFileYamlHandler manifestFileYamlHandler;
  @Inject private UsageRestrictionsYamlHandler usageRestrictionsYamlHandler;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private HarnessTagYamlHandler harnessTagYamlHandler;
  @Inject private InfrastructureDefinitionYamlHandler infrastructureDefinitionYamlHandler;
  @Inject private ManifestSelectionYamlHandler manifestSelectionYamlHandler;
  @Inject private TimeRangeBasedFreezeConfigYamlHandler timeRangeBasedFreezeConfigYamlHandler;
  @Inject private GovernanceConfigYamlHandler governanceConfigYamlHandler;
  @Inject private CustomAppFilterYamlHandler customAppFilterYamlHandler;
  @Inject private CustomEnvFilterYamlHandler customEnvFilterYamlHandler;
  @Inject private EventConfigYamlHandler eventConfigYamlHandler;
  @Inject private ServiceFilterYamlHandler serviceFilterYamlHandler;

  public <T extends BaseYamlHandler> T getYamlHandler(YamlType yamlType) {
    return getYamlHandler(yamlType, null);
  }

  public <T extends BaseYamlHandler> T getYamlHandler(YamlType yamlType, String subType) {
    Object yamlHandler = null;
    switch (yamlType) {
      case CLOUD_PROVIDER:
      case CLOUD_PROVIDER_OVERRIDE:
        yamlHandler = cloudProviderYamlHelperMap.get(subType);
        break;
      case ARTIFACT_SERVER:
        if (SettingCategory.HELM_REPO.getSettingVariableTypes()
                .stream()
                .map(Enum::name)
                .collect(Collectors.toList())
                .contains(subType)) {
          yamlHandler = helmRepoYamlHelperMap.get(subType);
        } else if (SettingCategory.AZURE_ARTIFACTS.getSettingVariableTypes()
                       .stream()
                       .map(Enum::name)
                       .collect(Collectors.toList())
                       .contains(subType)) {
          yamlHandler = azureArtifactsYamlHandlerMap.get(subType);
        } else {
          yamlHandler = artifactServerYamlHelperMap.get(subType);
        }
        break;
      case ARTIFACT_SERVER_OVERRIDE:
        yamlHandler = artifactServerYamlHelperMap.get(subType);
        break;
      case COLLABORATION_PROVIDER:
        yamlHandler = collaborationProviderYamlHelperMap.get(subType);
        break;
      case SOURCE_REPO_PROVIDER:
        yamlHandler = sourceRepoProviderYamlHelperMap.get(subType);
        break;
      case LOADBALANCER_PROVIDER:
        yamlHandler = elbConfigYamlHandler;
        break;
      case VERIFICATION_PROVIDER:
        yamlHandler = verificationProviderYamlHelperMap.get(subType);
        break;
      case TRIGGER:
        yamlHandler = triggerYamlHandler;
        break;
      case APPLICATION:
        yamlHandler = applicationYamlHandler;
        break;
      case GLOBAL_TEMPLATE_LIBRARY:
      case APPLICATION_TEMPLATE_LIBRARY:
        yamlHandler = templateLibraryYamlHandlerMap.get(subType);
        break;
      case SERVICE:
        yamlHandler = serviceYamlHandler;
        break;
      case ARTIFACT_STREAM:
      case ARTIFACT_SERVER_ARTIFACT_STREAM_OVERRIDE:
      case CLOUD_PROVIDER_ARTIFACT_STREAM_OVERRIDE:
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
      case TRIGGER_CONDITION:
        yamlHandler = triggerYamlHelperMapBinder.get(subType);
        break;
      case PAYLOAD_SOURCE:
        yamlHandler = payloadSourceMapBinder.get(subType);
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
      case STEP_SKIP_STRATEGY:
        yamlHandler = stepSkipStrategyYamlHandler;
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
      case APPLICATION_MANIFEST:
      case APPLICATION_MANIFEST_APP_SERVICE:
      case APPLICATION_MANIFEST_VALUES_SERVICE_OVERRIDE:
      case APPLICATION_MANIFEST_VALUES_ENV_OVERRIDE:
      case APPLICATION_MANIFEST_VALUES_ENV_SERVICE_OVERRIDE:
      case APPLICATION_MANIFEST_PCF_ENV_SERVICE_OVERRIDE:
      case APPLICATION_MANIFEST_PCF_OVERRIDES_ALL_SERVICE:
      case APPLICATION_MANIFEST_HELM_OVERRIDES_ALL_SERVICE:
      case APPLICATION_MANIFEST_HELM_ENV_SERVICE_OVERRIDE:
      case APPLICATION_MANIFEST_OC_PARAMS_ENV_OVERRIDE:
      case APPLICATION_MANIFEST_OC_PARAMS_ENV_SERVICE_OVERRIDE:
      case APPLICATION_MANIFEST_OC_PARAMS_SERVICE_OVERRIDE:
      case APPLICATION_MANIFEST_KUSTOMIZE_PATCHES_ENV_OVERRIDE:
      case APPLICATION_MANIFEST_KUSTOMIZE_PATCHES_ENV_SERVICE_OVERRIDE:
      case APPLICATION_MANIFEST_KUSTOMIZE_PATCHES_SERVICE_OVERRIDE:
      case APPLICATION_MANIFEST_APP_SETTINGS_ENV_OVERRIDE:
      case APPLICATION_MANIFEST_APP_SETTINGS_ENV_SERVICE_OVERRIDE:
      case APPLICATION_MANIFEST_CONN_STRINGS_ENV_OVERRIDE:
      case APPLICATION_MANIFEST_CONN_STRINGS_ENV_SERVICE_OVERRIDE:
        yamlHandler = applicationManifestYamlHandler;
        break;
      case MANIFEST_FILE:
      case MANIFEST_FILE_VALUES_SERVICE_OVERRIDE:
      case MANIFEST_FILE_VALUES_ENV_OVERRIDE:
      case MANIFEST_FILE_VALUES_ENV_SERVICE_OVERRIDE:
      case MANIFEST_FILE_PCF_OVERRIDE_ENV_OVERRIDE:
      case MANIFEST_FILE_PCF_OVERRIDE_ENV_SERVICE_OVERRIDE:
      case MANIFEST_FILE_OC_PARAMS_ENV_OVERRIDE:
      case MANIFEST_FILE_OC_PARAMS_ENV_SERVICE_OVERRIDE:
      case MANIFEST_FILE_OC_PARAMS_SERVICE_OVERRIDE:
      case MANIFEST_FILE_KUSTOMIZE_PATCHES_ENV_OVERRIDE:
      case MANIFEST_FILE_KUSTOMIZE_PATCHES_ENV_SERVICE_OVERRIDE:
      case MANIFEST_FILE_KUSTOMIZE_PATCHES_SERVICE_OVERRIDE:
      case MANIFEST_FILE_APP_SETTINGS_ENV_OVERRIDE:
      case MANIFEST_FILE_APP_SERVICE:
      case MANIFEST_FILE_APP_SETTINGS_ENV_SERVICE_OVERRIDE:
      case MANIFEST_FILE_CONN_STRINGS_ENV_OVERRIDE:
      case MANIFEST_FILE_CONN_STRINGS_ENV_SERVICE_OVERRIDE:
        yamlHandler = manifestFileYamlHandler;
        break;
      case CV_CONFIGURATION:
        yamlHandler = cvConfigurationYamlHelperMap.get(subType);
        break;
      case ARTIFACT_SELECTION:
        yamlHandler = artifactSelectionYamlHandler;
        break;
      case CLOUD_PROVIDER_INFRASTRUCTURE:
        yamlHandler = cloudProviderInfrastructureYamlHandlerMap.get(subType);
        break;
      case INFRA_DEFINITION:
        yamlHandler = infrastructureDefinitionYamlHandler;
        break;

      case TAG:
        yamlHandler = harnessTagYamlHandler;
        break;

      case MANIFEST_SELECTION:
        yamlHandler = manifestSelectionYamlHandler;
        break;

      // insert map here
      case GOVERNANCE_FREEZE_CONFIG:
        yamlHandler = governanceFreezeConfigYamlHandlerMap.get(subType);
        break;

      case GOVERNANCE_CONFIG:
        yamlHandler = governanceConfigYamlHandler;
        break;

      // insert map here
      case APPLICATION_FILTER:
        yamlHandler = applicationFilterYamlHandlerMap.get(subType);
        break;
        // insert map here
      case ENV_FILTER:
        yamlHandler = environmentFilterYamlHandlerMap.get(subType);
        break;

      case EVENT_RULE:
        yamlHandler = eventConfigYamlHandler;
        break;

      case SERVICE_FILTER:
        yamlHandler = serviceFilterYamlHandler;
        break;
      default:
        break;
    }

    if (yamlHandler == null) {
      String msg = "Yaml handler not found for yaml type: " + yamlType + " and subType: " + subType;
      log.error(msg);
      throw new GeneralException(msg);
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
    } else if (entity instanceof InfrastructureDefinition) {
      return YamlType.INFRA_DEFINITION;
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
    } else if (entity instanceof CVConfiguration) {
      return YamlType.CV_CONFIGURATION;
    } else if (entity instanceof Trigger) {
      return YamlType.TRIGGER;
    } else if (entity instanceof TriggerArtifactVariable) {
      return YamlType.TRIGGER_ARTIFACT_VARIABLE;
    } else if (entity instanceof WebhookEvent) {
      return YamlType.WEBHOOK_EVENT;
    } else if (entity instanceof Template) {
      final String appId = ((Template) entity).getAppId();
      return TemplateYamlConfig.getInstance(appId).getYamlType();
    } else if (entity instanceof GovernanceConfig) {
      return YamlType.GOVERNANCE_CONFIG;
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
    } else if (entity instanceof InfrastructureDefinition) {
      return ((InfrastructureDefinition) entity).getName();
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
    } else if (entity instanceof ManifestFile) {
      return ((ManifestFile) entity).getFileName();
    } else if (entity instanceof ApplicationManifest) {
      String name = ((ApplicationManifest) entity).getName();
      return isNotBlank(name) ? name : YamlConstants.INDEX;
    } else if (entity instanceof CVConfiguration) {
      return ((CVConfiguration) entity).getName();
    } else if (entity instanceof Trigger) {
      return ((Trigger) entity).getName();
    } else if (entity instanceof Template) {
      return ((Template) entity).getName();
    } else if (entity instanceof GovernanceConfig) {
      return YamlConstants.DEPLOYMENT_GOVERNANCE_FOLDER;
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

  public <T> String obtainYamlFileNameWithFeatureFlag(T entity) {
    String entityName = entity.getClass().getSimpleName();

    if (nonLeafEntitiesWithFeatureFlag.contains(entityName)) {
      return YamlConstants.INDEX;
    } else if (leafEntitiesWithFeatureFlag.contains(entityName)) {
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

  public <T> boolean isNonLeafEntityWithFeatureFlag(T entity) {
    String entityName = entity.getClass().getSimpleName();

    if (nonLeafEntitiesWithFeatureFlag.contains(entityName)) {
      return true;
    } else if (leafEntitiesWithFeatureFlag.contains(entityName)) {
      return false;
    }

    throw new InvalidRequestException("Unhandled case while verifying if its a leaf or non leaf entity for entity type"
        + entity.getClass().getSimpleName());
  }

  public <T> boolean isEntityNeedsActualFile(T entity) {
    String entityName = entity.getClass().getSimpleName();

    if (entitiesWithActualFiles.contains(entityName)) {
      return true;
    } else {
      return false;
    }
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
    } else if (entity instanceof CVConfiguration) {
      return ((CVConfiguration) entity).getStateType().name();
    }

    return null;
  }

  private static List<String> obtainNonLeafEntities() {
    return Lists.newArrayList("Environment", "Application", "Service");
  }

  private static List<String> obtainNonLeafEntitiesWithFeatureFlag() {
    return Lists.newArrayList("Environment", "Application", "Service", "SettingAttribute");
  }

  private static List<String> obtainUseRealFileEntities() {
    return Lists.newArrayList("ManifestFile");
  }

  private static List<String> obtainLeafEntities() {
    return Lists.newArrayList("NotificationGroup", "Pipeline", "InfrastructureMapping", "PhysicalInfrastructureMapping",
        "PhysicalInfrastructureMappingWinRm", "", "Workflow", "Trigger", "PcfInfrastructureMapping",
        "GcpKubernetesInfrastructureMapping", "EcsInfrastructureMapping", "DirectKubernetesInfrastructureMapping",
        "CodeDeployInfrastructureMapping", "AzureKubernetesInfrastructureMapping", "AwsLambdaInfraStructureMapping",
        "AwsInfrastructureMapping", "AwsAmiInfrastructureMapping", "ArtifactStream", "InfrastructureProvisioner",
        "TerraformInfrastructureProvisioner", "CloudFormationInfrastructureProvisioner", "JenkinsArtifactStream",
        "NexusArtifactStream", "GcsArtifactStream", "SmbArtifactStream", "SftpArtifactStream", "GcrArtifactStream",
        "EcrArtifactStream", "DockerArtifactStream", "BambooArtifactStream", "ArtifactoryArtifactStream",
        "AmiArtifactStream", "AmazonS3ArtifactStream", "AcrArtifactStream", "HelmChartSpecification",
        "EcsServiceSpecification", "PcfServiceSpecification", "LambdaSpecification", "UserDataSpecification",
        "EcsContainerTask", "KubernetesContainerTask", "ConfigFile", "SettingAttribute", "ServiceCommand",
        "ManifestFile", "ApplicationManifest", "CustomArtifactStream", "APMCVServiceConfiguration",
        "AppDynamicsCVServiceConfiguration", "CloudWatchCVServiceConfiguration", "NewRelicCVServiceConfiguration",
        "DatadogCVServiceConfiguration", "DynaTraceCVServiceConfiguration", "DatadogLogCVConfiguration",
        "InstanaCVConfiguration", "PrometheusCVServiceConfiguration", "StackDriverMetricCVConfiguration",
        "BugsnagCVConfiguration", "ElkCVConfiguration", "LogsCVConfiguration", "SplunkCVConfiguration",
        "StackdriverCVConfiguration", "AzureInfrastructureMapping", "InfrastructureDefinition",
        "ShellScriptInfrastructureProvisioner", "Template", "CustomLogCVServiceConfiguration", "GovernanceConfig");
  }

  private static List<String> obtainLeafEntitiesWithFeatureFlag() {
    return Lists.newArrayList("NotificationGroup", "Pipeline", "InfrastructureMapping", "PhysicalInfrastructureMapping",
        "PhysicalInfrastructureMappingWinRm", "", "Workflow", "Trigger", "PcfInfrastructureMapping",
        "GcpKubernetesInfrastructureMapping", "EcsInfrastructureMapping", "DirectKubernetesInfrastructureMapping",
        "CodeDeployInfrastructureMapping", "AzureKubernetesInfrastructureMapping", "AwsLambdaInfraStructureMapping",
        "AwsInfrastructureMapping", "AwsAmiInfrastructureMapping", "ArtifactStream", "InfrastructureProvisioner",
        "TerraformInfrastructureProvisioner", "CloudFormationInfrastructureProvisioner", "JenkinsArtifactStream",
        "NexusArtifactStream", "GcsArtifactStream", "SmbArtifactStream", "SftpArtifactStream", "GcrArtifactStream",
        "EcrArtifactStream", "DockerArtifactStream", "BambooArtifactStream", "ArtifactoryArtifactStream",
        "AmiArtifactStream", "AmazonS3ArtifactStream", "AcrArtifactStream", "HelmChartSpecification",
        "EcsServiceSpecification", "PcfServiceSpecification", "LambdaSpecification", "UserDataSpecification",
        "EcsContainerTask", "KubernetesContainerTask", "ConfigFile", "ServiceCommand", "ManifestFile",
        "ApplicationManifest", "CustomArtifactStream", "APMCVServiceConfiguration", "AppDynamicsCVServiceConfiguration",
        "CloudWatchCVServiceConfiguration", "NewRelicCVServiceConfiguration", "DatadogCVServiceConfiguration",
        "DynaTraceCVServiceConfiguration", "DatadogLogCVConfiguration", "InstanaCVConfiguration",
        "PrometheusCVServiceConfiguration", "StackDriverMetricCVConfiguration", "BugsnagCVConfiguration",
        "ElkCVConfiguration", "LogsCVConfiguration", "SplunkCVConfiguration", "StackdriverCVConfiguration",
        "AzureInfrastructureMapping", "InfrastructureDefinition", "ShellScriptInfrastructureProvisioner", "Template",
        "CustomLogCVServiceConfiguration");
  }
}

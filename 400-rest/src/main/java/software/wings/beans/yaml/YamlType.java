/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.yaml;

import static software.wings.beans.EntityType.VERIFICATION_CONFIGURATION;
import static software.wings.beans.yaml.YamlConstants.ANY;
import static software.wings.beans.yaml.YamlConstants.ANY_EXCEPT_YAML;
import static software.wings.beans.yaml.YamlConstants.APPLICATIONS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.APPLICATION_TEMPLATE_LIBRARY_FOLDER;
import static software.wings.beans.yaml.YamlConstants.APP_SETTINGS_FILE;
import static software.wings.beans.yaml.YamlConstants.ARTIFACT_SERVERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.ARTIFACT_SOURCES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.ARTIFACT_STREAMS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.AZURE_APP_SETTINGS_OVERRIDES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.AZURE_CONN_STRINGS_OVERRIDES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.CG_EVENT_CONFIG_FOLDER;
import static software.wings.beans.yaml.YamlConstants.CLOUD_PROVIDERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.COLLABORATION_PROVIDERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.COMMANDS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.CONFIG_FILES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.CONN_STRINGS_FILE;
import static software.wings.beans.yaml.YamlConstants.CV_CONFIG_FOLDER;
import static software.wings.beans.yaml.YamlConstants.DEFAULTS_YAML;
import static software.wings.beans.yaml.YamlConstants.DEPLOYMENT_SPECIFICATION_FOLDER;
import static software.wings.beans.yaml.YamlConstants.ENVIRONMENTS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.GLOBAL_TEMPLATE_LIBRARY_FOLDER;
import static software.wings.beans.yaml.YamlConstants.HELM_CHART_OVERRIDE_FOLDER;
import static software.wings.beans.yaml.YamlConstants.INDEX_YAML;
import static software.wings.beans.yaml.YamlConstants.INFRA_DEFINITION_FOLDER;
import static software.wings.beans.yaml.YamlConstants.INFRA_MAPPING_FOLDER;
import static software.wings.beans.yaml.YamlConstants.KUSTOMIZE_PATCHES_FILE;
import static software.wings.beans.yaml.YamlConstants.KUSTOMIZE_PATCHES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.LOAD_BALANCERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.MANIFEST_FILE_EXPRESSION;
import static software.wings.beans.yaml.YamlConstants.MANIFEST_FILE_FOLDER;
import static software.wings.beans.yaml.YamlConstants.MANIFEST_FOLDER;
import static software.wings.beans.yaml.YamlConstants.MANIFEST_FOLDER_APP_SERVICE;
import static software.wings.beans.yaml.YamlConstants.MULTIPLE_ANY;
import static software.wings.beans.yaml.YamlConstants.NOTIFICATION_GROUPS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.OC_PARAMS_FILE;
import static software.wings.beans.yaml.YamlConstants.OC_PARAMS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;
import static software.wings.beans.yaml.YamlConstants.PCF_OVERRIDES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.PCF_YAML_EXPRESSION;
import static software.wings.beans.yaml.YamlConstants.PIPELINES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.PROVISIONERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.SERVICES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.SETUP_FOLDER;
import static software.wings.beans.yaml.YamlConstants.SOURCE_REPO_PROVIDERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.TAGS_YAML;
import static software.wings.beans.yaml.YamlConstants.TRIGGER_FOLDER;
import static software.wings.beans.yaml.YamlConstants.VALUES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.VALUES_YAML_KEY;
import static software.wings.beans.yaml.YamlConstants.VERIFICATION_PROVIDERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.WORKFLOWS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.YAML_EXPRESSION;
import static software.wings.utils.Utils.generatePath;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.CgEventConfig;
import io.harness.governance.ApplicationFilter;
import io.harness.governance.EnvironmentFilter;
import io.harness.governance.GovernanceFreezeConfig;
import io.harness.governance.ServiceFilter;

import software.wings.beans.Application;
import software.wings.beans.ConfigFile;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.FailureStrategy;
import software.wings.beans.GraphNode;
import software.wings.beans.HarnessTag;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.LambdaSpecification.DefaultSpecification;
import software.wings.beans.LambdaSpecification.FunctionSpecification;
import software.wings.beans.NameValuePair;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.ObjectType;
import software.wings.beans.PhaseStep;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.AbstractCommandUnit;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.container.ContainerDefinition;
import software.wings.beans.container.LogConfiguration;
import software.wings.beans.container.PortMapping;
import software.wings.beans.container.StorageConfiguration;
import software.wings.beans.defaults.Defaults;
import software.wings.beans.governance.GovernanceConfig;
import software.wings.beans.template.Template;
import software.wings.beans.trigger.ArtifactSelection;
import software.wings.beans.trigger.ManifestSelection;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.workflow.StepSkipStrategy;
import software.wings.infra.CloudProviderInfrastructure;
import software.wings.infra.InfrastructureDefinition;
import software.wings.security.UsageRestrictions;
import software.wings.settings.SettingValue;
import software.wings.verification.CVConfiguration;
import software.wings.yaml.trigger.PayloadSourceYaml;
import software.wings.yaml.trigger.TriggerArtifactSelectionValueYaml;
import software.wings.yaml.trigger.TriggerArtifactVariableYaml;
import software.wings.yaml.trigger.TriggerConditionYaml;
import software.wings.yaml.trigger.TriggerVariableYaml;
import software.wings.yaml.trigger.WebhookEventYaml;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author rktummala on 10/17/17
 */
@OwnedBy(HarnessTeam.DX)
public enum YamlType {
  CLOUD_PROVIDER(SettingCategory.CLOUD_PROVIDER.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, CLOUD_PROVIDERS_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, CLOUD_PROVIDERS_FOLDER, ANY), SettingAttribute.class),
  CLOUD_PROVIDER_OVERRIDE(SettingCategory.CLOUD_PROVIDER.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, CLOUD_PROVIDERS_FOLDER, ANY, INDEX_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, CLOUD_PROVIDERS_FOLDER, ANY), SettingAttribute.class),
  ARTIFACT_SERVER(YamlConstants.ARTIFACT_SERVER,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, ARTIFACT_SERVERS_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, ARTIFACT_SERVERS_FOLDER, ANY), SettingAttribute.class),
  ARTIFACT_SERVER_OVERRIDE(YamlConstants.ARTIFACT_SERVER,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, ARTIFACT_SERVERS_FOLDER, ANY, INDEX_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, ARTIFACT_SERVERS_FOLDER, ANY), SettingAttribute.class),
  COLLABORATION_PROVIDER(YamlConstants.COLLABORATION_PROVIDER,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, COLLABORATION_PROVIDERS_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, COLLABORATION_PROVIDERS_FOLDER, ANY), SettingAttribute.class),
  SOURCE_REPO_PROVIDER(YamlConstants.SOURCE_REPO_PROVIDER,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, SOURCE_REPO_PROVIDERS_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, SOURCE_REPO_PROVIDERS_FOLDER, ANY), SettingAttribute.class),
  LOADBALANCER_PROVIDER(YamlConstants.LOADBALANCER_PROVIDER,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, LOAD_BALANCERS_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, LOAD_BALANCERS_FOLDER, ANY), SettingAttribute.class),
  VERIFICATION_PROVIDER(YamlConstants.VERIFICATION_PROVIDER,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, VERIFICATION_PROVIDERS_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, VERIFICATION_PROVIDERS_FOLDER, ANY), SettingAttribute.class),
  APPLICATION(EntityType.APPLICATION.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, INDEX_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY), Application.class),
  SERVICE(EntityType.SERVICE.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, INDEX_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY), Service.class),
  APPLICATION_MANIFEST(EntityType.APPLICATION_MANIFEST.name(),
      generatePath(
          PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, MANIFEST_FOLDER, ANY),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, MANIFEST_FOLDER),
      Service.class),
  APPLICATION_MANIFEST_VALUES_SERVICE_OVERRIDE(YamlConstants.VALUES,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, VALUES_FOLDER,
          INDEX_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, VALUES_FOLDER),
      ApplicationManifest.class),

  // All Services
  APPLICATION_MANIFEST_VALUES_ENV_OVERRIDE(YamlConstants.VALUES,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          VALUES_FOLDER, INDEX_YAML),
      generatePath(
          PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY, VALUES_FOLDER),
      ApplicationManifest.class),
  // Service Specific
  APPLICATION_MANIFEST_VALUES_ENV_SERVICE_OVERRIDE(YamlConstants.VALUES,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          VALUES_FOLDER, SERVICES_FOLDER, ANY, INDEX_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          VALUES_FOLDER, SERVICES_FOLDER, ANY),
      ApplicationManifest.class),
  // Azure App Service Manifest
  APPLICATION_MANIFEST_APP_SERVICE(EntityType.APPLICATION_MANIFEST.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY,
          MANIFEST_FOLDER_APP_SERVICE, INDEX_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY,
          MANIFEST_FOLDER_APP_SERVICE),
      ApplicationManifest.class),
  // PCF Override All Services
  APPLICATION_MANIFEST_PCF_OVERRIDES_ALL_SERVICE(YamlConstants.VALUES,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          PCF_OVERRIDES_FOLDER, INDEX_YAML),
      generatePath(
          PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY, PCF_OVERRIDES_FOLDER),
      ApplicationManifest.class),
  // PCF Override Specific Services
  APPLICATION_MANIFEST_PCF_ENV_SERVICE_OVERRIDE(YamlConstants.VALUES,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          PCF_OVERRIDES_FOLDER, SERVICES_FOLDER, ANY, INDEX_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          PCF_OVERRIDES_FOLDER, SERVICES_FOLDER, ANY),
      ApplicationManifest.class),

  // Helm Override All Services
  APPLICATION_MANIFEST_HELM_OVERRIDES_ALL_SERVICE(EntityType.APPLICATION_MANIFEST.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          HELM_CHART_OVERRIDE_FOLDER, INDEX_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          HELM_CHART_OVERRIDE_FOLDER),
      ApplicationManifest.class),
  // Helm Env Service Override
  APPLICATION_MANIFEST_HELM_ENV_SERVICE_OVERRIDE(YamlConstants.VALUES,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          HELM_CHART_OVERRIDE_FOLDER, SERVICES_FOLDER, ANY, INDEX_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          HELM_CHART_OVERRIDE_FOLDER, SERVICES_FOLDER, ANY),
      ApplicationManifest.class),

  MANIFEST_FILE(YamlConstants.MANIFEST_FILE,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, MANIFEST_FOLDER,
          MANIFEST_FILE_FOLDER, MANIFEST_FILE_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, MANIFEST_FOLDER,
          MANIFEST_FILE_FOLDER),
      ManifestFile.class),
  MANIFEST_FILE_APP_SERVICE(YamlConstants.MANIFEST_FILE,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY,
          MANIFEST_FOLDER_APP_SERVICE, MANIFEST_FILE_FOLDER, ANY_EXCEPT_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY,
          MANIFEST_FOLDER_APP_SERVICE, MANIFEST_FILE_FOLDER),
      ManifestFile.class),
  MANIFEST_FILE_VALUES_SERVICE_OVERRIDE(YamlConstants.VALUES,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, VALUES_FOLDER,
          VALUES_YAML_KEY),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, VALUES_FOLDER),
      ManifestFile.class),
  MANIFEST_FILE_VALUES_ENV_OVERRIDE(YamlConstants.VALUES,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          VALUES_FOLDER, VALUES_YAML_KEY),
      generatePath(
          PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY, VALUES_FOLDER),
      ManifestFile.class),
  MANIFEST_FILE_VALUES_ENV_SERVICE_OVERRIDE(YamlConstants.VALUES,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          VALUES_FOLDER, SERVICES_FOLDER, ANY, VALUES_YAML_KEY),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          VALUES_FOLDER, SERVICES_FOLDER, ANY),
      ManifestFile.class),

  // PATCHES_INDEX_FILE
  APPLICATION_MANIFEST_KUSTOMIZE_PATCHES_SERVICE_OVERRIDE(YamlConstants.KUSTOMIZE_PATCHES_ENTITY,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY,
          KUSTOMIZE_PATCHES_FOLDER, INDEX_YAML),
      generatePath(
          PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, KUSTOMIZE_PATCHES_FOLDER),
      ApplicationManifest.class),

  APPLICATION_MANIFEST_KUSTOMIZE_PATCHES_ENV_OVERRIDE(YamlConstants.KUSTOMIZE_PATCHES_ENTITY,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          KUSTOMIZE_PATCHES_FOLDER, INDEX_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          KUSTOMIZE_PATCHES_FOLDER),
      ApplicationManifest.class),

  APPLICATION_MANIFEST_KUSTOMIZE_PATCHES_ENV_SERVICE_OVERRIDE(YamlConstants.KUSTOMIZE_PATCHES_ENTITY,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          KUSTOMIZE_PATCHES_FOLDER, SERVICES_FOLDER, ANY, INDEX_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          KUSTOMIZE_PATCHES_FOLDER, SERVICES_FOLDER, ANY),
      ApplicationManifest.class),

  // PATCHES_FILE
  MANIFEST_FILE_KUSTOMIZE_PATCHES_SERVICE_OVERRIDE(YamlConstants.KUSTOMIZE_PATCHES_ENTITY,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY,
          KUSTOMIZE_PATCHES_FOLDER, KUSTOMIZE_PATCHES_FILE),
      generatePath(
          PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, KUSTOMIZE_PATCHES_FOLDER),
      ManifestFile.class),
  MANIFEST_FILE_KUSTOMIZE_PATCHES_ENV_OVERRIDE(YamlConstants.KUSTOMIZE_PATCHES_ENTITY,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          KUSTOMIZE_PATCHES_FOLDER, KUSTOMIZE_PATCHES_FILE),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          KUSTOMIZE_PATCHES_FOLDER),
      ManifestFile.class),
  MANIFEST_FILE_KUSTOMIZE_PATCHES_ENV_SERVICE_OVERRIDE(YamlConstants.KUSTOMIZE_PATCHES_ENTITY,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          KUSTOMIZE_PATCHES_FOLDER, SERVICES_FOLDER, ANY, KUSTOMIZE_PATCHES_FILE),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          KUSTOMIZE_PATCHES_FOLDER, SERVICES_FOLDER, ANY),
      ManifestFile.class),

  // PARAMS_INDEX_FILE
  APPLICATION_MANIFEST_OC_PARAMS_SERVICE_OVERRIDE(YamlConstants.OC_PARAMS_ENTITY,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY,
          OC_PARAMS_FOLDER, INDEX_YAML),
      generatePath(
          PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, OC_PARAMS_FOLDER),
      ApplicationManifest.class),

  APPLICATION_MANIFEST_OC_PARAMS_ENV_OVERRIDE(YamlConstants.OC_PARAMS_ENTITY,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          OC_PARAMS_FOLDER, INDEX_YAML),
      generatePath(
          PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY, OC_PARAMS_FOLDER),
      ApplicationManifest.class),

  APPLICATION_MANIFEST_OC_PARAMS_ENV_SERVICE_OVERRIDE(YamlConstants.OC_PARAMS_ENTITY,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          OC_PARAMS_FOLDER, SERVICES_FOLDER, ANY, INDEX_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          OC_PARAMS_FOLDER, SERVICES_FOLDER, ANY),
      ApplicationManifest.class),

  // PARAMS_FILE
  MANIFEST_FILE_OC_PARAMS_SERVICE_OVERRIDE(YamlConstants.OC_PARAMS_ENTITY,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY,
          OC_PARAMS_FOLDER, OC_PARAMS_FILE),
      generatePath(
          PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, OC_PARAMS_FOLDER),
      ManifestFile.class),
  MANIFEST_FILE_OC_PARAMS_ENV_OVERRIDE(YamlConstants.OC_PARAMS_ENTITY,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          OC_PARAMS_FOLDER, OC_PARAMS_FILE),
      generatePath(
          PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY, OC_PARAMS_FOLDER),
      ManifestFile.class),
  MANIFEST_FILE_OC_PARAMS_ENV_SERVICE_OVERRIDE(YamlConstants.VALUES,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          OC_PARAMS_FOLDER, SERVICES_FOLDER, ANY, OC_PARAMS_FILE),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          OC_PARAMS_FOLDER, SERVICES_FOLDER, ANY),
      ManifestFile.class),
  // Azure App Settings and Conn Strings service and env overrides
  APPLICATION_MANIFEST_APP_SETTINGS_ENV_OVERRIDE(YamlConstants.APP_SETTINGS_ENTITY,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          AZURE_APP_SETTINGS_OVERRIDES_FOLDER, INDEX_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          AZURE_APP_SETTINGS_OVERRIDES_FOLDER),
      ApplicationManifest.class),
  APPLICATION_MANIFEST_APP_SETTINGS_ENV_SERVICE_OVERRIDE(YamlConstants.APP_SETTINGS_ENTITY,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          AZURE_APP_SETTINGS_OVERRIDES_FOLDER, SERVICES_FOLDER, ANY, INDEX_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          AZURE_APP_SETTINGS_OVERRIDES_FOLDER, SERVICES_FOLDER, ANY),
      ApplicationManifest.class),

  MANIFEST_FILE_APP_SETTINGS_ENV_OVERRIDE(YamlConstants.APP_SETTINGS_ENTITY,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          AZURE_APP_SETTINGS_OVERRIDES_FOLDER, APP_SETTINGS_FILE),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          AZURE_APP_SETTINGS_OVERRIDES_FOLDER),
      ManifestFile.class),
  MANIFEST_FILE_APP_SETTINGS_ENV_SERVICE_OVERRIDE(YamlConstants.VALUES,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          AZURE_APP_SETTINGS_OVERRIDES_FOLDER, SERVICES_FOLDER, ANY, APP_SETTINGS_FILE),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          AZURE_APP_SETTINGS_OVERRIDES_FOLDER, SERVICES_FOLDER, ANY),
      ManifestFile.class),

  APPLICATION_MANIFEST_CONN_STRINGS_ENV_OVERRIDE(YamlConstants.CONN_STRINGS_ENTITY,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          AZURE_CONN_STRINGS_OVERRIDES_FOLDER, INDEX_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          AZURE_CONN_STRINGS_OVERRIDES_FOLDER),
      ApplicationManifest.class),
  APPLICATION_MANIFEST_CONN_STRINGS_ENV_SERVICE_OVERRIDE(YamlConstants.CONN_STRINGS_ENTITY,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          AZURE_CONN_STRINGS_OVERRIDES_FOLDER, SERVICES_FOLDER, ANY, INDEX_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          AZURE_CONN_STRINGS_OVERRIDES_FOLDER, SERVICES_FOLDER, ANY),
      ApplicationManifest.class),

  MANIFEST_FILE_CONN_STRINGS_ENV_OVERRIDE(YamlConstants.CONN_STRINGS_ENTITY,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          AZURE_CONN_STRINGS_OVERRIDES_FOLDER, CONN_STRINGS_FILE),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          AZURE_CONN_STRINGS_OVERRIDES_FOLDER),
      ManifestFile.class),
  MANIFEST_FILE_CONN_STRINGS_ENV_SERVICE_OVERRIDE(YamlConstants.VALUES,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          AZURE_CONN_STRINGS_OVERRIDES_FOLDER, SERVICES_FOLDER, ANY, CONN_STRINGS_FILE),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          AZURE_CONN_STRINGS_OVERRIDES_FOLDER, SERVICES_FOLDER, ANY),
      ManifestFile.class),
  // This defines prefix and path expression for PCF Override yml files
  MANIFEST_FILE_PCF_OVERRIDE_ENV_OVERRIDE(YamlConstants.VALUES,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          PCF_OVERRIDES_FOLDER, PCF_YAML_EXPRESSION),
      generatePath(
          PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY, PCF_OVERRIDES_FOLDER),
      ManifestFile.class),
  // This defines prefix and path expression for PCF Override yml files
  MANIFEST_FILE_PCF_OVERRIDE_ENV_SERVICE_OVERRIDE(YamlConstants.VALUES,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          PCF_OVERRIDES_FOLDER, SERVICES_FOLDER, ANY, PCF_YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          PCF_OVERRIDES_FOLDER, SERVICES_FOLDER, ANY),
      ManifestFile.class),
  PROVISIONER(EntityType.PROVISIONER.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, PROVISIONERS_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, PROVISIONERS_FOLDER, ANY),
      InfrastructureProvisioner.class),
  ARTIFACT_STREAM(EntityType.ARTIFACT_STREAM.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY,
          ARTIFACT_SOURCES_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY,
          ARTIFACT_SOURCES_FOLDER, ANY),
      ArtifactStream.class),
  ARTIFACT_SERVER_ARTIFACT_STREAM_OVERRIDE(EntityType.ARTIFACT_STREAM.name(),
      generatePath(
          PATH_DELIMITER, false, SETUP_FOLDER, ARTIFACT_SERVERS_FOLDER, ANY, ARTIFACT_STREAMS_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, ARTIFACT_SERVERS_FOLDER, ANY, ARTIFACT_STREAMS_FOLDER, ANY),
      ArtifactStream.class),
  CLOUD_PROVIDER_ARTIFACT_STREAM_OVERRIDE(EntityType.ARTIFACT_STREAM.name(),
      generatePath(
          PATH_DELIMITER, false, SETUP_FOLDER, CLOUD_PROVIDERS_FOLDER, ANY, ARTIFACT_STREAMS_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, CLOUD_PROVIDERS_FOLDER, ANY, ARTIFACT_STREAMS_FOLDER, ANY),
      ArtifactStream.class),
  DEPLOYMENT_SPECIFICATION(YamlConstants.DEPLOYMENT_SPECIFICATION,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY,
          DEPLOYMENT_SPECIFICATION_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY,
          DEPLOYMENT_SPECIFICATION_FOLDER, ANY),
      DefaultSpecification.class),
  COMMAND(EntityType.COMMAND.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, COMMANDS_FOLDER,
          YAML_EXPRESSION),
      generatePath(
          PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, COMMANDS_FOLDER, ANY),
      ServiceCommand.class),
  CONFIG_FILE_CONTENT(YamlConstants.CONFIG_FILE_CONTENT,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY,
          CONFIG_FILES_FOLDER, ANY_EXCEPT_YAML),
      generatePath(
          PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, CONFIG_FILES_FOLDER, ANY),
      ConfigFile.class),
  CONFIG_FILE(EntityType.CONFIG.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY,
          CONFIG_FILES_FOLDER, YAML_EXPRESSION),
      generatePath(
          PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, CONFIG_FILES_FOLDER, ANY),
      ConfigFile.class),

  GOVERNANCE_FREEZE_CONFIG(EntityType.GOVERNANCE_FREEZE_CONFIG.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, YamlConstants.GOVERNANCE_FOLDER,
          YamlConstants.DEPLOYMENT_GOVERNANCE_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, YamlConstants.GOVERNANCE_FOLDER,
          YamlConstants.DEPLOYMENT_GOVERNANCE_FOLDER, ANY),
      GovernanceFreezeConfig.class),

  GOVERNANCE_CONFIG(EntityType.GOVERNANCE_CONFIG.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, YamlConstants.GOVERNANCE_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, YamlConstants.GOVERNANCE_FOLDER, ANY), GovernanceConfig.class),

  ENVIRONMENT(EntityType.ENVIRONMENT.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY, INDEX_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY),
      Environment.class),
  CONFIG_FILE_OVERRIDE_CONTENT(YamlConstants.CONFIG_FILE_OVERRIDE_CONTENT,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          CONFIG_FILES_FOLDER, ANY, ANY_EXCEPT_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          CONFIG_FILES_FOLDER, ANY, ANY),
      ConfigFile.class),
  CONFIG_FILE_OVERRIDE(EntityType.CONFIG.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          CONFIG_FILES_FOLDER, ANY, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          CONFIG_FILES_FOLDER, ANY, ANY),
      ConfigFile.class),
  INFRA_MAPPING(EntityType.INFRASTRUCTURE_MAPPING.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          INFRA_MAPPING_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          INFRA_MAPPING_FOLDER, ANY),
      InfrastructureMapping.class),
  INFRA_DEFINITION(EntityType.INFRASTRUCTURE_DEFINITION.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          INFRA_DEFINITION_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          INFRA_DEFINITION_FOLDER, ANY),
      InfrastructureDefinition.class),
  WORKFLOW(EntityType.WORKFLOW.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, WORKFLOWS_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, WORKFLOWS_FOLDER, ANY),
      Workflow.class),
  TRIGGER(EntityType.TRIGGER.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, TRIGGER_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, TRIGGER_FOLDER, ANY), Trigger.class),

  PIPELINE(EntityType.PIPELINE.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, PIPELINES_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, PIPELINES_FOLDER, ANY),
      Pipeline.class),

  GLOBAL_TEMPLATE_LIBRARY(EntityType.TEMPLATE.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, GLOBAL_TEMPLATE_LIBRARY_FOLDER, MULTIPLE_ANY, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, GLOBAL_TEMPLATE_LIBRARY_FOLDER), Template.class),

  EVENT_RULE(EntityType.EVENT_RULE.name(),
      generatePath(
          PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, CG_EVENT_CONFIG_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, CG_EVENT_CONFIG_FOLDER, ANY),
      CgEventConfig.class),

  APPLICATION_TEMPLATE_LIBRARY(EntityType.TEMPLATE.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, APPLICATION_TEMPLATE_LIBRARY_FOLDER,
          MULTIPLE_ANY, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, APPLICATION_TEMPLATE_LIBRARY_FOLDER),
      Template.class),

  CV_CONFIGURATION(VERIFICATION_CONFIGURATION.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          CV_CONFIG_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          CV_CONFIG_FOLDER, ANY),
      CVConfiguration.class),

  // TODO: Remove these two deprecated YamlType.
  CONFIG_FILE_OVERRIDE_CONTENT_DEPRECATED(YamlConstants.CONFIG_FILE_OVERRIDE_CONTENT,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          CONFIG_FILES_FOLDER, ANY_EXCEPT_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          CONFIG_FILES_FOLDER, ANY),
      ConfigFile.class),
  CONFIG_FILE_OVERRIDE_DEPRECATED(EntityType.CONFIG.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          CONFIG_FILES_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          CONFIG_FILES_FOLDER, ANY),
      ConfigFile.class),
  // Some of these bean classes are embedded within other entities and don't have an yaml file path
  NAME_VALUE_PAIR(ObjectType.NAME_VALUE_PAIR, "", "", NameValuePair.class),
  PHASE(ObjectType.PHASE, "", "", WorkflowPhase.class),
  PHASE_STEP(ObjectType.PHASE_STEP, "", "", PhaseStep.class),
  TEMPLATE_EXPRESSION(ObjectType.TEMPLATE_EXPRESSION, "", "", TemplateExpression.class),
  VARIABLE(ObjectType.VARIABLE, "", "", Variable.class),
  STEP(ObjectType.STEP, "", "", GraphNode.class),
  FAILURE_STRATEGY(ObjectType.FAILURE_STRATEGY, "", "", FailureStrategy.class),
  STEP_SKIP_STRATEGY(ObjectType.STEP_SKIP_STRATEGY, "", "", StepSkipStrategy.class),
  NOTIFICATION_RULE(ObjectType.NOTIFICATION_RULE, "", "", NotificationRule.class),
  PIPELINE_STAGE(ObjectType.PIPELINE_STAGE, "", "", PipelineStage.class),
  NOTIFICATION_GROUP(ObjectType.NOTIFICATION_GROUP,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, NOTIFICATION_GROUPS_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, NOTIFICATION_GROUPS_FOLDER, ANY), NotificationGroup.class),
  COMMAND_UNIT(ObjectType.COMMAND_UNIT, "", "", AbstractCommandUnit.class),
  CONTAINER_DEFINITION(ObjectType.CONTAINER_DEFINITION, "", "", ContainerDefinition.class),
  LOG_CONFIGURATION(ObjectType.LOG_CONFIGURATION, "", "", LogConfiguration.class),
  PORT_MAPPING(ObjectType.PORT_MAPPING, "", "", PortMapping.class),
  STORAGE_CONFIGURATION(ObjectType.STORAGE_CONFIGURATION, "", "", StorageConfiguration.class),
  DEFAULT_SPECIFICATION(ObjectType.DEFAULT_SPECIFICATION, "", "", DefaultSpecification.class),
  FUNCTION_SPECIFICATION(ObjectType.FUNCTION_SPECIFICATION, "", "", FunctionSpecification.class),
  SETTING_ATTRIBUTE(ObjectType.SETTING_ATTRIBUTE, "", "", SettingAttribute.class),
  SETTING_VALUE(ObjectType.SETTING_VALUE, "", "", SettingValue.class),
  APPLICATION_DEFAULTS(ObjectType.APPLICATION_DEFAULTS,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, DEFAULTS_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY), Defaults.class),
  ACCOUNT_DEFAULTS(ObjectType.ACCOUNT_DEFAULTS, generatePath(PATH_DELIMITER, false, SETUP_FOLDER, DEFAULTS_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, ANY), Defaults.class),
  USAGE_RESTRICTIONS(ObjectType.USAGE_RESTRICTIONS, "", "", UsageRestrictions.class),
  TRIGGER_CONDITION(ObjectType.TRIGGER_CONDITION, "", "", TriggerConditionYaml.class),
  PAYLOAD_SOURCE(ObjectType.PAYLOAD_SOURCE, "", "", PayloadSourceYaml.class),
  TRIGGER_ARTIFACT_VARIABLE(ObjectType.TRIGGER_ARTIFACT_VARIABLE, "", "", TriggerArtifactVariableYaml.class),
  WEBHOOK_EVENT(ObjectType.WEBHOOK_EVENT, "", "", WebhookEventYaml.class),
  TRIGGER_VARIABLE(ObjectType.TRIGGER_VARIABLE, "", "", TriggerVariableYaml.class),
  TRIGGER_ARTIFACT_VALUE(ObjectType.TRIGGER_ARTIFACT_VALUE, "", "", TriggerArtifactSelectionValueYaml.class),
  ARTIFACT_SELECTION(ObjectType.ARTIFACT_SELECTION, "", "", ArtifactSelection.Yaml.class),
  MANIFEST_SELECTION(ObjectType.MANIFEST_SELECTION, "", "", ManifestSelection.Yaml.class),
  TAG(EntityType.TAG.name(), generatePath(PATH_DELIMITER, false, SETUP_FOLDER, TAGS_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, ANY), HarnessTag.class),
  CLOUD_PROVIDER_INFRASTRUCTURE(ObjectType.CLOUD_PROVIDER_INFRASTRUCTURE, "", "", CloudProviderInfrastructure.class),
  APPLICATION_FILTER(ObjectType.APPLICATION_FILTER, "", "", ApplicationFilter.class),
  ENV_FILTER(ObjectType.ENVIRONMENT_FILTER, "", "", EnvironmentFilter.class),
  SERVICE_FILTER(ObjectType.SERVICE_FILTER, "", "", ServiceFilter.class);

  private String entityType;
  private String pathExpression;
  private String prefixExpression;
  private Class beanClass;

  YamlType(String entityType, String pathExpression, String prefixExpression, Class beanClass) {
    this.entityType = entityType;
    this.pathExpression = pathExpression;
    this.prefixExpression = prefixExpression;
    this.beanClass = beanClass;
  }

  public String getEntityType() {
    return entityType;
  }

  public String getPathExpression() {
    return pathExpression;
  }

  public String getPrefixExpression() {
    return prefixExpression;
  }

  public Class getBeanClass() {
    return beanClass;
  }

  public static List<YamlType> getYamlTypes(Class beanClass) {
    List<YamlType> yamlTypes = new ArrayList<>();
    for (YamlType yamlType : YamlType.values()) {
      if (yamlType.getBeanClass() == beanClass) {
        yamlTypes.add(yamlType);
      }
    }
    return yamlTypes;
  }

  private static EnumMap<YamlType, Pattern> yamlTypeCompiledPatternMap = new EnumMap<>(YamlType.class);

  public static Pattern getCompiledPatternForYamlTypePathExpression(YamlType yamlType) {
    return yamlTypeCompiledPatternMap.computeIfAbsent(
        yamlType, yamlType1 -> Pattern.compile(yamlType1.getPathExpression()));
  }
}

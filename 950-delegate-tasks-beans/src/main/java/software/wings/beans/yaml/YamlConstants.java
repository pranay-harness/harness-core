/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.yaml;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.google.common.collect.Lists;
import java.util.List;

/**
 * @author rktummala on 10/17/17
 */
@TargetModule(HarnessModule._957_CG_BEANS)
@OwnedBy(DX)
public interface YamlConstants {
  String PATH_DELIMITER = "/";
  String ANY = ".[^/]*?";
  String MULTIPLE_ANY = ".+";
  String MANIFEST_FILE_EXPRESSION = ".*";
  String YAML_EXPRESSION = ".[^/]*?\\.yaml";
  String PCF_YAML_EXPRESSION = ".[^/]*?\\.yml";
  String ANY_EXCEPT_YAML = ".[^/]*?[^\\.yaml]";
  String YAML_FILE_NAME_PATTERN = ".*?" + PATH_DELIMITER + YAML_EXPRESSION;
  //  String SETUP_ENTITY_ID = "setup";
  String SETUP_FOLDER = "Setup";
  String SETUP_FOLDER_PATH = "Setup/";
  String APPLICATIONS_FOLDER = "Applications";
  String SERVICES_FOLDER = "Services";
  String MANIFEST_FOLDER = "Manifests";
  String MANIFEST_FOLDER_APP_SERVICE = "App Service Settings";
  String MANIFEST_FILE_FOLDER = "Files";
  String MANIFEST_FILE = "MANIFEST_FILE";
  String APPLICATIONS_MANIFEST = "ApplicationManifest";
  String PROVISIONERS_FOLDER = "Provisioners";
  String COMMANDS_FOLDER = "Commands";
  String CONFIG_FILES_FOLDER = "Config Files";
  String ENVIRONMENTS_FOLDER = "Environments";
  String INFRA_MAPPING_FOLDER = "Service Infrastructure";
  String INFRA_DEFINITION_FOLDER = "Infrastructure Definitions";
  String CV_CONFIG_FOLDER = "Service Verification";
  String WORKFLOWS_FOLDER = "Workflows";
  String TRIGGER_FOLDER = "Triggers";
  String NEW_TRIGGER_FOLDER = "NewTriggers";

  String PIPELINES_FOLDER = "Pipelines";

  String CLOUD_PROVIDERS_FOLDER = "Cloud Providers";
  String ARTIFACT_SERVERS_FOLDER = "Artifact Servers";
  String COLLABORATION_PROVIDERS_FOLDER = "Collaboration Providers";
  String SOURCE_REPO_PROVIDERS_FOLDER = "Source Repo Providers";
  String VERIFICATION_PROVIDERS_FOLDER = "Verification Providers";
  String NOTIFICATION_GROUPS_FOLDER = "Notification Groups";
  String GLOBAL_TEMPLATE_LIBRARY_FOLDER = "Template Library";
  String APPLICATION_TEMPLATE_LIBRARY_FOLDER = "Template Library";

  String ARTIFACT_SOURCES_FOLDER = "Artifact Servers";
  String ARTIFACT_STREAMS_FOLDER = "Artifact Streams";
  String DEPLOYMENT_SPECIFICATION_FOLDER = "Deployment Specifications";

  String LOAD_BALANCERS_FOLDER = "Load Balancers";

  String YAML_EXTENSION = ".yaml";
  String INDEX_YAML = "Index.yaml";
  String DEFAULTS_YAML = "Defaults.yaml";
  String INDEX = "Index";
  String DEFAULTS = "Defaults";
  String ARTIFACT_SERVER = "ARTIFACT_SERVER";
  String COLLABORATION_PROVIDER = "COLLABORATION_PROVIDER";
  String SOURCE_REPO_PROVIDER = "SOURCE_REPO_PROVIDER";
  String LOADBALANCER_PROVIDER = "LOADBALANCER_PROVIDER";
  String VERIFICATION_PROVIDER = "VERIFICATION_PROVIDER";
  String CONFIG_FILE_CONTENT = "CONFIG_FILE_CONTENT";
  String CONFIG_FILE_OVERRIDE_CONTENT = "CONFIG_FILE_OVERRIDE_CONTENT";

  String DEPLOYMENT_SPECIFICATION = "DEPLOYMENT_SPECIFICATION";

  String LAMBDA_SPEC_YAML_FILE_NAME = "Lambda";
  String USER_DATA_SPEC_YAML_FILE_NAME = "UserData";
  String ECS_CONTAINER_TASK_YAML_FILE_NAME = "Ecs";
  String ECS_SERVICE_SPEC_YAML_FILE_NAME = "EcsServiceSpec";
  String KUBERNETES_CONTAINER_TASK_YAML_FILE_NAME = "Kubernetes";
  String HELM_CHART_YAML_FILE_NAME = "HelmChart";
  String PCF_MANIFEST_YAML_FILE_NAME = "PcfManifest";

  String LINK_PREFIX = "link";
  String NODE_PREFIX = "node";

  String NODE_PROPERTY_REFERENCEID = "referenceId";
  String NODE_PROPERTY_COMMAND_STRING = "commandString";
  String NODE_PROPERTY_TAIL_FILES = "tailFiles";
  String NODE_PROPERTY_TAIL_PATTERNS = "tailPatterns";
  String NODE_PROPERTY_COMMAND_TYPE = "commandType";
  String NODE_PROPERTY_COMMAND_PATH = "commandPath";
  String NODE_PROPERTY_FILE_CATEGORY = "fileCategory";
  String NODE_PROPERTY_DESTINATION_DIR_PATH = "destinationDirectoryPath";
  String NODE_PROPERTY_ARTIFACT_VARIABLE_NAME = "artifactVariableName";
  String NODE_PROPERTY_DESTINATION_PARENT_PATH = "destinationParentPath";
  String IS_ROLLBACK = "IS_ROLLBACK";

  String FIELD_HARNESS_API_VERSION = "harnessApiVersion";
  String FIELD_TYPE = "type";

  String GIT_YAML_LOG_PREFIX = "GIT_YAML_LOG_ENTRY: ";
  String GIT_TERRAFORM_LOG_PREFIX = "GIT_TERRAFORM_LOG_ENTRY: ";
  String GIT_TERRAGRUNT_LOG_PREFIX = "GIT_TERRAGRUNT_LOG_ENTRY: ";
  String GIT_TRIGGER_LOG_PREFIX = "GIT_TRIGGER_LOG_PREFIX: ";
  String GIT_DEFAULT_LOG_PREFIX = "GIT_DEFAULT_LOG_PREFIX: ";
  String GIT_HELM_LOG_PREFIX = "GIT_HELM_LOG_ENTRY: ";
  String GIT_SETUP_RBAC_PREFIX = "GIT_SETUP_RBAC_PREFIX: ";

  String APPLICATION_FOLDER_PATH = SETUP_FOLDER_PATH + APPLICATIONS_FOLDER;

  String VALUES = "VALUES";
  String VALUES_FOLDER = "Values";
  String PCF_OVERRIDES_FOLDER = "PCF Overrides";
  String HELM_CHART_OVERRIDE_FOLDER = "Helm Chart Overrides";

  String VALUES_YAML_KEY = "values.yaml";

  String TAGS = "Tags";
  String TAGS_YAML = "Tags.yaml";

  List<String> ALLOWED_BOOLEAN_VALUES = Lists.newArrayList("true", "false");

  // OC PARAMS
  String OC_PARAMS_ENTITY = "OC_PARAMS";
  String OC_PARAMS_FOLDER = "OC Params";
  String OC_PARAMS_FILE = "params";

  // KUSTOMIZE PATCHES
  String KUSTOMIZE_PATCHES_ENTITY = "KUSTOMIZE_PATCHES";
  String KUSTOMIZE_PATCHES_FOLDER = "Kustomize Patches";
  String KUSTOMIZE_PATCHES_FILE = "patches.yaml";

  // AZURE APP SERVICES
  String APP_SETTINGS_ENTITY = "APP_SETTINGS";
  String AZURE_APP_SETTINGS_OVERRIDES_FOLDER = "App Settings Overrides";
  String APP_SETTINGS_FILE = "appsettings";

  String CONN_STRINGS_ENTITY = "CONN_STRINGS";
  String AZURE_CONN_STRINGS_OVERRIDES_FOLDER = "Conn Strings Overrides";
  String CONN_STRINGS_FILE = "connstrings";

  String APP_SERVICE_MANIFEST_FILE_NAME = "appsettings";
  String EMPTY_SETTINGS_CONTENT = "[]";

  // ECS PARAMS
  String ECS_NOT_USED = "ECS_NOT_USED";

  // Governance Folder
  String GOVERNANCE_FOLDER = "Governance";
  String DEPLOYMENT_GOVERNANCE_FOLDER = "Deployment Governance";

  // App Telemetry
  String CG_EVENT_CONFIG_FOLDER = "Event Rules";
}

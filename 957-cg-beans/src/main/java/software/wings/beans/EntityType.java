/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

/**
 *
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

/**
 * The enum entity type.
 *
 * @author Rishi
 */
@OwnedBy(CDC)
public enum EntityType {

  /**
   * Service entity type.
   */
  SERVICE,
  /**
   * Provisioner entity type.
   */
  PROVISIONER,
  /**
   * Environment entity type.
   */
  ENVIRONMENT,
  /**
   * Host entity type.
   */
  HOST,
  /**
   * Release entity type.
   */
  RELEASE,
  /**
   * Artifacts entity type.
   */
  ARTIFACT,
  /**
   * Ssh user entity type.
   */
  SSH_USER,
  /**
   * Ssh password entity type.
   */
  SSH_PASSWORD,
  /**
   * Ssh app account entity type.
   */
  SSH_APP_ACCOUNT,

  /**
   * Ssh key passphrase entity type.
   */
  SSH_KEY_PASSPHRASE,
  /**
   * Ssh app account passowrd entity type.
   */
  SSH_APP_ACCOUNT_PASSOWRD,

  /**
   * Simple deployment entity type.
   */
  SIMPLE_DEPLOYMENT,

  /**
   * Orchestrated deployment entity type.
   */
  ORCHESTRATED_DEPLOYMENT,

  /**
   * Pipeline entity type.
   */
  PIPELINE,

  /**
   * Workflow entity type.
   */
  WORKFLOW,

  /**
   * Deployment entity type.
   */
  DEPLOYMENT,

  /**
   * Instance entity type.
   */
  INSTANCE,
  /**
   * Application entity type.
   */
  APPLICATION,
  /**
   * Command entity type.
   */
  COMMAND,
  /**
   * Config entity type.
   */
  CONFIG,
  /**
   * Service template entity type.
   */
  SERVICE_TEMPLATE,
  /**
   * Infrastructure Mapping type.
   */
  INFRASTRUCTURE_MAPPING,
  /**
   * Infrastructure Definition type.
   */
  INFRASTRUCTURE_DEFINITION,
  /**
   * User entity type
   */
  USER,
  /**
   * Artifact Stream entity type
   */
  ARTIFACT_STREAM,
  /**
   *  AppDynamics ConfigId
   */
  APPDYNAMICS_CONFIGID,
  /**
   * AppDynamics AppId
   */
  APPDYNAMICS_APPID,
  /**
   * AppDynamics TierId
   */
  APPDYNAMICS_TIERID,
  /**
   * Elk config Id
   */
  ELK_CONFIGID,
  /**
   * Elk indices
   */
  ELK_INDICES,
  /**
   *  NewRelic ConfigId
   */
  NEWRELIC_CONFIGID,
  /**
   * NewRelic AppId
   */
  NEWRELIC_APPID,
  /**
   * SSH Connection attribute Id
   */
  SS_SSH_CONNECTION_ATTRIBUTE,
  /**
   * WINRM Connection attribute Id
   */
  SS_WINRM_CONNECTION_ATTRIBUTE,
  /***
   * SUMO ConfigId
   */
  SUMOLOGIC_CONFIGID,

  /***
   * Splunk ConfigId
   */
  SPLUNK_CONFIGID,

  /**
   *  NewRelic ConfigId
   */
  NEWRELIC_MARKER_CONFIGID,
  /**
   * NewRelic AppId
   */
  NEWRELIC_MARKER_APPID,

  API_KEY,

  ACCOUNT,

  APPLICATION_MANIFEST,

  USER_GROUP,

  WHITELISTED_IP,

  CF_AWS_CONFIG_ID,

  VERIFICATION_CONFIGURATION,

  HELM_GIT_CONFIG_ID,

  NOTIFICATION_GROUP,

  HELM_CHART_SPECIFICATION,

  PCF_SERVICE_SPECIFICATION,

  LAMBDA_SPECIFICATION,

  USER_DATA_SPECIFICATION,

  ECS_CONTAINER_SPECIFICATION,

  ECS_SERVICE_SPECIFICATION,

  K8S_CONTAINER_SPECIFICATION,

  CONFIG_FILE,

  SERVICE_COMMAND,

  MANIFEST_FILE,

  SERVICE_VARIABLE,

  TRIGGER,

  ROLE,

  TEMPLATE,

  TEMPLATE_FOLDER,

  SETTING_ATTRIBUTE,

  ENCRYPTED_RECORDS,

  CV_CONFIGURATION,

  TAG,

  CUSTOM_DASHBOARD,

  PIPELINE_GOVERNANCE_STANDARD,

  WORKFLOW_EXECUTION,

  SERVERLESS_INSTANCE,

  USER_INVITE,

  LOGIN_SETTINGS,

  SSO_SETTINGS,

  DELEGATE,

  DELEGATE_SCOPE,

  DELEGATE_PROFILE,

  EXPORT_EXECUTIONS_REQUEST,

  GCP_CONFIG,

  GIT_CONFIG,

  JENKINS_SERVER,

  SECRETS_MANAGER,

  HELM_CHART,

  SECRET,

  CONNECTOR,

  CLOUD_PROVIDER,

  GOVERNANCE_FREEZE_CONFIG,

  GOVERNANCE_CONFIG,

  EVENT_RULE
}

/**
 *
 */

package software.wings.beans;

/**
 * The enum entity type.
 *
 * @author Rishi
 */
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
  /***
   * SUMO ConfigId
   */
  SUMOLOGIC_CONFIGID,

  /**
   *  NewRelic ConfigId
   */
  NEWRELIC_MARKER_CONFIGID,
  /**
   * NewRelic AppId
   */
  NEWRELIC_MARKER_APPID
}

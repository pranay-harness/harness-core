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
  ELK_INDICES
}

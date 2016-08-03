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
  SERVICE, /**
            * Environment entity type.
            */
  ENVIRONMENT, /**
                * Tag entity type.
                */
  TAG, /**
        * Host entity type.
        */
  HOST, /**
         * Release entity type.
         */
  RELEASE, /**
            * Artifacts entity type.
            */
  ARTIFACT, /**
             * Ssh user entity type.
             */
  SSH_USER, /**
             * Ssh password entity type.
             */
  SSH_PASSWORD, /**
                 * Ssh app account entity type.
                 */
  SSH_APP_ACCOUNT, /**
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

  INSTANCE;
}

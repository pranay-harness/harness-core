/**
 *
 */

package software.wings.beans;

/**
 * The enum Execution argument type.
 *
 * @author Rishi
 */
public enum ExecutionArgumentType {
  /**
   * Service execution argument type.
   */
  SERVICE, /**
            * Release execution argument type.
            */
  RELEASE, /**
            * Artifacts execution argument type.
            */
  ARTIFACTS, /**
              * Ssh user execution argument type.
              */
  SSH_USER, /**
             * Ssh password execution argument type.
             */
  SSH_PASSWORD, /**
                 * Ssh app account execution argument type.
                 */
  SSH_APP_ACCOUNT, /**
                    * Ssh app account passowrd execution argument type.
                    */
  SSH_APP_ACCOUNT_PASSOWRD;
}

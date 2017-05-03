/**
 *
 */

package software.wings.sm;

/**
 * The Enum StateTypeScope.
 *
 * @author Rishi
 */
public enum StateTypeScope {
  /**
   * Pipeline stencils state type scope.
   */
  PIPELINE_STENCILS, /**
                      * Orchestration stencils state type scope.
                      */
  ORCHESTRATION_STENCILS, /**
                           * Command stencils state type scope.
                           */
  COMMAND_STENCILS, /**
                     * None stencils state type scope.
                     */
  COMMON, /**
           * Deployment state type scope.
           */
  DEPLOYMENT, /**
               * Verification state type scope.
               */
  VERIFICATION, /**
                 * Traffic routing state type scope.
                 */
  TRAFFIC_ROUTING, /**
                    * None state type scope.
                    */
  NONE;
}

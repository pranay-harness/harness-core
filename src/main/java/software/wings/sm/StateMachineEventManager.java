/**
 *
 */

package software.wings.sm;

import software.wings.beans.WorkflowExecutionEvent;

/**
 * The interface State machine event manager.
 *
 * @author Rishi
 */
public interface StateMachineEventManager {
  /**
   * Check for event workflow execution event.
   *
   * @return the workflow execution event
   */
  WorkflowExecutionEvent checkForEvent();
}

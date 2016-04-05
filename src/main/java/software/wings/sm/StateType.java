/**
 *
 */
package software.wings.sm;

/**
 * @author Rishi
 *
 */
public enum StateType {
  REPEAT,
  FORK,
  HTTP,
  WAIT,
  PAUSE,
  START,
  STOP,
  RESTART,
  DEPLOY,
  SUB_STATE_MACHINE;
}

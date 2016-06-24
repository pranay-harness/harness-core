/**
 *
 */

package software.wings.sm.states;

import software.wings.sm.StateType;

// TODO: Auto-generated Javadoc

/**
 * The Class StartState.
 *
 * @author Rishi
 */
public class StartState extends CommandState {
  /**
   * Instantiates a new start state.
   *
   * @param name the name
   */
  public StartState(String name) {
    super(name, StateType.START.name());
  }
}

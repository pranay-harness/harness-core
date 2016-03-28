package software.wings.sm;
/**
 *
 */

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

import org.junit.Test;

import software.wings.beans.ErrorConstants;
import software.wings.exception.WingsException;

/**
 * @author Rishi
 *
 */
public class TestStateMachine {
  @Test
  public void testValidate() {
    StateMachine sm = new StateMachine();
    State state = new StateA();
    sm.addState(state);
    state = new StateB();
    sm.addState(state);
    state = new StateC();
    sm.addState(state);
    sm.setInitialStateName(StateA.class.getName());
    assertThat(true).as("Validate result").isEqualTo(sm.validate());
  }

  @Test
  public void testValidateDup() {
    try {
      StateMachine sm = new StateMachine();
      State state = new StateA();
      sm.addState(state);
      state = new StateB();
      sm.addState(state);
      state = new StateC();
      sm.addState(state);
      sm.setInitialStateName(StateA.class.getName());
      state = new StateB();
      sm.addState(state);
      sm.validate();
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      assertThat(e).hasMessage(ErrorConstants.DUPLICATE_STATE_NAMES);
    }
  }
}

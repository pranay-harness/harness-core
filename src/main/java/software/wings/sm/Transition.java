/**
 *
 */
package software.wings.sm;

import java.io.Serializable;

/**
 * @author Rishi
 *
 */
public class Transition implements Serializable {
  private static final long serialVersionUID = 1L;
  private State fromState;
  private State toState;
  private TransitionType transitionType;

  public Transition() {}

  public Transition(State fromState, TransitionType transitionType, State toState) {
    this.fromState = fromState;
    this.transitionType = transitionType;
    this.toState = toState;
  }

  public State getFromState() {
    return fromState;
  }
  public void setFromState(State fromState) {
    this.fromState = fromState;
  }
  public State getToState() {
    return toState;
  }
  public void setToState(State toState) {
    this.toState = toState;
  }
  public TransitionType getTransitionType() {
    return transitionType;
  }
  public void setTransitionType(TransitionType transitionType) {
    this.transitionType = transitionType;
  }
  @Override
  public String toString() {
    return "Transition [fromState=" + fromState + ", toState=" + toState + ", transitionType=" + transitionType + "]";
  }
}

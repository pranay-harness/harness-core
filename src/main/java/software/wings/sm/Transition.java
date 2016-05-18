package software.wings.sm;

import java.io.Serializable;

/**
 * Represents transition between states.
 *
 * @author Rishi
 */
public class Transition implements Serializable {
  private static final long serialVersionUID = 1L;
  private State fromState;
  private State toState;
  private TransitionType transitionType;

  public Transition() {}

  /**
   * creates a transition object to represent.
   *
   * @param fromState      start state.
   * @param transitionType return status of start state.
   * @param toState        end state.
   */
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

  public static final class Builder {
    private State fromState;
    private State toState;
    private TransitionType transitionType;

    private Builder() {}

    public static Builder aTransition() {
      return new Builder();
    }

    public Builder withFromState(State fromState) {
      this.fromState = fromState;
      return this;
    }

    public Builder withToState(State toState) {
      this.toState = toState;
      return this;
    }

    public Builder withTransitionType(TransitionType transitionType) {
      this.transitionType = transitionType;
      return this;
    }

    public Transition build() {
      Transition transition = new Transition();
      transition.setFromState(fromState);
      transition.setToState(toState);
      transition.setTransitionType(transitionType);
      return transition;
    }
  }
}

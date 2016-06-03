package software.wings.sm;

import java.io.Serializable;

// TODO: Auto-generated Javadoc

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

  /**
   * Instantiates a new transition.
   */
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

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Transition [fromState=" + fromState + ", toState=" + toState + ", transitionType=" + transitionType + "]";
  }

  /**
   * The Class Builder.
   */
  public static final class Builder {
    private State fromState;
    private State toState;
    private TransitionType transitionType;

    private Builder() {}

    /**
     * A transition.
     *
     * @return the builder
     */
    public static Builder aTransition() {
      return new Builder();
    }

    /**
     * With from state.
     *
     * @param fromState the from state
     * @return the builder
     */
    public Builder withFromState(State fromState) {
      this.fromState = fromState;
      return this;
    }

    /**
     * With to state.
     *
     * @param toState the to state
     * @return the builder
     */
    public Builder withToState(State toState) {
      this.toState = toState;
      return this;
    }

    /**
     * With transition type.
     *
     * @param transitionType the transition type
     * @return the builder
     */
    public Builder withTransitionType(TransitionType transitionType) {
      this.transitionType = transitionType;
      return this;
    }

    /**
     * Builds the.
     *
     * @return the transition
     */
    public Transition build() {
      Transition transition = new Transition();
      transition.setFromState(fromState);
      transition.setToState(toState);
      transition.setTransitionType(transitionType);
      return transition;
    }
  }
}

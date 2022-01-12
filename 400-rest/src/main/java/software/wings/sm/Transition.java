/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

/**
 * Represents transition between states.
 *
 * @author Rishi
 */
@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class Transition {
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

  /**
   * Gets from state.
   *
   * @return the from state
   */
  public State getFromState() {
    return fromState;
  }

  /**
   * Sets from state.
   *
   * @param fromState the from state
   */
  public void setFromState(State fromState) {
    this.fromState = fromState;
  }

  /**
   * Gets to state.
   *
   * @return the to state
   */
  public State getToState() {
    return toState;
  }

  /**
   * Sets to state.
   *
   * @param toState the to state
   */
  public void setToState(State toState) {
    this.toState = toState;
  }

  /**
   * Gets transition type.
   *
   * @return the transition type
   */
  public TransitionType getTransitionType() {
    return transitionType;
  }

  /**
   * Sets transition type.
   *
   * @param transitionType the transition type
   */
  public void setTransitionType(TransitionType transitionType) {
    this.transitionType = transitionType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Transition that = (Transition) o;

    if (fromState != null ? !fromState.equals(that.fromState) : that.fromState != null) {
      return false;
    }
    if (toState != null ? !toState.equals(that.toState) : that.toState != null) {
      return false;
    }
    return transitionType == that.transitionType;
  }

  @Override
  public int hashCode() {
    int result = fromState != null ? fromState.hashCode() : 0;
    result = 31 * result + (toState != null ? toState.hashCode() : 0);
    result = 31 * result + (transitionType != null ? transitionType.hashCode() : 0);
    return result;
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

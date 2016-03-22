/**
 *
 */
package software.wings.sm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.PostLoad;
import org.mongodb.morphia.annotations.Serialized;
import org.mongodb.morphia.annotations.Transient;

import software.wings.beans.Base;
import software.wings.beans.ErrorConstants;
import software.wings.exception.WingsException;

/**
 * @author Rishi
 *
 */
@Entity(value = "stateMachines", noClassnameStored = true)
public class StateMachine extends Base {
  @SuppressWarnings("unused") private static final long serialVersionUID = 1L;
  private String initialStateName;

  @Serialized private List<State> states = new ArrayList<>();

  @Serialized private List<Transition> transitions = new ArrayList<>();

  @Transient private transient Map<String, State> cachedStatesMap = null;

  @Transient private transient Map<String, Map<TransitionType, List<State>>> cachedTransitionFlowMap = null;

  public String getInitialStateName() {
    return initialStateName;
  }
  public void setInitialStateName(String initialStateName) {
    this.initialStateName = initialStateName;
  }
  public List<State> getStates() {
    return states;
  }
  public void setStates(List<State> states) {
    this.states = states;
  }
  public List<Transition> getTransitions() {
    return transitions;
  }
  public void setTransitions(List<Transition> transitions) {
    this.transitions = transitions;
  }

  public State getInitialState() {
    Map<String, State> statesMap = getStatesMap();
    return statesMap.get(initialStateName);
  }

  public State getSuccessTransition(String fromStateName) {
    return getNextState(fromStateName, TransitionType.SUCCESS);
  }

  public State getFailureTransition(String fromStateName) {
    return getNextState(fromStateName, TransitionType.FAILURE);
  }

  public State getState(String stateName) {
    Map<String, State> statesMap = getStatesMap();
    if (statesMap == null) {
      return null;
    }
    return statesMap.get(stateName);
  }

  public State getNextState(String fromStateName, TransitionType transitionType) {
    List<State> nextStates = getNextStates(fromStateName, transitionType);
    if (nextStates == null || nextStates.size() == 0) {
      return null;
    }
    return nextStates.get(0);
  }
  public List<State> getNextStates(String fromStateName, TransitionType transitionType) {
    Map<String, Map<TransitionType, List<State>>> transitionFlowMap = getTransitionFlowMap();
    if (transitionFlowMap == null || transitionFlowMap.get(fromStateName) == null) {
      return null;
    }
    return transitionFlowMap.get(fromStateName).get(transitionType);
  }

  public boolean validate() {
    Map<String, State> statesMap = getStatesMap();
    if (initialStateName == null || statesMap.get(initialStateName) == null) {
      throw new WingsException(ErrorConstants.INITIAL_STATE_NOT_DEFINED);
    }
    getTransitionFlowMap();
    return true;
  }

  /**
   * @param statesMap
   * @return
   *
   */
  public Map<String, Map<TransitionType, List<State>>> getTransitionFlowMap() {
    if (cachedTransitionFlowMap != null && cachedTransitionFlowMap.size() == 0) {
      return cachedTransitionFlowMap;
    }

    Map<String, State> statesMap = getStatesMap();
    Set<String> invalidStateNames = new HashSet<>();
    Set<String> statesWithDupTransitions = new HashSet<>();
    Set<String> nonForkStates = new HashSet<>();
    Set<String> nonRepeatStates = new HashSet<>();
    Map<String, Map<TransitionType, List<State>>> flowMap = new HashMap<>();

    Map<String, List<String>> forkStateNamesMap = new HashMap<>();
    for (Transition transition : transitions) {
      if (transition.getTransitionType() == null) {
        throw new WingsException(ErrorConstants.TRANSITION_TYPE_NULL);
      }
      State fromState = transition.getFromState();
      State toState = transition.getToState();
      if (fromState == null || toState == null) {
        throw new WingsException(ErrorConstants.TRANSITION_NOT_LINKED);
      }
      boolean invalidState = false;
      if (statesMap.get(fromState.getName()) == null) {
        invalidStateNames.add(fromState.getName());
        invalidState = true;
      }
      if (statesMap.get(toState.getName()) == null) {
        invalidStateNames.add(toState.getName());
        invalidState = true;
      }
      if (!invalidState) {
        if (transition.getTransitionType() == TransitionType.FORK && fromState.getStateType() != StateType.FORK) {
          nonForkStates.add(fromState.getName());
          continue;
        }

        if (transition.getTransitionType() == TransitionType.REPEAT && fromState.getStateType() != StateType.REPEAT) {
          nonRepeatStates.add(fromState.getName());
          continue;
        }

        Map<TransitionType, List<State>> transitionMap = flowMap.get(fromState.getName());
        if (transitionMap == null) {
          transitionMap = new HashMap<>();
          flowMap.put(fromState.getName(), transitionMap);
          List<State> toStates = new ArrayList<>();
          toStates.add(toState);
          transitionMap.put(transition.getTransitionType(), toStates);
        } else {
          List<State> toStates = transitionMap.get(transition.getTransitionType());
          if (toStates == null) {
            toStates = new ArrayList<>();
            toStates.add(toState);
            transitionMap.put(transition.getTransitionType(), toStates);
          } else {
            if (transition.getTransitionType() != TransitionType.FORK
                && transition.getTransitionType() != TransitionType.CONDITIONAL) {
              statesWithDupTransitions.add(fromState.getName());
            } else {
              toStates.add(toState);
            }
          }
        }

        if (transition.getTransitionType() == TransitionType.FORK) {
          List<String> forkStateNames = forkStateNamesMap.get(fromState.getName());
          if (forkStateNames == null) {
            forkStateNames = new ArrayList<>();
            forkStateNamesMap.put(fromState.getName(), forkStateNames);
          }
          forkStateNames.add(toState.getName());
        }
      }
    }
    if (invalidStateNames.size() > 0) {
      Map<String, Object> params = new HashMap<>();
      params.put("invalidStateNames", invalidStateNames.toString());
      throw new WingsException(params, ErrorConstants.TRANSITION_TO_INCORRECT_STATE);
    }
    if (nonForkStates.size() > 0) {
      Map<String, Object> params = new HashMap<>();
      params.put("nonForkStates", nonForkStates.toString());
      throw new WingsException(params, ErrorConstants.NON_FORK_STATES);
    }
    if (nonRepeatStates.size() > 0) {
      Map<String, Object> params = new HashMap<>();
      params.put("nonRepeatStates", nonRepeatStates.toString());
      throw new WingsException(params, ErrorConstants.NON_REPEAT_STATES);
    }
    if (statesWithDupTransitions.size() > 0) {
      Map<String, Object> params = new HashMap<>();
      params.put("statesWithDupTransitions", statesWithDupTransitions.toString());
      throw new WingsException(params, ErrorConstants.STATES_WITH_DUP_TRANSITIONS);
    }
    for (String forkStateName : forkStateNamesMap.keySet()) {
      ForkState forkFromState = (ForkState) statesMap.get(forkStateName);
      forkFromState.setForkStateNames(forkStateNamesMap.get(forkStateName));
    }

    cachedTransitionFlowMap = flowMap;
    return flowMap;
  }

  public Map<String, State> getStatesMap() {
    if (cachedStatesMap != null && cachedStatesMap.size() > 0) {
      return cachedStatesMap;
    }
    Map<String, State> statesMap = new HashMap<>();
    HashSet<String> dupNames = new HashSet<>();

    for (State state : states) {
      if (statesMap.get(state.getName()) != null) {
        dupNames.add(state.getName());
      } else {
        statesMap.put(state.getName(), state);
      }
    }
    if (dupNames.size() > 0) {
      Map<String, Object> params = new HashMap<>();
      params.put("dupStateNames", dupNames.toString());
      throw new WingsException(params, ErrorConstants.DUPLICATE_STATE_NAMES);
    }

    cachedStatesMap = statesMap;
    return statesMap;
  }

  @PostLoad
  public void afterLoad() {
    validate();
  }

  @Override
  public String toString() {
    return "StateMachine [initialStateName=" + initialStateName + ", states=" + states + ", transitions=" + transitions
        + "]";
  }
}

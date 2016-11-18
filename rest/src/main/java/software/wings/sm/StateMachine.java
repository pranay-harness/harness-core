package software.wings.sm;

import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static java.util.stream.Collectors.toList;
import static org.apache.sshd.common.util.GenericUtils.isEmpty;
import static software.wings.sm.StateType.REPEAT;
import static software.wings.sm.Transition.Builder.aTransition;
import static software.wings.sm.states.RepeatState.Builder.aRepeatState;

import com.google.common.collect.Lists;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.PostLoad;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.ErrorCodes;
import software.wings.beans.ExecutionStrategy;
import software.wings.beans.Graph;
import software.wings.beans.Graph.Link;
import software.wings.beans.Graph.Node;
import software.wings.beans.Pipeline;
import software.wings.beans.Workflow;
import software.wings.common.WingsExpressionProcessorFactory;
import software.wings.exception.WingsException;
import software.wings.sm.states.ForkState;
import software.wings.sm.states.RepeatState;
import software.wings.utils.MapperUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Describes a StateMachine.
 *
 * @author Rishi
 */
@Entity(value = "stateMachines", noClassnameStored = true)
public class StateMachine extends Base {
  private static final Logger logger = LoggerFactory.getLogger(StateMachine.class);

  @Indexed private String originId;

  @Indexed private Integer originVersion;

  @Indexed private String name;

  private Graph graph;

  private List<State> states = Lists.newArrayList();

  private List<Transition> transitions = Lists.newArrayList();

  private String initialStateName;

  @Transient private transient Map<String, State> cachedStatesMap = null;

  @Transient private transient Map<String, Map<TransitionType, List<State>>> cachedTransitionFlowMap = null;

  /**
   * Instantiates a new state machine.
   */
  public StateMachine() {}

  /**
   * Instantiates a new state machine.
   *
   * @param workflow   the workflow
   * @param graph      the graph
   * @param stencilMap the stencil map
   */
  public StateMachine(
      Workflow workflow, Integer originVersion, Graph graph, Map<String, StateTypeDescriptor> stencilMap) {
    logger.info("graph received for transform: {}", graph);
    setAppId(workflow.getAppId());
    this.originId = workflow.getUuid();
    this.originVersion = originVersion;
    this.graph = graph;
    this.name = graph.getGraphName();
    try {
      transform(stencilMap);
    } catch (WingsException e) {
      logger.error(e.getLocalizedMessage(), e);
      throw e;
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage(), e);
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "StateMachine transformation error");
    }
  }

  public StateMachine(Pipeline pipeline, Map<String, StateTypeDescriptor> stencilMap) {
    logger.info("Pipeline received for transformation {} " + pipeline.toString());
    setAppId(pipeline.getAppId());
    this.originId = pipeline.getUuid();
    //    this.originVersion = originVersion;
    //    this.graph = graph;
    //    this.name = graph.getGraphName();
    try {
      transformPipeline(pipeline, stencilMap);
    } catch (WingsException e) {
      logger.error(e.getLocalizedMessage(), e);
      throw e;
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage(), e);
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "StateMachine transformation error");
    }
  }

  private void transformPipeline(Pipeline pipeline, Map<String, StateTypeDescriptor> stencilMap) {
    ensureValidatePipelineForTransformation(pipeline);

    String originStateName = pipeline.getPipelineStages().get(0).getPipelineStageElements().get(0).getName();

    pipeline.getPipelineStages()
        .stream()
        .flatMap(pipelineStage -> pipelineStage.getPipelineStageElements().stream())
        .forEach(pipelineStageElement -> {

          StateTypeDescriptor stateTypeDesc = stencilMap.get(pipelineStageElement.getType());

          State state = stateTypeDesc.newInstance(pipelineStageElement.getName());

          Map<String, Object> properties = pipelineStageElement.getProperties();

          // populate properties
          MapperUtils.mapObject(properties, state);

          state.resolveProperties();

          addState(state);
        });

    if (originStateName == null) {
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "Origin state missing");
    }

    if (pipeline.getPipelineStages().size() > 1) {
      Map<String, State> statesMap = getStatesMap();
      for (int stageIdx = 0; stageIdx < pipeline.getPipelineStages().size() - 1; stageIdx++) {
        String currentStateName =
            pipeline.getPipelineStages().get(stageIdx).getPipelineStageElements().get(0).getName();
        String nextStateName =
            pipeline.getPipelineStages().get(stageIdx + 1).getPipelineStageElements().get(0).getName();

        State stateFrom = statesMap.get(currentStateName);
        State stateTo = statesMap.get(nextStateName);
        addTransition(aTransition()
                          .withFromState(stateFrom)
                          .withTransitionType(TransitionType.SUCCESS)
                          .withToState(stateTo)
                          .build());
      }
    }
    setInitialStateName(originStateName);
    validate();
    clearCache();
  }

  private void ensureValidatePipelineForTransformation(Pipeline pipeline) {
    if (pipeline.getPipelineStages().size() == 0) {
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "Pipeline must have one stage at least.");
    }
    Boolean moreThanOneEnvStateInOneStage =
        pipeline.getPipelineStages()
            .stream()
            .map(pipelineStage -> pipelineStage.getPipelineStageElements().size() > 1)
            .findFirst()
            .orElse(false);
    if (moreThanOneEnvStateInOneStage) {
      throw new WingsException(
          ErrorCodes.INVALID_REQUEST, "message", "Pipeline with more than one execution in one stage in not supported");
    }
  }

  private void transform(Map<String, StateTypeDescriptor> stencilMap) {
    String originStateName = null;
    for (Node node : graph.getNodes()) {
      if (node.getType() == null || stencilMap.get(node.getType()) == null) {
        throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "Unknown stencil type");
      }

      if (node.isOrigin()) {
        if (originStateName != null) {
          throw new WingsException(ErrorCodes.INVALID_REQUEST, "message",
              "Duplicate origin state: " + originStateName + " and " + node.getName());
        }

        originStateName = node.getName();
      }

      StateTypeDescriptor stateTypeDesc = stencilMap.get(node.getType());

      State state = stateTypeDesc.newInstance(node.getName());

      Map<String, Object> properties = node.getProperties();

      // populate properties
      MapperUtils.mapObject(properties, state);

      state.resolveProperties();

      addState(state);
    }

    if (originStateName == null) {
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "Origin state missing");
    }

    try {
      Map<String, Node> nodeIdMap = graph.getNodesMap();
      Map<String, State> statesMap = getStatesMap();

      if (graph.getLinks() != null) {
        for (Link link : graph.getLinks()) {
          Node nodeFrom = nodeIdMap.get(link.getFrom());
          Node nodeTo = nodeIdMap.get(link.getTo());

          State stateFrom = statesMap.get(nodeFrom.getName());
          State stateTo = statesMap.get(nodeTo.getName());

          TransitionType transitionType = TransitionType.valueOf(link.getType().toUpperCase());

          if (transitionType == TransitionType.FORK) {
            ((ForkState) stateFrom).addForkState(stateTo);
          } else if (transitionType == TransitionType.REPEAT) {
            ((RepeatState) stateFrom).setRepeatTransitionStateName(stateTo.getName());
          }

          addTransition(
              aTransition().withFromState(stateFrom).withTransitionType(transitionType).withToState(stateTo).build());
        }
      }
      setInitialStateName(originStateName);
      validate();
      addRepeatersBasedOnStateRequiredContextElement();
      clearCache();
    } catch (IllegalArgumentException e) {
      throw new WingsException(e);
    }
  }

  /**
   * Clear cache.
   */
  void clearCache() {
    cachedStatesMap = null;
    cachedTransitionFlowMap = null;
  }

  /**
   * Gets initial state name.
   *
   * @return the initial state name
   */
  public String getInitialStateName() {
    return initialStateName;
  }

  /**
   * Sets initial state name.
   *
   * @param initialStateName the initial state name
   */
  public void setInitialStateName(String initialStateName) {
    this.initialStateName = initialStateName;
  }

  /**
   * Gets states.
   *
   * @return the states
   */
  public List<State> getStates() {
    return states;
  }

  /**
   * Sets states.
   *
   * @param states the states
   */
  public void setStates(List<State> states) {
    this.states = states;
  }

  /**
   * Adds a state to state machine.
   *
   * @param state state to be added.
   * @return state after saving.
   */
  public State addState(State state) {
    if (states == null) {
      states = new ArrayList<>();
    }
    states.add(state);
    return state;
  }

  /**
   * Gets transitions.
   *
   * @return the transitions
   */
  public List<Transition> getTransitions() {
    return transitions;
  }

  /**
   * Sets transitions.
   *
   * @param transitions the transitions
   */
  public void setTransitions(List<Transition> transitions) {
    this.transitions = transitions;
  }

  /**
   * Adds transition to state machine.
   *
   * @param transition transition to be added.
   * @return transition after add.
   */
  public Transition addTransition(Transition transition) {
    if (transitions == null) {
      transitions = new ArrayList<>();
    }
    transitions.add(transition);
    return transition;
  }

  /**
   * Gets initial state.
   *
   * @return the initial state
   */
  public State getInitialState() {
    Map<String, State> statesMap = getStatesMap();
    return statesMap.get(initialStateName);
  }

  /**
   * Gets states map.
   *
   * @return map to state to stateNames.
   */
  public Map<String, State> getStatesMap() {
    if (cachedStatesMap != null && cachedStatesMap.size() > 0) {
      return cachedStatesMap;
    }
    Map<String, State> statesMap = new HashMap<>();
    HashSet<String> dupNames = new HashSet<>();

    if (states != null) {
      for (State state : states) {
        if (statesMap.get(state.getName()) != null) {
          dupNames.add(state.getName());
        } else {
          statesMap.put(state.getName(), state);
        }
      }
    }
    if (dupNames.size() > 0) {
      throw new WingsException(ErrorCodes.DUPLICATE_STATE_NAMES, "dupStateNames", dupNames.toString());
    }

    cachedStatesMap = statesMap;
    return statesMap;
  }

  /**
   * Gets the success transition.
   *
   * @param fromStateName the from state name
   * @return the success transition
   */
  public State getSuccessTransition(String fromStateName) {
    return getNextState(fromStateName, TransitionType.SUCCESS);
  }

  /**
   * Returns next state given start state and transition type.
   *
   * @param fromStateName  start state.
   * @param transitionType transition type to look state from.
   * @return first next state if any or null.
   */
  public State getNextState(String fromStateName, TransitionType transitionType) {
    List<State> nextStates = getNextStates(fromStateName, transitionType);
    if (nextStates == null || nextStates.size() == 0) {
      return null;
    }
    return nextStates.get(0);
  }

  /**
   * Returns list of next states given start state and transition type.
   *
   * @param fromStateName  start state.
   * @param transitionType transition type to look state from.
   * @return list of next states or null.
   */
  public List<State> getNextStates(String fromStateName, TransitionType transitionType) {
    Map<String, Map<TransitionType, List<State>>> transitionFlowMap = getTransitionFlowMap();
    if (transitionFlowMap == null || transitionFlowMap.get(fromStateName) == null) {
      return null;
    }
    return transitionFlowMap.get(fromStateName).get(transitionType);
  }

  /**
   * Gets next states.
   *
   * @param fromStateName the from state name
   * @return the next states
   */
  public List<State> getNextStates(String fromStateName) {
    Map<String, Map<TransitionType, List<State>>> transitionFlowMap = getTransitionFlowMap();
    if (transitionFlowMap == null || transitionFlowMap.get(fromStateName) == null) {
      return null;
    }
    return transitionFlowMap.get(fromStateName)
        .values()
        .stream()
        .flatMap(transitions -> transitions.stream())
        .collect(toList());
  }

  /**
   * Gets transition flow map.
   *
   * @return a transition flow map describing transition types to list of states.
   */
  public Map<String, Map<TransitionType, List<State>>> getTransitionFlowMap() {
    if (cachedTransitionFlowMap != null && cachedTransitionFlowMap.size() > 0) {
      return cachedTransitionFlowMap;
    }

    Map<String, State> statesMap = getStatesMap();
    Set<String> invalidStateNames = new HashSet<>();
    Set<String> statesWithDupTransitions = new HashSet<>();
    Set<String> nonForkStates = new HashSet<>();
    Set<String> nonRepeatStates = new HashSet<>();
    Map<String, Map<TransitionType, List<State>>> flowMap = new HashMap<>();

    Map<String, List<String>> forkStateNamesMap = new HashMap<>();
    if (transitions != null) {
      for (Transition transition : transitions) {
        if (transition.getTransitionType() == null) {
          throw new WingsException(ErrorCodes.TRANSITION_TYPE_NULL);
        }
        State fromState = transition.getFromState();
        State toState = transition.getToState();
        if (fromState == null || toState == null) {
          throw new WingsException(ErrorCodes.TRANSITION_NOT_LINKED);
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
          if (transition.getTransitionType() == TransitionType.FORK
              && !StateType.FORK.name().equals(fromState.getStateType())) {
            nonForkStates.add(fromState.getName());
            continue;
          }

          if (transition.getTransitionType() == TransitionType.REPEAT
              && !REPEAT.name().equals(fromState.getStateType())) {
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
    }
    if (invalidStateNames.size() > 0) {
      throw new WingsException(
          ErrorCodes.TRANSITION_TO_INCORRECT_STATE, "invalidStateNames", invalidStateNames.toString());
    }
    if (nonForkStates.size() > 0) {
      throw new WingsException(ErrorCodes.NON_FORK_STATES, "nonForkStates", nonForkStates.toString());
    }
    if (nonRepeatStates.size() > 0) {
      throw new WingsException(ErrorCodes.NON_REPEAT_STATES, "nonRepeatStates", nonRepeatStates.toString());
    }
    if (statesWithDupTransitions.size() > 0) {
      throw new WingsException(
          ErrorCodes.STATES_WITH_DUP_TRANSITIONS, "statesWithDupTransitions", statesWithDupTransitions.toString());
    }
    for (String forkStateName : forkStateNamesMap.keySet()) {
      ForkState forkFromState = (ForkState) statesMap.get(forkStateName);
      forkFromState.setForkStateNames(forkStateNamesMap.get(forkStateName));
    }

    cachedTransitionFlowMap = flowMap;
    return flowMap;
  }

  /**
   * Gets the failure transition.
   *
   * @param fromStateName the from state name
   * @return the failure transition
   */
  public State getFailureTransition(String fromStateName) {
    return getNextState(fromStateName, TransitionType.FAILURE);
  }

  /**
   * Get state based on name.
   *
   * @param stateName name of state to lookup for.
   * @return state object if found or null.
   */
  public State getState(String stateName) {
    Map<String, State> statesMap = getStatesMap();
    if (statesMap == null) {
      return null;
    }
    return statesMap.get(stateName);
  }

  /**
   * Gets transition from.
   *
   * @param stateFrom the state from
   * @return the transition from
   */
  public List<Transition> getTransitionFrom(State stateFrom) {
    return transitions.stream().filter(transition -> transition.getFromState() == stateFrom).collect(toList());
  }

  /**
   * After load.
   */
  @PostLoad
  public void afterLoad() {
    validate();
  }

  /**
   * Validates a state machine.
   *
   * @return true if valid.
   */
  public boolean validate() {
    Map<String, State> statesMap = getStatesMap();
    if (initialStateName == null || statesMap.get(initialStateName) == null) {
      throw new WingsException(ErrorCodes.INITIAL_STATE_NOT_DEFINED);
    }
    getTransitionFlowMap();
    return true;
  }

  /**
   * Add repeaters based on state required context element.
   */
  void addRepeatersBasedOnStateRequiredContextElement() {
    for (List<State> path : getPaths()) {
      List<ContextElementType> contextElementsPresent = Lists.newArrayList(ContextElementType.OTHER);
      for (int i = 0; i < path.size(); i++) {
        State state = path.get(i);

        if (state instanceof RepeatState) {
          contextElementsPresent.add(((RepeatState) state).getRepeatElementType());

        } else if (state.getRequiredContextElementType() != null
            && !contextElementsPresent.contains(state.getRequiredContextElementType())) {
          List<Transition> transitionsToOldState = getTransitionsTo(state);
          List<Transition> transitionsFromOldState = getTransitionFrom(state);

          String newStateName =
              state.getName() + "--" + UPPER_UNDERSCORE.to(UPPER_CAMEL, state.getRequiredContextElementType().name());
          State newRepeatState = addState(
              aRepeatState()
                  .withName(state.getName())
                  .withRepeatElementType(state.getRequiredContextElementType())
                  .withExecutionStrategy(ExecutionStrategy.PARALLEL)
                  .withRepeatElementExpression(
                      WingsExpressionProcessorFactory.getDefaultExpression(state.getRequiredContextElementType()))
                  .withRepeatTransitionStateName(newStateName)
                  .build());

          transitions.removeAll(transitionsToOldState);
          transitions.removeAll(transitionsFromOldState);

          state.setName(
              state.getName() + "--" + UPPER_UNDERSCORE.to(UPPER_CAMEL, state.getRequiredContextElementType().name()));

          addTransition(aTransition()
                            .withTransitionType(TransitionType.REPEAT)
                            .withFromState(newRepeatState)
                            .withToState(state)
                            .build());
          for (Transition transitionToOldState : transitionsToOldState) {
            addTransition(aTransition()
                              .withTransitionType(transitionToOldState.getTransitionType())
                              .withFromState(transitionToOldState.getFromState())
                              .withToState(newRepeatState)
                              .build());
            for (Transition transitionFromOldState : transitionsFromOldState) {
              addTransition(aTransition()
                                .withTransitionType(transitionFromOldState.getTransitionType())
                                .withFromState(newRepeatState)
                                .withToState(transitionFromOldState.getToState())
                                .build());
            }
          }
        }
      }
    }
  }

  private List<Transition> getTransitionsTo(State state) {
    return transitions.stream().filter(transition -> transition.getToState() == state).collect(toList());
  }

  private List<List<State>> getPaths() {
    return getPaths(new ArrayDeque<>(Collections.singletonList(getInitialState())), Lists.newArrayList());
  }

  private List<List<State>> getPaths(Deque<State> path, List<List<State>> paths) {
    State state = path.peekLast();
    List<State> nextStates = getNextStates(state.getName());
    if (!isEmpty(nextStates)) {
      nextStates.forEach(nextState -> {
        Deque<State> newPath = new ArrayDeque<>(path);
        newPath.add(nextState);
        getPaths(newPath, paths);
      });

    } else {
      paths.add(Lists.newArrayList(path));
    }
    return paths;
  }

  /**
   * Gets origin id.
   *
   * @return the origin id
   */
  public String getOriginId() {
    return originId;
  }

  /**
   * Sets origin id.
   *
   * @param originId the origin id
   */
  public void setOriginId(String originId) {
    this.originId = originId;
  }

  /**
   * Gets name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets graph.
   *
   * @return the graph
   */
  public Graph getGraph() {
    return graph;
  }

  /**
   * Sets graph.
   *
   * @param graph the graph
   */
  public void setGraph(Graph graph) {
    this.graph = graph;
  }

  public Integer getOriginVersion() {
    return originVersion;
  }

  public void setOriginVersion(Integer originVersion) {
    this.originVersion = originVersion;
  }

  /**
   * {@inheritDoc}
   */ /* (non-Javadoc)
   * @see software.wings.beans.Base#toString()
   */
  @Override
  public String toString() {
    return "StateMachine [initialStateName=" + initialStateName + ", states=" + states + ", transitions=" + transitions
        + "]";
  }

  public static final class StateMachineBuilder {
    private String originId;
    private Integer originVersion;
    private String name;
    private Graph graph;
    private List<State> states = Lists.newArrayList();
    private List<Transition> transitions = Lists.newArrayList();
    private String initialStateName;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private StateMachineBuilder() {}

    public static StateMachineBuilder aStateMachine() {
      return new StateMachineBuilder();
    }

    public StateMachineBuilder withOriginId(String originId) {
      this.originId = originId;
      return this;
    }

    public StateMachineBuilder withOriginVersion(Integer originVersion) {
      this.originVersion = originVersion;
      return this;
    }

    public StateMachineBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public StateMachineBuilder withGraph(Graph graph) {
      this.graph = graph;
      return this;
    }

    public StateMachineBuilder addState(State state) {
      this.states.add(state);
      return this;
    }

    public StateMachineBuilder addTransition(Transition transition) {
      this.transitions.add(transition);
      return this;
    }

    public StateMachineBuilder withInitialStateName(String initialStateName) {
      this.initialStateName = initialStateName;
      return this;
    }

    public StateMachineBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public StateMachineBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public StateMachineBuilder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public StateMachineBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public StateMachineBuilder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public StateMachineBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public StateMachine build() {
      StateMachine stateMachine = new StateMachine();
      stateMachine.setOriginId(originId);
      stateMachine.setOriginVersion(originVersion);
      stateMachine.setName(name);
      stateMachine.setGraph(graph);
      stateMachine.setStates(states);
      stateMachine.setTransitions(transitions);
      stateMachine.setInitialStateName(initialStateName);
      stateMachine.setUuid(uuid);
      stateMachine.setAppId(appId);
      stateMachine.setCreatedBy(createdBy);
      stateMachine.setCreatedAt(createdAt);
      stateMachine.setLastUpdatedBy(lastUpdatedBy);
      stateMachine.setLastUpdatedAt(lastUpdatedAt);
      return stateMachine;
    }
  }
}

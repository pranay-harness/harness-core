package software.wings.sm.states;

import static org.apache.commons.lang3.StringUtils.abbreviate;
import static software.wings.api.ForkElement.Builder.aForkElement;

import com.google.common.base.Joiner;

import com.github.reinert.jjschema.SchemaIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ExecutionDataValue;
import software.wings.api.ForkElement;
import software.wings.common.Constants;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.ExecutionStatusData;
import software.wings.sm.SpawningExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.utils.KryoUtils;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Describes a ForkState by which we can fork execution to multiple threads in state machine.
 *
 * @author Rishi
 */
public class ForkState extends State {
  private static final Logger logger = LoggerFactory.getLogger(ForkState.class);

  @SchemaIgnore private List<String> forkStateNames = new ArrayList<>();

  /**
   * Instantiates a new fork state.
   *
   * @param name the name
   */
  public ForkState(String name) {
    super(name, StateType.FORK.name());
  }

  /*
   * (non-Javadoc)
   *
   * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
   */
  @Override
  public ExecutionResponse execute(ExecutionContext contextIntf) {
    ExecutionContextImpl context = (ExecutionContextImpl) contextIntf;
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
    List<String> correlationIds = new ArrayList<>();

    SpawningExecutionResponse executionResponse = new SpawningExecutionResponse();
    ForkStateExecutionData forkStateExecutionData = new ForkStateExecutionData();
    forkStateExecutionData.setForkStateNames(forkStateNames);
    forkStateExecutionData.setElements(new ArrayList<>());
    for (String state : forkStateNames) {
      ForkElement element = aForkElement().withStateName(state).withParentId(stateExecutionInstance.getUuid()).build();
      StateExecutionInstance childStateExecutionInstance = KryoUtils.clone(stateExecutionInstance);
      childStateExecutionInstance.setStateParams(null);

      childStateExecutionInstance.setContextElement(element);
      childStateExecutionInstance.setDisplayName(state);
      childStateExecutionInstance.setStateName(state);
      childStateExecutionInstance.setNotifyId(element.getUuid());
      childStateExecutionInstance.setPrevInstanceId(null);
      childStateExecutionInstance.setDelegateTaskId(null);
      childStateExecutionInstance.setContextTransition(true);
      childStateExecutionInstance.setStatus(ExecutionStatus.NEW);
      childStateExecutionInstance.setStartTs(null);
      childStateExecutionInstance.setEndTs(null);
      childStateExecutionInstance.setCreatedAt(0);
      childStateExecutionInstance.setLastUpdatedAt(0);
      executionResponse.add(childStateExecutionInstance);
      correlationIds.add(element.getUuid());
      forkStateExecutionData.getElements().add(childStateExecutionInstance.getContextElement().getName());
    }

    executionResponse.setStateExecutionData(forkStateExecutionData);
    executionResponse.setAsync(true);
    executionResponse.setCorrelationIds(correlationIds);
    return executionResponse;
  }

  /**
   * Handle abort event.
   *
   * @param context the context
   */
  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  /*
   * (non-Javadoc)
   *
   * @see software.wings.sm.State#handleAsyncResponse(software.wings.sm.ExecutionContextImpl, java.util.Map)
   */
  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    ExecutionResponse executionResponse = new ExecutionResponse();
    for (Object status : response.values()) {
      ExecutionStatus executionStatus = ((ExecutionStatusData) status).getExecutionStatus();
      if (executionStatus != ExecutionStatus.SUCCESS) {
        executionResponse.setExecutionStatus(executionStatus);
      }
    }
    logger.info("Fork state execution completed - stateExecutionInstanceId:{}, displayName:{}, executionStatus:{}",
        ((ExecutionContextImpl) context).getStateExecutionInstance().getUuid(), getName(),
        executionResponse.getExecutionStatus());
    return executionResponse;
  }

  @Override
  @SchemaIgnore
  public Integer getTimeoutMillis() {
    return Constants.DEFAULT_PARENT_STATE_TIMEOUT_MILLIS;
  }

  /**
   * Gets fork state names.
   *
   * @return the fork state names
   */
  @SchemaIgnore
  public List<String> getForkStateNames() {
    return forkStateNames;
  }

  /**
   * Sets fork state names.
   *
   * @param forkStateNames the fork state names
   */
  @SchemaIgnore
  public void setForkStateNames(List<String> forkStateNames) {
    this.forkStateNames = forkStateNames;
  }

  /**
   * Gets fork element name.
   *
   * @param state the state
   * @return the fork element name
   */
  @SchemaIgnore
  public String getForkElementName(String state) {
    return "Fork-" + state;
  }

  /**
   * Adds the fork state.
   *
   * @param state the state
   */
  public void addForkState(State state) {
    this.forkStateNames.add(state.getName());
  }

  /**
   * The type Fork state execution data.
   */
  public static class ForkStateExecutionData extends ElementStateExecutionData {
    private List<String> elements;
    private List<String> forkStateNames;

    /**
     * Gets elements.
     *
     * @return the elements
     */
    public List<String> getElements() {
      return elements;
    }

    /**
     * Sets elements.
     *
     * @param elements the elements
     */
    public void setElements(List<String> elements) {
      this.elements = elements;
    }

    /**
     * Gets fork state names.
     *
     * @return the fork state names
     */
    public List<String> getForkStateNames() {
      return forkStateNames;
    }

    /**
     * Sets fork state names.
     *
     * @param forkStateNames the fork state names
     */
    public void setForkStateNames(List<String> forkStateNames) {
      this.forkStateNames = forkStateNames;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, ExecutionDataValue> getExecutionDetails() {
      Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
      putNotNull(executionDetails, "forkStateNames",
          ExecutionDataValue.builder().displayName("Forking to").value(Joiner.on(", ").join(forkStateNames)).build());
      return executionDetails;
    }

    @Override
    public Map<String, ExecutionDataValue> getExecutionSummary() {
      Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
      putNotNull(executionDetails, "forkStateNames",
          ExecutionDataValue.builder()
              .displayName("Forking to")
              .value(abbreviate(Joiner.on(", ").join(forkStateNames), Constants.SUMMARY_PAYLOAD_LIMIT))
              .build());
      return executionDetails;
    }
  }
}

package software.wings.sm.states;

import static org.apache.commons.lang3.StringUtils.abbreviate;
import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import com.google.common.base.Joiner;

import com.github.reinert.jjschema.SchemaIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ExecutionDataValue;
import software.wings.common.Constants;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.ExecutionStatus.ExecutionStatusData;
import software.wings.sm.SpawningExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.utils.JsonUtils;
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
    int index = 0;
    for (String state : forkStateNames) {
      String notifyId = stateExecutionInstance.getUuid() + "-forkTo-" + state;
      StateExecutionInstance childStateExecutionInstance =
          JsonUtils.clone(stateExecutionInstance, StateExecutionInstance.class);

      index++;
      childStateExecutionInstance.setContextElementName("Fork" + index);
      childStateExecutionInstance.setContextElementType(StateType.FORK.name());
      childStateExecutionInstance.setStateName(state);
      childStateExecutionInstance.setNotifyId(notifyId);
      childStateExecutionInstance.setPrevInstanceId(null);
      childStateExecutionInstance.setContextTransition(true);
      childStateExecutionInstance.setStatus(ExecutionStatus.NEW);
      childStateExecutionInstance.setStartTs(null);
      childStateExecutionInstance.setEndTs(null);
      executionResponse.add(childStateExecutionInstance);
      correlationIds.add(notifyId);
      forkStateExecutionData.getElements().add(childStateExecutionInstance.getContextElementName());
    }

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
  public ExecutionResponse handleAsyncResponse(ExecutionContextImpl context, Map<String, NotifyResponseData> response) {
    ExecutionResponse executionResponse = new ExecutionResponse();
    for (Object status : response.values()) {
      ExecutionStatus executionStatus = ((ExecutionStatusData) status).getExecutionStatus();
      if (executionStatus != ExecutionStatus.SUCCESS) {
        executionResponse.setExecutionStatus(ExecutionStatus.FAILED);
      }
    }
    logger.info("Fork state execution completed - stateExecutionInstanceId:{}, stateName:{}, executionStatus:{}",
        context.getStateExecutionInstance().getUuid(), getName(), executionResponse.getExecutionStatus());
    return executionResponse;
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
  public static class ForkStateExecutionData extends StateExecutionData {
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
          anExecutionDataValue().withValue(Joiner.on(", ").join(forkStateNames)).withDisplayName("Forking to").build());
      return executionDetails;
    }

    @Override
    public Map<String, ExecutionDataValue> getExecutionSummary() {
      Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
      putNotNull(executionDetails, "forkStateNames",
          anExecutionDataValue()
              .withValue(abbreviate(Joiner.on(", ").join(forkStateNames), Constants.SUMMARY_PAYLOAD_LIMIT))
              .withDisplayName("Forking to")
              .build());
      return executionDetails;
    }
  }
}

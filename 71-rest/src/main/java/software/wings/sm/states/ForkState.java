package software.wings.sm.states;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static java.lang.String.join;
import static org.apache.commons.lang3.StringUtils.abbreviate;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.ExecutionStatusResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.serializer.KryoUtils;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.ExecutionDataValue;
import software.wings.api.ForkElement;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionResponse.ExecutionResponseBuilder;
import software.wings.sm.State;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Describes a ForkState by which we can fork execution to multiple threads in state machine.
 *
 * @author Rishi
 */
@OwnedBy(CDC)
@Slf4j
public class ForkState extends State {
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

    ExecutionResponseBuilder executionResponseBuilder = ExecutionResponse.builder();
    ForkStateExecutionData forkStateExecutionData = new ForkStateExecutionData();
    forkStateExecutionData.setForkStateNames(forkStateNames);
    forkStateExecutionData.setElements(new ArrayList<>());
    for (String state : forkStateNames) {
      ForkElement element = ForkElement.builder().stateName(state).parentId(stateExecutionInstance.getUuid()).build();
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
      childStateExecutionInstance.setHasInspection(false);
      executionResponseBuilder.stateExecutionInstance(childStateExecutionInstance);
      correlationIds.add(element.getUuid());
      forkStateExecutionData.getElements().add(childStateExecutionInstance.getContextElement().getName());
    }

    return executionResponseBuilder.stateExecutionData(forkStateExecutionData)
        .async(true)
        .correlationIds(correlationIds)
        .build();
  }

  /**
   * Handle abort event.
   *
   * @param context the context
   */
  @Override
  public void handleAbortEvent(ExecutionContext context) {
    // nothing to handle
  }

  /*
   * (non-Javadoc)
   *
   * @see software.wings.sm.State#handleAsyncResponse(software.wings.sm.ExecutionContextImpl, java.util.Map)
   */
  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    ExecutionResponseBuilder executionResponseBuilder = ExecutionResponse.builder();
    for (ResponseData status : response.values()) {
      ExecutionStatus executionStatus = ((ExecutionStatusResponseData) status).getExecutionStatus();
      if (executionStatus != ExecutionStatus.SUCCESS) {
        executionResponseBuilder.executionStatus(executionStatus);
      }
    }
    final ExecutionResponse executionResponse = executionResponseBuilder.build();
    logger.info("Fork state execution completed - displayName:{}, executionStatus:{}", getName(),
        executionResponse.getExecutionStatus());
    return executionResponse;
  }

  @Override
  @SchemaIgnore
  public Integer getTimeoutMillis() {
    return INFINITE_TIMEOUT;
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
          ExecutionDataValue.builder().displayName("Forking to").value(join(", ", forkStateNames)).build());
      return executionDetails;
    }

    @Override
    public Map<String, ExecutionDataValue> getExecutionSummary() {
      Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
      putNotNull(executionDetails, "forkStateNames",
          ExecutionDataValue.builder()
              .displayName("Forking to")
              .value(abbreviate(join(", ", forkStateNames), StateExecutionData.SUMMARY_PAYLOAD_LIMIT))
              .build());
      return executionDetails;
    }
  }
}

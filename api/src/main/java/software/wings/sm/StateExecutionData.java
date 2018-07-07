package software.wings.sm;

import static io.harness.govern.Switch.unhandled;

import com.google.common.collect.Maps;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import software.wings.api.ExecutionDataValue;
import software.wings.beans.CountsByStatuses;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents state machine execution data.
 *
 * @author Rishi
 */
public class StateExecutionData {
  private String stateName;
  private String stateType;
  private Long startTs;
  private Long endTs;
  private ExecutionStatus status;
  private String errorMsg;
  private Integer waitInterval;
  private ContextElement element;
  private Map<String, Object> stateParams;
  private DelegateMetaInfo delegateMetaInfo;
  private Map<String, Object> templateVariable;

  public StateExecutionData() {}

  public StateExecutionData(String stateName, String stateType, Long startTs, Long endTs, ExecutionStatus status,
      String errorMsg, Integer waitInterval, ContextElement element, Map<String, Object> stateParams,
      Map<String, Object> templateVariable) {
    this.stateName = stateName;
    this.stateType = stateType;
    this.startTs = startTs;
    this.endTs = endTs;
    this.status = status;
    this.errorMsg = errorMsg;
    this.waitInterval = waitInterval;
    this.element = element;
    this.stateParams = stateParams;
    this.templateVariable = templateVariable;
  }

  public Map<String, Object> getTemplateVariable() {
    return templateVariable;
  }

  public void setTemplateVariable(Map<String, Object> templateVariable) {
    this.templateVariable = templateVariable;
  }

  /**
   * Gets state name.
   *
   * @return the state name
   */
  public String getStateName() {
    return stateName;
  }

  /**
   * Sets state name.
   *
   * @param stateName the state name
   */
  public void setStateName(String stateName) {
    this.stateName = stateName;
  }

  /**
   * Gets state type.
   *
   * @return the state type
   */
  public String getStateType() {
    return stateType;
  }

  /**
   * Sets state type.
   *
   * @param stateType the state type
   */
  public void setStateType(String stateType) {
    this.stateType = stateType;
  }

  /**
   * Gets start ts.
   *
   * @return the start ts
   */
  public Long getStartTs() {
    return startTs;
  }

  /**
   * Sets start ts.
   *
   * @param startTs the start ts
   */
  public void setStartTs(Long startTs) {
    this.startTs = startTs;
  }

  /**
   * Gets end ts.
   *
   * @return the end ts
   */
  public Long getEndTs() {
    return endTs;
  }

  /**
   * Sets end ts.
   *
   * @param endTs the end ts
   */
  public void setEndTs(Long endTs) {
    this.endTs = endTs;
  }

  /**
   * Gets status.
   *
   * @return the status
   */
  public ExecutionStatus getStatus() {
    return status;
  }

  /**
   * Sets status.
   *
   * @param status the status
   */
  public void setStatus(ExecutionStatus status) {
    this.status = status;
  }

  /**
   * Gets error msg.
   *
   * @return the error msg
   */
  public String getErrorMsg() {
    return errorMsg;
  }

  public DelegateMetaInfo getDelegateMetaInfo() {
    return delegateMetaInfo;
  }

  /**
   * Sets error msg.
   *
   * @param errorMsg the error msg
   */
  public void setErrorMsg(String errorMsg) {
    this.errorMsg = errorMsg;
  }

  public Integer getWaitInterval() {
    return waitInterval;
  }

  public void setWaitInterval(Integer waitInterval) {
    this.waitInterval = waitInterval;
  }

  public ContextElement getElement() {
    return element;
  }

  public void setElement(ContextElement element) {
    this.element = element;
  }

  public Map<String, Object> getStateParams() {
    return stateParams;
  }

  public void setStateParams(Map<String, Object> stateParams) {
    this.stateParams = stateParams;
  }

  public void setDelegateMetaInfo(DelegateMetaInfo delegateMetaInfo) {
    this.delegateMetaInfo = delegateMetaInfo;
  }

  /**
   * Gets execution summary.
   *
   * @return the execution summary
   */
  @JsonProperty("executionSummary")
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionData = Maps.newLinkedHashMap();
    executionData.put("total", ExecutionDataValue.builder().displayName("Total").value(1).build());
    CountsByStatuses breakDown = new CountsByStatuses();
    switch (status) {
      case FAILED:
      case ERROR:
      case ABORTED:
      case DISCONTINUING:
      case WAITING:
      case REJECTED:
      case EXPIRED:
        breakDown.setFailed(1);
        break;
      case NEW:
      case STARTING:
      case RUNNING:
      case PAUSED:
        breakDown.setInprogress(1);
        break;
      case QUEUED:
        breakDown.setQueued(1);
        break;
      case SUCCESS:
      case SKIPPED:
        breakDown.setSuccess(1);
        break;
      default:
        unhandled(status);
    }
    executionData.put("breakdown", ExecutionDataValue.builder().displayName("breakdown").value(breakDown).build());
    putNotNull(executionData, "errorMsg", ExecutionDataValue.builder().displayName("Message").value(errorMsg).build());
    return executionData;
  }

  /**
   * Sets execution summary.
   *
   * @param ignored the ignored
   */
  public void setExecutionSummary(Map<String, ExecutionDataValue> ignored) {}

  /**
   * Gets execution details.
   *
   * @return the execution details
   */
  @JsonProperty("executionDetails")
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = Maps.newLinkedHashMap();

    putNotNull(
        executionDetails, "errorMsg", ExecutionDataValue.builder().displayName("Message").value(errorMsg).build());
    putNotNull(
        executionDetails, "startTs", ExecutionDataValue.builder().displayName("Started At").value(startTs).build());
    putNotNull(executionDetails, "endTs", ExecutionDataValue.builder().displayName("Ended At").value(endTs).build());

    if (getDelegateMetaInfo() != null) {
      putNotNull(executionDetails, "delegateName",
          ExecutionDataValue.builder().displayName("Delegate").value(this.getDelegateMetaInfo().getHostName()).build());
    }

    return executionDetails;
  }

  /**
   * Sets execution details.
   *
   * @param ignored the ignored
   */
  public void setExecutionDetails(Map<String, ExecutionDataValue> ignored) {}

  /**
   * Put not null.
   *
   * @param orderedMap the ordered map
   * @param name       the name
   * @param value      the value
   */
  protected void putNotNull(Map<String, ExecutionDataValue> orderedMap, String name, ExecutionDataValue value) {
    if (value != null && value.getValue() != null) {
      orderedMap.put(name, value);
    }
  }

  protected Map<String, String> removeNullValues(Map<String, String> map) {
    return map.entrySet()
        .stream()
        .filter(e -> e.getValue() != null)
        .collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));
  }

  @JsonIgnore
  public StepExecutionSummary getStepExecutionSummary() {
    StepExecutionSummary stepExecutionSummary = new StepExecutionSummary();
    populateStepExecutionSummary(stepExecutionSummary);
    return stepExecutionSummary;
  }

  protected void populateStepExecutionSummary(StepExecutionSummary stepExecutionSummary) {
    if (element != null) {
      stepExecutionSummary.setElement(element.cloneMin());
    }
    stepExecutionSummary.setStepName(stateName);
    stepExecutionSummary.setStatus(status);
    stepExecutionSummary.setMessage(errorMsg);
  }

  public static final class StateExecutionDataBuilder {
    private String stateName;
    private Long startTs;
    private Long endTs;
    private ExecutionStatus status;
    private String errorMsg;
    private Integer waitInterval;
    private ContextElement element;

    private StateExecutionDataBuilder() {}

    public static StateExecutionDataBuilder aStateExecutionData() {
      return new StateExecutionDataBuilder();
    }

    public StateExecutionDataBuilder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    public StateExecutionDataBuilder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    public StateExecutionDataBuilder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    public StateExecutionDataBuilder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public StateExecutionDataBuilder withErrorMsg(String errorMsg) {
      this.errorMsg = errorMsg;
      return this;
    }

    public StateExecutionDataBuilder withWaitInterval(Integer waitInterval) {
      this.waitInterval = waitInterval;
      return this;
    }

    public StateExecutionDataBuilder withElement(ContextElement element) {
      this.element = element;
      return this;
    }

    public StateExecutionData build() {
      StateExecutionData stateExecutionData = new StateExecutionData();
      stateExecutionData.setStateName(stateName);
      stateExecutionData.setStartTs(startTs);
      stateExecutionData.setEndTs(endTs);
      stateExecutionData.setStatus(status);
      stateExecutionData.setErrorMsg(errorMsg);
      stateExecutionData.setWaitInterval(waitInterval);
      stateExecutionData.setElement(element);
      return stateExecutionData;
    }
  }
}

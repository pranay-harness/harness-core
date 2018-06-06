package software.wings.sm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.simpleframework.xml.Transient;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.OrchestrationWorkflowType;
import software.wings.beans.WorkflowType;
import software.wings.dl.WingsDeque;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents State Machine Instance.
 *
 * @author Rishi
 */
@Entity(value = "stateExecutionInstances", noClassnameStored = true)
@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StateExecutionInstance extends Base {
  public static final String CALLBACK = "callback";
  public static final String CONTEXT_ELEMENT_KEY = "contextElement";
  public static final String CONTEXT_ELEMENTS_KEY = "contextElements";
  public static final String CONTEXT_TRANSITION_KEY = "contextTransition";
  public static final String DEDICATED_INTERRUPT_COUNT_KEY = "dedicatedInterruptCount";
  public static final String DISPLAY_NAME_KEY = "displayName";
  public static final String EXECUTION_UUID_KEY = "executionUuid";
  public static final String EXECUTION_TYPE_KEY = "executionType";
  public static final String EXPIRY_TS_KEY = "expiryTs";
  public static final String INTERRUPT_HISTORY_KEY = "interruptHistory";
  public static final String PARENT_INSTANCE_ID_KEY = "parentInstanceId";
  public static final String PHASE_SUBWORKFLOW_ID_KEY = "phaseSubWorkflowId";
  public static final String PIPELINE_STATE_ELEMENT_ID_KEY = "pipelineStateElementId";
  public static final String PREV_INSTANCE_ID_KEY = "prevInstanceId";
  public static final String ROLLBACK_KEY = "rollback";
  public static final String STATE_EXECUTION_DATA_HISTORY_KEY = "stateExecutionDataHistory";
  public static final String STATE_EXECUTION_MAP_KEY = "stateExecutionMap";
  public static final String STATE_NAME_KEY = "stateName";
  public static final String STATE_TYPE_KEY = "stateType";
  public static final String STATUS_KEY = "status";
  public static final String STEP_ID_KEY = "stepId";
  public static final String WORKFLOW_ID_KEY = "workflowId";

  private String stateMachineId;
  private String childStateMachineId;
  private String displayName;
  private String stateName;
  @Indexed private String stateType;
  private ContextElement contextElement;
  private boolean contextTransition;
  private boolean rollback;
  private String delegateTaskId;
  private String rollbackPhaseName;

  @Embedded private WingsDeque<ContextElement> contextElements = new WingsDeque<>();
  private Map<String, StateExecutionData> stateExecutionMap = new HashMap<>();
  private List<StateExecutionData> stateExecutionDataHistory = new ArrayList<>();

  private Integer dedicatedInterruptCount;
  private List<ExecutionInterruptEffect> interruptHistory = new ArrayList<>();

  private List<ExecutionEventAdvisor> executionEventAdvisors;

  private List<ContextElement> notifyElements;

  private StateMachineExecutionCallback callback;

  private String executionName;

  private WorkflowType executionType;

  @Indexed private String executionUuid;

  @Indexed private String parentInstanceId;

  @Indexed private String prevInstanceId;

  private String nextInstanceId;

  @Indexed private String cloneInstanceId;

  private String notifyId;
  @Indexed private ExecutionStatus status = ExecutionStatus.NEW;

  private Map<String, Object> stateParams;

  private Long startTs;
  private Long endTs;
  @Indexed private Long expiryTs;

  @Transient private String workflowId;
  @Transient private String pipelineStateElementId;
  @Transient private String phaseSubWorkflowId;
  @Transient private String stepId;

  private OrchestrationWorkflowType orchestrationWorkflowType;

  @SchemaIgnore
  @JsonIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(6).toInstant());

  public StateExecutionData getStateExecutionData() {
    return stateExecutionMap.get(displayName);
  }

  @Override
  @JsonIgnore
  public Map<String, Object> getShardKeys() {
    Map<String, Object> shardKeys = super.getShardKeys();
    shardKeys.put("executionUuid", executionUuid);
    return shardKeys;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String stateMachineId;
    private String childStateMachineId;
    private String displayName;
    private String stateName;
    private String stateType;
    private ContextElement contextElement;
    private boolean contextTransition;
    private WingsDeque<ContextElement> contextElements = new WingsDeque<>();
    private Map<String, StateExecutionData> stateExecutionMap = new HashMap<>();
    private List<StateExecutionData> stateExecutionDataHistory = new ArrayList<>();
    private List<ContextElement> notifyElements;
    private StateMachineExecutionCallback callback;
    private String executionName;
    private WorkflowType executionType;
    private OrchestrationWorkflowType orchestrationWorkflowType;
    private String executionUuid;
    private String parentInstanceId;
    private String prevInstanceId;
    private String nextInstanceId;
    private String cloneInstanceId;
    private String notifyId;
    private ExecutionStatus status = ExecutionStatus.NEW;
    private Long startTs;
    private Long endTs;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    /**
     * A state execution instance builder.
     *
     * @return the builder
     */
    public static Builder aStateExecutionInstance() {
      return new Builder();
    }

    /**
     * With state machine id builder.
     *
     * @param childStateMachineId the state machine id
     * @return the builder
     */
    public Builder withChildStateMachineId(String childStateMachineId) {
      this.childStateMachineId = childStateMachineId;
      return this;
    }

    public Builder withStateMachineId(String stateMachineId) {
      this.stateMachineId = stateMachineId;
      return this;
    }

    /**
     * With state name builder.
     *
     * @param displayName the state name
     * @return the builder
     */
    public Builder withDisplayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    /**
     * With state name builder.
     *
     * @param stateName the state name
     * @return the builder
     */
    public Builder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    /**
     * With state type builder.
     *
     * @param stateType the state type
     * @return the builder
     */
    public Builder withStateType(String stateType) {
      this.stateType = stateType;
      return this;
    }

    /**
     * With context element builder.
     *
     * @param contextElement the context element
     * @return the builder
     */
    public Builder withContextElement(ContextElement contextElement) {
      this.contextElement = contextElement;
      return this;
    }

    /**
     * With context transition builder.
     *
     * @param contextTransition the context transition
     * @return the builder
     */
    public Builder withContextTransition(boolean contextTransition) {
      this.contextTransition = contextTransition;
      return this;
    }

    /**
     * With context elements builder.
     *
     * @param contextElements the context elements
     * @return the builder
     */
    public Builder withContextElements(WingsDeque<ContextElement> contextElements) {
      this.contextElements = contextElements;
      return this;
    }

    /**
     * With context elements builder.
     *
     * @param contextElement the context element
     * @return the builder
     */
    public Builder addContextElement(ContextElement contextElement) {
      this.contextElements.add(contextElement);
      return this;
    }

    /**
     * With state execution map builder.
     *
     * @param stateName the state name
     * @param stateExecutionData the state execution data
     * @return the builder
     */
    public Builder addStateExecutionData(String stateName, StateExecutionData stateExecutionData) {
      this.stateExecutionMap.put(stateName, stateExecutionData);
      return this;
    }

    /**
     * With state execution map builder.
     *
     * @param stateExecutionMap the state execution map
     * @return the builder
     */
    public Builder withStateExecutionMap(Map<String, StateExecutionData> stateExecutionMap) {
      this.stateExecutionMap = stateExecutionMap;
      return this;
    }

    /**
     * With state execution map builder.
     *
     * @param stateExecutionData the state execution data
     * @return the builder
     */
    public Builder addStateExecutionData(StateExecutionData stateExecutionData) {
      this.stateExecutionMap.put(displayName, stateExecutionData);
      return this;
    }

    /**
     * With state execution data history builder.
     *
     * @param stateExecutionDataHistory the state execution data history
     * @return the builder
     */
    public Builder withStateExecutionDataHistory(List<StateExecutionData> stateExecutionDataHistory) {
      this.stateExecutionDataHistory = stateExecutionDataHistory;
      return this;
    }

    public Builder withNotifyElements(List<ContextElement> notifyElements) {
      this.notifyElements = notifyElements;
      return this;
    }

    public Builder withCallback(StateMachineExecutionCallback callback) {
      this.callback = callback;
      return this;
    }

    /**
     * With execution name builder.
     *
     * @param executionName the execution name
     * @return the builder
     */
    public Builder withExecutionName(String executionName) {
      this.executionName = executionName;
      return this;
    }

    /**
     * With execution type builder.
     *
     * @param executionType the execution type
     * @return the builder
     */
    public Builder withExecutionType(WorkflowType executionType) {
      this.executionType = executionType;
      return this;
    }

    /**
     * With execution type builder.
     *
     * @param orchestrationWorkflowType the execution type
     * @return the builder
     */
    public Builder withOrchestrationWorkflowType(OrchestrationWorkflowType orchestrationWorkflowType) {
      this.orchestrationWorkflowType = orchestrationWorkflowType;
      return this;
    }

    /**
     * With execution uuid builder.
     *
     * @param executionUuid the execution uuid
     * @return the builder
     */
    public Builder withExecutionUuid(String executionUuid) {
      this.executionUuid = executionUuid;
      return this;
    }

    /**
     * With parent instance id builder.
     *
     * @param parentInstanceId the parent instance id
     * @return the builder
     */
    public Builder withParentInstanceId(String parentInstanceId) {
      this.parentInstanceId = parentInstanceId;
      return this;
    }

    /**
     * With prev instance id builder.
     *
     * @param prevInstanceId the prev instance id
     * @return the builder
     */
    public Builder withPrevInstanceId(String prevInstanceId) {
      this.prevInstanceId = prevInstanceId;
      return this;
    }

    /**
     * With next instance id builder.
     *
     * @param nextInstanceId the next instance id
     * @return the builder
     */
    public Builder withNextInstanceId(String nextInstanceId) {
      this.nextInstanceId = nextInstanceId;
      return this;
    }

    /**
     * With clone instance id builder.
     *
     * @param cloneInstanceId the clone instance id
     * @return the builder
     */
    public Builder withCloneInstanceId(String cloneInstanceId) {
      this.cloneInstanceId = cloneInstanceId;
      return this;
    }

    /**
     * With notify id builder.
     *
     * @param notifyId the notify id
     * @return the builder
     */
    public Builder withNotifyId(String notifyId) {
      this.notifyId = notifyId;
      return this;
    }

    /**
     * With status builder.
     *
     * @param status the status
     * @return the builder
     */
    public Builder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    /**
     * With start ts builder.
     *
     * @param startTs the start ts
     * @return the builder
     */
    public Builder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    /**
     * With end ts builder.
     *
     * @param endTs the end ts
     * @return the builder
     */
    public Builder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aStateExecutionInstance()
          .withStateMachineId(stateMachineId)
          .withStateName(stateName)
          .withDisplayName(displayName)
          .withStateType(stateType)
          .withContextElement(contextElement)
          .withContextTransition(contextTransition)
          .withContextElements(contextElements)
          .withStateExecutionMap(stateExecutionMap)
          .withStateExecutionDataHistory(stateExecutionDataHistory)
          .withNotifyElements(notifyElements)
          .withCallback(callback)
          .withExecutionName(executionName)
          .withExecutionType(executionType)
          .withOrchestrationWorkflowType(orchestrationWorkflowType)
          .withExecutionUuid(executionUuid)
          .withParentInstanceId(parentInstanceId)
          .withPrevInstanceId(prevInstanceId)
          .withNextInstanceId(nextInstanceId)
          .withCloneInstanceId(cloneInstanceId)
          .withNotifyId(notifyId)
          .withStatus(status)
          .withStartTs(startTs)
          .withEndTs(endTs)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    /**
     * Build state execution instance.
     *
     * @return the state execution instance
     */
    public StateExecutionInstance build() {
      StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
      stateExecutionInstance.setStateMachineId(stateMachineId);
      stateExecutionInstance.setChildStateMachineId(childStateMachineId);
      stateExecutionInstance.setDisplayName(displayName);
      stateExecutionInstance.setStateName(stateName);
      stateExecutionInstance.setStateType(stateType);
      stateExecutionInstance.setContextElement(contextElement);
      stateExecutionInstance.setContextTransition(contextTransition);
      stateExecutionInstance.setContextElements(contextElements);
      stateExecutionInstance.setStateExecutionMap(stateExecutionMap);
      stateExecutionInstance.setStateExecutionDataHistory(stateExecutionDataHistory);
      stateExecutionInstance.setNotifyElements(notifyElements);
      stateExecutionInstance.setCallback(callback);
      stateExecutionInstance.setExecutionName(executionName);
      stateExecutionInstance.setExecutionType(executionType);
      stateExecutionInstance.setOrchestrationWorkflowType(orchestrationWorkflowType);
      stateExecutionInstance.setExecutionUuid(executionUuid);
      stateExecutionInstance.setParentInstanceId(parentInstanceId);
      stateExecutionInstance.setPrevInstanceId(prevInstanceId);
      stateExecutionInstance.setNextInstanceId(nextInstanceId);
      stateExecutionInstance.setCloneInstanceId(cloneInstanceId);
      stateExecutionInstance.setNotifyId(notifyId);
      stateExecutionInstance.setStatus(status);
      stateExecutionInstance.setStartTs(startTs);
      stateExecutionInstance.setEndTs(endTs);
      stateExecutionInstance.setUuid(uuid);
      stateExecutionInstance.setAppId(appId);
      stateExecutionInstance.setCreatedBy(createdBy);
      stateExecutionInstance.setCreatedAt(createdAt);
      stateExecutionInstance.setLastUpdatedBy(lastUpdatedBy);
      stateExecutionInstance.setLastUpdatedAt(lastUpdatedAt);
      return stateExecutionInstance;
    }
  }
}

package software.wings.sm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
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
@Indexes(@Index(options = @IndexOptions(name = "stateTypes"),
    fields = { @Field("appId")
               , @Field("executionUuid"), @Field("stateType") }))
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

  private WingsDeque<ContextElement> contextElements = new WingsDeque<>();
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

  private String prevInstanceId;

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

    public static Builder aStateExecutionInstance() {
      return new Builder();
    }

    public Builder withChildStateMachineId(String childStateMachineId) {
      this.childStateMachineId = childStateMachineId;
      return this;
    }

    public Builder withStateMachineId(String stateMachineId) {
      this.stateMachineId = stateMachineId;
      return this;
    }

    public Builder withDisplayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    public Builder withStateType(String stateType) {
      this.stateType = stateType;
      return this;
    }

    public Builder withContextElement(ContextElement contextElement) {
      this.contextElement = contextElement;
      return this;
    }

    public Builder withContextTransition(boolean contextTransition) {
      this.contextTransition = contextTransition;
      return this;
    }

    public Builder withContextElements(WingsDeque<ContextElement> contextElements) {
      this.contextElements = contextElements;
      return this;
    }

    public Builder addContextElement(ContextElement contextElement) {
      this.contextElements.add(contextElement);
      return this;
    }

    public Builder addStateExecutionData(String stateName, StateExecutionData stateExecutionData) {
      this.stateExecutionMap.put(stateName, stateExecutionData);
      return this;
    }

    public Builder addStateExecutionData(StateExecutionData stateExecutionData) {
      this.stateExecutionMap.put(displayName, stateExecutionData);
      return this;
    }

    public Builder withStateExecutionMap(Map<String, StateExecutionData> stateExecutionMap) {
      this.stateExecutionMap = stateExecutionMap;
      return this;
    }

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

    public Builder withExecutionName(String executionName) {
      this.executionName = executionName;
      return this;
    }

    public Builder withExecutionType(WorkflowType executionType) {
      this.executionType = executionType;
      return this;
    }

    public Builder withOrchestrationWorkflowType(OrchestrationWorkflowType orchestrationWorkflowType) {
      this.orchestrationWorkflowType = orchestrationWorkflowType;
      return this;
    }

    public Builder withExecutionUuid(String executionUuid) {
      this.executionUuid = executionUuid;
      return this;
    }

    public Builder withParentInstanceId(String parentInstanceId) {
      this.parentInstanceId = parentInstanceId;
      return this;
    }

    public Builder withPrevInstanceId(String prevInstanceId) {
      this.prevInstanceId = prevInstanceId;
      return this;
    }

    public Builder withNextInstanceId(String nextInstanceId) {
      this.nextInstanceId = nextInstanceId;
      return this;
    }

    public Builder withCloneInstanceId(String cloneInstanceId) {
      this.cloneInstanceId = cloneInstanceId;
      return this;
    }

    public Builder withNotifyId(String notifyId) {
      this.notifyId = notifyId;
      return this;
    }

    public Builder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public Builder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    public Builder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

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

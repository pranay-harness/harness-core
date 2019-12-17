package software.wings.service.impl.analysis;

import static software.wings.utils.Misc.replaceUnicodeWithDot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.iterator.PersistentRegularIterable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.beans.FeatureName;
import software.wings.beans.ServiceSecretKey.ServiceApiVersion;
import software.wings.sm.StateType;
import software.wings.utils.Misc;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by sriram_parthasarathy on 8/23/17.
 */
@Indexes({
  @Index(fields = { @Field("stateExecutionId")
                    , @Field("executionStatus") },
      options = @IndexOptions(unique = true, name = "task_Unique_Idx"))
  ,
      @Index(fields = {
        @Field("analysisType"), @Field("executionStatus"), @Field("timeSeriesAnalysisIteration")
      }, options = @IndexOptions(name = "timeSeriesAnalysisIterationIdx")), @Index(fields = {
        @Field("analysisType"), @Field("executionStatus"), @Field("logAnalysisIteration")
      }, options = @IndexOptions(name = "logAnalysisIterationIdx"))
})
@Data
@FieldNameConstants(innerTypeName = "AnalysisContextKeys")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity(value = "verificationServiceTask", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class AnalysisContext extends Base implements PersistentRegularIterable {
  private String accountId;
  private String workflowId;
  private String workflowExecutionId;
  @Indexed private String stateExecutionId;
  private String serviceId;
  private String predictiveCvConfigId;
  private int predictiveHistoryMinutes;
  private Map<String, String> controlNodes;
  private Map<String, String> testNodes;
  private String query;
  private boolean isSSL;
  private int appPort;
  private AnalysisComparisonStrategy comparisonStrategy;
  private int timeDuration;
  private StateType stateType;
  private String analysisServerConfigId;
  private String correlationId;
  private int smooth_window;
  private int tolerance;
  private String prevWorkflowExecutionId;
  private int minimumRequestsPerMinute;
  private int comparisonWindow;
  private int parallelProcesses;
  private Map<String, List<TimeSeries>> timeSeriesToCollect = new HashMap<>();
  private boolean runTillConvergence;
  private String delegateTaskId;
  private String envId;
  private String hostNameField;
  private MLAnalysisType analysisType;
  @Indexed private ExecutionStatus executionStatus;
  private long startDataCollectionMinute;
  // Collection interval in minutes
  private int collectionInterval;
  private ServiceApiVersion version = ServiceApiVersion.values()[ServiceApiVersion.values().length - 1];
  private DataCollectionInfo dataCollectionInfo;
  private DataCollectionInfoV2 dataCollectionInfov2;
  private boolean cvTasksCreated;

  private int retry;
  private final Map<FeatureName, Boolean> featureFlags = new HashMap<>();
  @Indexed private Long timeSeriesAnalysisIteration;
  @Indexed private Long logAnalysisIteration;
  @Indexed private Long cvTaskCreationIteration;

  @JsonIgnore
  @SchemaIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusWeeks(1).toInstant());

  private String managerVersion;

  @Builder
  private AnalysisContext(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, String entityYamlPath, boolean syncFromGit, String accountId, String workflowId,
      String workflowExecutionId, String stateExecutionId, String serviceId, String predictiveCvConfigId,
      int predictiveHistoryMinutes, Map<String, String> controlNodes, Map<String, String> testNodes, String query,
      boolean isSSL, int appPort, AnalysisComparisonStrategy comparisonStrategy, int timeDuration, StateType stateType,
      String analysisServerConfigId, String correlationId, int smooth_window, int tolerance,
      String prevWorkflowExecutionId, int minimumRequestsPerMinute, int comparisonWindow, int parallelProcesses,
      Map<String, List<TimeSeries>> timeSeriesToCollect, boolean runTillConvergence, String delegateTaskId,
      MLAnalysisType analysisType, ExecutionStatus executionStatus, String managerVersion, String envId,
      String hostNameField, int collectionInterval, long startDataCollectionMinute,
      DataCollectionInfo dataCollectionInfo) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath, syncFromGit);
    this.accountId = accountId;
    this.workflowId = workflowId;
    this.workflowExecutionId = workflowExecutionId;
    this.stateExecutionId = stateExecutionId;
    this.serviceId = serviceId;
    this.predictiveHistoryMinutes = predictiveHistoryMinutes;
    this.predictiveCvConfigId = predictiveCvConfigId;
    this.controlNodes = controlNodes == null ? new HashMap<>() : controlNodes;
    this.testNodes = testNodes == null ? new HashMap<>() : testNodes;
    this.query = query;
    this.isSSL = isSSL;
    this.appPort = appPort;
    this.comparisonStrategy = comparisonStrategy;
    this.timeDuration = timeDuration;
    this.stateType = stateType;
    this.analysisServerConfigId = analysisServerConfigId;
    this.correlationId = correlationId;
    this.smooth_window = smooth_window;
    this.tolerance = tolerance;
    this.prevWorkflowExecutionId = prevWorkflowExecutionId;
    this.minimumRequestsPerMinute = minimumRequestsPerMinute;
    this.comparisonWindow = comparisonWindow;
    this.parallelProcesses = parallelProcesses;
    this.timeSeriesToCollect = timeSeriesToCollect == null ? new HashMap<>() : timeSeriesToCollect;
    this.runTillConvergence = runTillConvergence;
    this.delegateTaskId = delegateTaskId;
    this.executionStatus = executionStatus == null ? ExecutionStatus.QUEUED : executionStatus;
    this.analysisType = analysisType;
    this.validUntil = Date.from(OffsetDateTime.now().plusWeeks(1).toInstant());
    this.version = ServiceApiVersion.values()[ServiceApiVersion.values().length - 1];
    this.retry = 0;
    this.managerVersion = managerVersion;
    this.envId = envId;
    this.hostNameField = hostNameField;
    this.collectionInterval = collectionInterval;
    this.startDataCollectionMinute = startDataCollectionMinute;
    this.dataCollectionInfo = dataCollectionInfo;
  }

  public LogClusterContext getClusterContext() {
    return LogClusterContext.builder()
        .appId(appId)
        .workflowId(workflowId)
        .workflowExecutionId(workflowExecutionId)
        .stateExecutionId(stateExecutionId)
        .serviceId(serviceId)
        .controlNodes(getControlNodes().keySet().stream().map(Misc::replaceUnicodeWithDot).collect(Collectors.toSet()))
        .testNodes(getTestNodes().keySet().stream().map(Misc::replaceUnicodeWithDot).collect(Collectors.toSet()))
        .query(query)
        .isSSL(isSSL)
        .appPort(appPort)
        .accountId(accountId)
        .stateType(stateType)
        .build();
  }

  @Override
  public void updateNextIteration(String fieldName, Long nextIteration) {
    if (AnalysisContextKeys.timeSeriesAnalysisIteration.equals(fieldName)) {
      this.timeSeriesAnalysisIteration = nextIteration;
      return;
    }
    if (AnalysisContextKeys.logAnalysisIteration.equals(fieldName)) {
      this.logAnalysisIteration = nextIteration;
      return;
    }
    if (AnalysisContextKeys.cvTaskCreationIteration.equals(fieldName)) {
      this.cvTaskCreationIteration = nextIteration;
      return;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    if (AnalysisContextKeys.timeSeriesAnalysisIteration.equals(fieldName)) {
      return this.timeSeriesAnalysisIteration;
    }
    if (AnalysisContextKeys.logAnalysisIteration.equals(fieldName)) {
      return this.logAnalysisIteration;
    }
    if (AnalysisContextKeys.cvTaskCreationIteration.equals(fieldName)) {
      return this.cvTaskCreationIteration;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  public Map<String, String> getControlNodes() {
    if (controlNodes == null) {
      return new HashMap<>();
    }
    return controlNodes;
  }

  public Map<String, String> getTestNodes() {
    if (testNodes == null) {
      return new HashMap<>();
    }
    return testNodes;
  }

  public void replaceUnicodeInControlNodesAndTestNodes() {
    controlNodes = updateNodesReplaceUniCode(getControlNodes());
    testNodes = updateNodesReplaceUniCode(getTestNodes());
  }

  private Map<String, String> updateNodesReplaceUniCode(Map<String, String> nodes) {
    Map<String, String> updatedNodes = new HashMap<>();
    nodes.forEach((host, groupName) -> updatedNodes.put(replaceUnicodeWithDot(host), groupName));
    return updatedNodes;
  }

  public void setFeatureFlag(FeatureName featureFlag, Boolean enabled) {
    featureFlags.put(featureFlag, enabled);
  }

  public boolean isFeatureFlagEnabled(FeatureName featureFlag) {
    return featureFlags.getOrDefault(featureFlag, false);
  }
}
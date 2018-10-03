package software.wings.service.impl.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.ServiceSecretKey.ServiceApiVersion;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sriram_parthasarathy on 8/23/17.
 */
@Entity(value = "verificationServiceTask", noClassnameStored = true)
@Indexes({
  @Index(fields = {
    @Field("workflowExecutionId"), @Field("stateExecutionId"), @Field("executionStatus")
  }, options = @IndexOptions(unique = true, name = "taskUniqueIdx"))
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisContext extends Base {
  private String accountId;
  private String workflowId;
  private String workflowExecutionId;
  private String stateExecutionId;
  private String serviceId;
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
  private MLAnalysisType analysisType;
  private @Indexed ExecutionStatus executionStatus;
  private @Indexed ServiceApiVersion version = ServiceApiVersion.values()[ServiceApiVersion.values().length - 1];

  private @Indexed int retry;

  @SchemaIgnore
  @JsonIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusWeeks(1).toInstant());

  @Builder
  private AnalysisContext(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, List<String> keywords, String entityYamlPath, boolean syncFromGit, String accountId,
      String workflowId, String workflowExecutionId, String stateExecutionId, String serviceId,
      Map<String, String> controlNodes, Map<String, String> testNodes, String query, boolean isSSL, int appPort,
      AnalysisComparisonStrategy comparisonStrategy, int timeDuration, StateType stateType,
      String analysisServerConfigId, String correlationId, int smooth_window, int tolerance,
      String prevWorkflowExecutionId, int minimumRequestsPerMinute, int comparisonWindow, int parallelProcesses,
      Map<String, List<TimeSeries>> timeSeriesToCollect, boolean runTillConvergence, String delegateTaskId,
      MLAnalysisType analysisType, ExecutionStatus executionStatus) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath, syncFromGit);
    this.accountId = accountId;
    this.workflowId = workflowId;
    this.workflowExecutionId = workflowExecutionId;
    this.stateExecutionId = stateExecutionId;
    this.serviceId = serviceId;
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
  }

  public LogClusterContext getClusterContext() {
    return LogClusterContext.builder()
        .appId(appId)
        .workflowId(workflowId)
        .workflowExecutionId(workflowExecutionId)
        .stateExecutionId(stateExecutionId)
        .serviceId(serviceId)
        .controlNodes(controlNodes.keySet())
        .testNodes(testNodes.keySet())
        .query(query)
        .isSSL(isSSL)
        .appPort(appPort)
        .accountId(accountId)
        .stateType(stateType)
        .build();
  }
}

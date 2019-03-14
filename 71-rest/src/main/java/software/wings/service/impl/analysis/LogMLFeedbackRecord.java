package software.wings.service.impl.analysis;

import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.service.impl.analysis.AnalysisServiceImpl.CLUSTER_TYPE;
import software.wings.service.impl.analysis.AnalysisServiceImpl.LogMLFeedbackType;
import software.wings.sm.StateType;

@Entity(value = "logMlFeedbackRecords", noClassnameStored = true)
@Indexes({
  @Index(fields = {
    @Field("applicationId"), @Field("stateExecutionId"), @Field("clusterType"), @Field("clusterLabel")
  }, options = @IndexOptions(unique = true, name = "logFeedbackUniqueIdx"))
})
@Data
@EqualsAndHashCode(callSuper = false)
public class LogMLFeedbackRecord extends Base {
  @NotEmpty @Indexed private String serviceId;

  private String workflowId;

  @Indexed private String workflowExecutionId;

  @Indexed private String stateExecutionId;

  private StateType stateType;

  @NotEmpty private int clusterLabel;

  @NotEmpty private AnalysisServiceImpl.CLUSTER_TYPE clusterType;

  @NotEmpty private AnalysisServiceImpl.LogMLFeedbackType logMLFeedbackType;

  @NotEmpty private String logMessage;

  @NotEmpty private String logMD5Hash;

  private String cvConfigId;

  private String comment;

  @Builder
  public LogMLFeedbackRecord(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, String serviceId, String workflowId,
      String workflowExecutionId, String stateExecutionId, StateType stateType, int clusterLabel,
      CLUSTER_TYPE clusterType, LogMLFeedbackType logMLFeedbackType, String logMessage, String logMD5Hash,
      String comment, String cvConfigId) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.serviceId = serviceId;
    this.workflowId = workflowId;
    this.workflowExecutionId = workflowExecutionId;
    this.stateExecutionId = stateExecutionId;
    this.stateType = stateType;
    this.clusterLabel = clusterLabel;
    this.clusterType = clusterType;
    this.logMLFeedbackType = logMLFeedbackType;
    this.logMessage = logMessage;
    this.logMD5Hash = logMD5Hash;
    this.comment = comment;
    this.cvConfigId = cvConfigId;
  }
}

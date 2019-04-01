package software.wings.service.impl.analysis;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.cxf.ws.addressing.ContextUtils.generateUUID;
import static software.wings.service.impl.GoogleDataStoreServiceImpl.addFieldIfNotEmpty;
import static software.wings.service.impl.GoogleDataStoreServiceImpl.readLong;
import static software.wings.service.impl.GoogleDataStoreServiceImpl.readString;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Key;

import io.harness.beans.EmbeddedUser;
import io.harness.persistence.GoogleDataStoreAware;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
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
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class LogMLFeedbackRecord extends Base implements GoogleDataStoreAware {
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

  private String supervisedLabel;

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

  @Override
  public com.google.cloud.datastore.Entity convertToCloudStorageEntity(Datastore datastore) {
    Key taskKey = datastore.newKeyFactory()
                      .setKind(this.getClass().getAnnotation(org.mongodb.morphia.annotations.Entity.class).value())
                      .newKey(this.getUuid() == null ? generateUUID() : this.getUuid());
    com.google.cloud.datastore.Entity.Builder recordBuilder = com.google.cloud.datastore.Entity.newBuilder(taskKey);
    addFieldIfNotEmpty(recordBuilder, "appId", appId, false);
    addFieldIfNotEmpty(recordBuilder, "workflowId", workflowId, false);
    addFieldIfNotEmpty(recordBuilder, "workflowExecutionId", workflowExecutionId, false);
    addFieldIfNotEmpty(recordBuilder, "serviceId", serviceId, false);
    addFieldIfNotEmpty(recordBuilder, "stateExecutionId", stateExecutionId, false);
    addFieldIfNotEmpty(recordBuilder, "cvConfigId", cvConfigId, false);
    addFieldIfNotEmpty(recordBuilder, "clusterType", clusterType.name(), false);
    addFieldIfNotEmpty(recordBuilder, "logMLFeedbackType", logMLFeedbackType.name(), true);
    addFieldIfNotEmpty(recordBuilder, "logMD5Hash", logMD5Hash, true);
    addFieldIfNotEmpty(recordBuilder, "logMessage", logMessage, true);
    addFieldIfNotEmpty(recordBuilder, "supervisedLabel", supervisedLabel, true);

    if (stateType != null) {
      addFieldIfNotEmpty(recordBuilder, "stateType", stateType.name(), true);
    }

    if (isNotEmpty(comment)) {
      addFieldIfNotEmpty(recordBuilder, "comment", comment, true);
    }

    return recordBuilder.build();
  }

  @Override
  public GoogleDataStoreAware readFromCloudStorageEntity(com.google.cloud.datastore.Entity entity) {
    final LogMLFeedbackRecord dataRecord =
        LogMLFeedbackRecord.builder()
            .appId(readString(entity, "appId"))
            .uuid(entity.getKey().getName())
            .logMessage(readString(entity, "logMessage"))
            .logMD5Hash(readString(entity, "logMD5Hash"))
            .workflowId(readString(entity, "workflowId"))
            .workflowExecutionId(readString(entity, "workflowExecutionId"))
            .serviceId(readString(entity, "serviceId"))
            .stateExecutionId(readString(entity, "stateExecutionId"))
            .clusterLabel((int) readLong(entity, "clusterLabel"))
            .clusterType(CLUSTER_TYPE.valueOf(readString(entity, "clusterType")))
            .logMLFeedbackType(LogMLFeedbackType.valueOf(readString(entity, "logMLFeedbackType")))
            .cvConfigId(readString(entity, "cvConfigId"))
            .build();

    final String comment = readString(entity, "comment");
    if (isNotEmpty(comment)) {
      dataRecord.setComment(comment);
    }

    final String stateType = readString(entity, "stateType");
    if (isNotEmpty(stateType)) {
      dataRecord.setStateType(StateType.valueOf(stateType));
    }

    final String supervisedLabel = readString(entity, "supervisedLabel");
    if (isNotEmpty(supervisedLabel)) {
      dataRecord.setSupervisedLabel(supervisedLabel);
    }

    return dataRecord;
  }
}

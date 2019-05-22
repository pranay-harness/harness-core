package software.wings.service.impl.analysis;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.System.currentTimeMillis;
import static software.wings.service.impl.GoogleDataStoreServiceImpl.addFieldIfNotEmpty;
import static software.wings.service.impl.GoogleDataStoreServiceImpl.readLong;
import static software.wings.service.impl.GoogleDataStoreServiceImpl.readString;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Key;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.EmbeddedUser;
import io.harness.persistence.GoogleDataStoreAware;
import io.harness.serializer.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.PrePersist;
import software.wings.security.ThreadLocalUserProvider;
import software.wings.service.impl.analysis.AnalysisServiceImpl.CLUSTER_TYPE;

@Entity(value = "cvFeedbackRecords", noClassnameStored = true)
@Data
@Builder
@FieldNameConstants(innerTypeName = "CVFeedbackRecordKeys")
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = false)
public class CVFeedbackRecord implements GoogleDataStoreAware {
  @Id private String uuid;
  @NotEmpty @Indexed private String serviceId;

  @NotEmpty @Indexed private String envId;

  @Indexed private String stateExecutionId;

  @Indexed private String cvConfigId;

  @NotEmpty private int clusterLabel;

  @NotEmpty private AnalysisServiceImpl.CLUSTER_TYPE clusterType;

  @NotEmpty private String logMessage;

  private String comment;

  private String supervisedLabel;

  private FeedbackPriority priority;

  private String jiraLink;

  private long analysisMinute;

  private FeedbackAction actionTaken;

  private long createdAt;
  private long lastUpdatedAt;

  @SchemaIgnore private EmbeddedUser createdBy;
  @SchemaIgnore private EmbeddedUser lastUpdatedBy;

  @PrePersist
  public void onSave() {
    if (uuid == null) {
      uuid = generateUuid();
    }

    EmbeddedUser embeddedUser = ThreadLocalUserProvider.threadLocalUser();
    if (createdBy == null) {
      createdBy = embeddedUser;
    }

    final long currentTime = currentTimeMillis();

    if (createdAt == 0) {
      createdAt = currentTime;
    }
    lastUpdatedAt = currentTime;
    lastUpdatedBy = embeddedUser;
  }

  @Override
  public com.google.cloud.datastore.Entity convertToCloudStorageEntity(Datastore datastore) {
    onSave();
    Key taskKey = datastore.newKeyFactory()
                      .setKind(this.getClass().getAnnotation(org.mongodb.morphia.annotations.Entity.class).value())
                      .newKey(this.getUuid() == null ? generateUuid() : this.getUuid());
    com.google.cloud.datastore.Entity.Builder recordBuilder = com.google.cloud.datastore.Entity.newBuilder(taskKey);

    addFieldIfNotEmpty(recordBuilder, CVFeedbackRecordKeys.serviceId, serviceId, false);
    addFieldIfNotEmpty(recordBuilder, CVFeedbackRecordKeys.envId, envId, false);
    addFieldIfNotEmpty(recordBuilder, CVFeedbackRecordKeys.stateExecutionId, stateExecutionId, false);
    addFieldIfNotEmpty(recordBuilder, CVFeedbackRecordKeys.cvConfigId, cvConfigId, false);
    addFieldIfNotEmpty(recordBuilder, CVFeedbackRecordKeys.clusterType, clusterType.name(), false);

    addFieldIfNotEmpty(recordBuilder, CVFeedbackRecordKeys.clusterLabel, clusterLabel, true);

    addFieldIfNotEmpty(recordBuilder, CVFeedbackRecordKeys.logMessage, logMessage, true);
    addFieldIfNotEmpty(recordBuilder, CVFeedbackRecordKeys.supervisedLabel, supervisedLabel, true);

    addFieldIfNotEmpty(recordBuilder, CVFeedbackRecordKeys.priority, priority.name(), false);
    addFieldIfNotEmpty(recordBuilder, CVFeedbackRecordKeys.analysisMinute, analysisMinute, true);
    addFieldIfNotEmpty(recordBuilder, CVFeedbackRecordKeys.actionTaken, actionTaken.name(), false);
    addFieldIfNotEmpty(recordBuilder, CVFeedbackRecordKeys.priority, priority.name(), true);
    addFieldIfNotEmpty(recordBuilder, CVFeedbackRecordKeys.jiraLink, jiraLink, false);

    addFieldIfNotEmpty(recordBuilder, CVFeedbackRecordKeys.createdAt, createdAt, true);
    addFieldIfNotEmpty(recordBuilder, CVFeedbackRecordKeys.lastUpdatedAt, lastUpdatedAt, true);
    addFieldIfNotEmpty(recordBuilder, CVFeedbackRecordKeys.createdBy, JsonUtils.asJson(createdBy), true);
    addFieldIfNotEmpty(recordBuilder, CVFeedbackRecordKeys.lastUpdatedBy, JsonUtils.asJson(lastUpdatedBy), true);
    return recordBuilder.build();
  }

  @Override
  public GoogleDataStoreAware readFromCloudStorageEntity(com.google.cloud.datastore.Entity entity) {
    final CVFeedbackRecord record =
        CVFeedbackRecord.builder()
            .serviceId(readString(entity, CVFeedbackRecordKeys.serviceId))
            .envId(readString(entity, CVFeedbackRecordKeys.envId))
            .stateExecutionId(readString(entity, CVFeedbackRecordKeys.stateExecutionId))
            .cvConfigId(readString(entity, CVFeedbackRecordKeys.cvConfigId))
            .clusterLabel((int) (readLong(entity, CVFeedbackRecordKeys.clusterLabel)))
            .clusterType(CLUSTER_TYPE.valueOf(readString(entity, CVFeedbackRecordKeys.clusterType)))
            .logMessage(readString(entity, CVFeedbackRecordKeys.logMessage))
            .analysisMinute(readLong(entity, CVFeedbackRecordKeys.analysisMinute))
            .comment(readString(entity, CVFeedbackRecordKeys.comment))
            .actionTaken(FeedbackAction.valueOf(readString(entity, CVFeedbackRecordKeys.actionTaken)))
            .uuid(entity.getKey().getName())
            .priority(FeedbackPriority.valueOf(readString(entity, CVFeedbackRecordKeys.priority)))
            .jiraLink(readString(entity, CVFeedbackRecordKeys.jiraLink))
            .createdAt(readLong(entity, CVFeedbackRecordKeys.createdAt))
            .lastUpdatedAt(readLong(entity, CVFeedbackRecordKeys.lastUpdatedAt))
            .supervisedLabel(readString(entity, CVFeedbackRecordKeys.supervisedLabel))
            .build();

    String createdBy = readString(entity, CVFeedbackRecordKeys.createdBy);
    if (isNotEmpty(createdBy)) {
      record.setCreatedBy(JsonUtils.asObject(createdBy, EmbeddedUser.class));
    }
    String lastUpdatedBy = readString(entity, CVFeedbackRecordKeys.lastUpdatedBy);
    if (isNotEmpty(lastUpdatedBy)) {
      record.setLastUpdatedBy(JsonUtils.asObject(lastUpdatedBy, EmbeddedUser.class));
    }
    return record;
  }
}

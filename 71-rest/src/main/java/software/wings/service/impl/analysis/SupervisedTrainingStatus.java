package software.wings.service.impl.analysis;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static software.wings.service.impl.GoogleDataStoreServiceImpl.addFieldIfNotEmpty;
import static software.wings.service.impl.GoogleDataStoreServiceImpl.readBoolean;
import static software.wings.service.impl.GoogleDataStoreServiceImpl.readLong;
import static software.wings.service.impl.GoogleDataStoreServiceImpl.readString;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Key;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.GoogleDataStoreAware;
import io.harness.persistence.UuidAware;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

/**
 * @author Praveen
 */
@Entity(value = "supervisedTrainingStatus", noClassnameStored = true)
@HarnessEntity(exportable = false)
@Data
@Builder
@FieldNameConstants(innerTypeName = "SupervisedTrainingStatusKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class SupervisedTrainingStatus implements GoogleDataStoreAware, CreatedAtAware, UuidAware {
  private String serviceId;
  private boolean isEmbeddingReady;
  private boolean isSupervisedReady;
  @Id String uuid;
  private long createdAt;

  @Override
  public com.google.cloud.datastore.Entity convertToCloudStorageEntity(Datastore datastore) {
    Key taskKey = datastore.newKeyFactory()
                      .setKind(this.getClass().getAnnotation(org.mongodb.morphia.annotations.Entity.class).value())
                      .newKey(this.uuid == null ? generateUuid() : this.uuid);

    com.google.cloud.datastore.Entity.Builder recordBuilder = com.google.cloud.datastore.Entity.newBuilder(taskKey);
    addFieldIfNotEmpty(recordBuilder, SupervisedTrainingStatusKeys.serviceId, serviceId, false);
    addFieldIfNotEmpty(recordBuilder, SupervisedTrainingStatusKeys.isEmbeddingReady, isEmbeddingReady, true);
    addFieldIfNotEmpty(recordBuilder, SupervisedTrainingStatusKeys.isSupervisedReady, isSupervisedReady, true);
    addFieldIfNotEmpty(recordBuilder, SupervisedTrainingStatusKeys.createdAt, createdAt, true);
    return recordBuilder.build();
  }

  @Override
  public GoogleDataStoreAware readFromCloudStorageEntity(com.google.cloud.datastore.Entity entity) {
    return SupervisedTrainingStatus.builder()
        .uuid(entity.getKey().getName())
        .serviceId(readString(entity, SupervisedTrainingStatusKeys.serviceId))
        .isSupervisedReady(readBoolean(entity, SupervisedTrainingStatusKeys.isSupervisedReady))
        .isEmbeddingReady(readBoolean(entity, SupervisedTrainingStatusKeys.isEmbeddingReady))
        .createdAt(readLong(entity, SupervisedTrainingStatusKeys.createdAt))
        .build();
  }
}

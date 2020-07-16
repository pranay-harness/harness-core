package io.harness.cvng.core.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;

import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import javax.validation.constraints.NotNull;

@Data
@Builder
@FieldNameConstants(innerTypeName = "DeletedCVConfigKeys")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "deletedCvConfigs")
@HarnessEntity(exportable = true)
public class DeletedCVConfig
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess, PersistentRegularIterable {
  @Id private String uuid;
  @NotNull @FdIndex private String accountId;
  @FdIndex private long dataCollectionTaskIteration;

  private CVConfig cvConfig;
  private long createdAt;
  private long lastUpdatedAt;
  private String dataCollectionTaskId;

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    if (DeletedCVConfigKeys.dataCollectionTaskIteration.equals(fieldName)) {
      this.dataCollectionTaskIteration = nextIteration;
      return;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    if (DeletedCVConfigKeys.dataCollectionTaskIteration.equals(fieldName)) {
      return this.dataCollectionTaskIteration;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }
}

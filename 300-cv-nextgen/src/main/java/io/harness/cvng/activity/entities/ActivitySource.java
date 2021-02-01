package io.harness.cvng.activity.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.cvng.beans.activity.ActivitySourceDTO;
import io.harness.cvng.beans.activity.ActivitySourceType;
import io.harness.cvng.core.entities.CVConfig.CVConfigKeys;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdUniqueIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@NoArgsConstructor
@SuperBuilder
@FieldNameConstants(innerTypeName = "ActivitySourceKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "activitySources")
@HarnessEntity(exportable = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
public abstract class ActivitySource
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, PersistentRegularIterable {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_idx")
                 .unique(true)
                 .field(ActivitySourceKeys.accountId)
                 .field(ActivitySourceKeys.orgIdentifier)
                 .field(ActivitySourceKeys.projectIdentifier)
                 .field(ActivitySourceKeys.identifier)
                 .build())
        .build();
  }

  @Id String uuid;
  long createdAt;
  long lastUpdatedAt;

  @NotNull @FdIndex String accountId;
  @NotNull String orgIdentifier;
  @NotNull String projectIdentifier;
  @NotNull @FdUniqueIndex String identifier;
  @NotNull String name;
  @NotNull ActivitySourceType type;

  @FdIndex String dataCollectionTaskId;
  @FdIndex Long dataCollectionTaskIteration;

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    if (CVConfigKeys.dataCollectionTaskIteration.equals(fieldName)) {
      this.dataCollectionTaskIteration = nextIteration;
      return;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    if (CVConfigKeys.dataCollectionTaskIteration.equals(fieldName)) {
      return this.dataCollectionTaskIteration;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  public abstract ActivitySourceDTO toDTO();

  public void validate() {
    Preconditions.checkNotNull(accountId);
    Preconditions.checkNotNull(orgIdentifier);
    Preconditions.checkNotNull(projectIdentifier);
    Preconditions.checkNotNull(identifier);
    Preconditions.checkNotNull(type);
    Preconditions.checkNotNull(name);
    this.validateParams();
  }

  protected abstract void validateParams();
}

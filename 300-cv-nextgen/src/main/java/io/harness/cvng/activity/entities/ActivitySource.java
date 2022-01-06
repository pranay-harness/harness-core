/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.activity.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.activity.ActivitySourceDTO;
import io.harness.cvng.beans.activity.ActivitySourceDTO.ActivitySourceDTOBuilder;
import io.harness.cvng.beans.activity.ActivitySourceType;
import io.harness.cvng.core.services.api.UpdatableEntity;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
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
import org.mongodb.morphia.query.UpdateOperations;

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
@OwnedBy(HarnessTeam.CV)
@StoreIn(DbAliases.CVNG)
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

  @NotNull String accountId;
  @NotNull String orgIdentifier;
  @NotNull String projectIdentifier;
  @NotNull String identifier;
  @NotNull String name;
  @NotNull ActivitySourceType type;

  @FdIndex String dataCollectionTaskId;
  @FdIndex Long dataCollectionTaskIteration;

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    if (ActivitySourceKeys.dataCollectionTaskIteration.equals(fieldName)) {
      this.dataCollectionTaskIteration = nextIteration;
      return;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    if (ActivitySourceKeys.dataCollectionTaskIteration.equals(fieldName)) {
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

  protected <C extends ActivitySourceDTO, B extends ActivitySourceDTOBuilder<C, B>> ActivitySourceDTOBuilder<C, B>
  fillCommon(ActivitySourceDTOBuilder<C, B> activitySourceDTOBuilder) {
    activitySourceDTOBuilder.identifier(identifier);
    activitySourceDTOBuilder.name(name);
    activitySourceDTOBuilder.orgIdentifier(orgIdentifier);
    activitySourceDTOBuilder.projectIdentifier(projectIdentifier);
    activitySourceDTOBuilder.lastUpdatedAt(lastUpdatedAt);
    activitySourceDTOBuilder.createdAt(createdAt);
    return activitySourceDTOBuilder;
  }

  public abstract static class ActivitySourceUpdatableEntity<T extends ActivitySource, D extends ActivitySourceDTO>
      implements UpdatableEntity<T, D> {
    public void setCommonOperations(UpdateOperations<T> updateOperations, D dto) {
      updateOperations.set(ActivitySourceKeys.name, dto.getName()).unset(ActivitySourceKeys.dataCollectionTaskId);
    }
  }
}

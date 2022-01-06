/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EmbeddedUser;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.mongo.index.SortCompoundMongoIndex;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

/**
 * Created by rishi on 10/13/16.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "EntityVersionKeys")
@TargetModule(HarnessModule._957_CG_BEANS)
public class EntityVersion extends Base {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(SortCompoundMongoIndex.builder()
                 .name("app_type_uuid_createdAt")
                 .field(EntityVersionKeys.appId)
                 .field(EntityVersionKeys.entityType)
                 .field(EntityVersionKeys.entityUuid)
                 .descSortField(EntityVersionKeys.createdAt)
                 .build())
        .add(SortCompoundMongoIndex.builder()
                 .name("app_type_uuid_version")
                 .field(EntityVersionKeys.appId)
                 .field(EntityVersionKeys.entityType)
                 .field(EntityVersionKeys.entityUuid)
                 .descSortField(EntityVersionKeys.version)
                 .build())
        .build();
  }

  public static final Integer INITIAL_VERSION = 1;

  private EntityType entityType;
  private String entityName;
  private ChangeType changeType;
  private String entityUuid;
  private String entityParentUuid;
  private String entityData;
  private Integer version;
  @FdIndex private String accountId;

  public EntityType getEntityType() {
    return entityType;
  }

  public void setEntityType(EntityType entityType) {
    this.entityType = entityType;
  }

  public String getEntityUuid() {
    return entityUuid;
  }

  public void setEntityUuid(String entityUuid) {
    this.entityUuid = entityUuid;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getEntityData() {
    return entityData;
  }

  public void setEntityData(String entityData) {
    this.entityData = entityData;
  }

  /**
   * Getter for property 'entityParentUuid'.
   *
   * @return Value for property 'entityParentUuid'.
   */
  public String getEntityParentUuid() {
    return entityParentUuid;
  }

  /**
   * Setter for property 'entityParentUuid'.
   *
   * @param entityParentUuid Value to set for property 'entityParentUuid'.
   */
  public void setEntityParentUuid(String entityParentUuid) {
    this.entityParentUuid = entityParentUuid;
  }

  /**
   * Getter for property 'entityName'.
   *
   * @return Value for property 'entityName'.
   */
  public String getEntityName() {
    return entityName;
  }

  /**
   * Setter for property 'entityName'.
   *
   * @param entityName Value to set for property 'entityName'.
   */
  public void setEntityName(String entityName) {
    this.entityName = entityName;
  }

  /**
   * Getter for property 'changeType'.
   *
   * @return Value for property 'changeType'.
   */
  public ChangeType getChangeType() {
    return changeType;
  }

  /**
   * Setter for property 'changeType'.
   *
   * @param changeType Value to set for property 'changeType'.
   */
  public void setChangeType(ChangeType changeType) {
    this.changeType = changeType;
  }

  public enum ChangeType { CREATED, UPDATED }

  public static final class Builder {
    private EntityType entityType;
    private String entityName;
    private ChangeType changeType;
    private String entityUuid;
    private String entityParentUuid;
    private String entityData;
    private Integer version;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private String accountId;

    private Builder() {}

    public static Builder anEntityVersion() {
      return new Builder();
    }

    public Builder withEntityType(EntityType entityType) {
      this.entityType = entityType;
      return this;
    }

    public Builder withEntityName(String entityName) {
      this.entityName = entityName;
      return this;
    }

    public Builder withChangeType(ChangeType changeType) {
      this.changeType = changeType;
      return this;
    }

    public Builder withEntityUuid(String entityUuid) {
      this.entityUuid = entityUuid;
      return this;
    }

    public Builder withEntityParentUuid(String entityParentUuid) {
      this.entityParentUuid = entityParentUuid;
      return this;
    }

    public Builder withEntityData(String entityData) {
      this.entityData = entityData;
      return this;
    }

    public Builder withVersion(Integer version) {
      this.version = version;
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

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder but() {
      return anEntityVersion()
          .withEntityType(entityType)
          .withEntityName(entityName)
          .withChangeType(changeType)
          .withEntityUuid(entityUuid)
          .withEntityParentUuid(entityParentUuid)
          .withEntityData(entityData)
          .withVersion(version)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withAccountId(accountId);
    }

    public EntityVersion build() {
      EntityVersion entityVersion = new EntityVersion();
      entityVersion.setEntityType(entityType);
      entityVersion.setEntityName(entityName);
      entityVersion.setChangeType(changeType);
      entityVersion.setEntityUuid(entityUuid);
      entityVersion.setEntityParentUuid(entityParentUuid);
      entityVersion.setEntityData(entityData);
      entityVersion.setVersion(version);
      entityVersion.setUuid(uuid);
      entityVersion.setAppId(appId);
      entityVersion.setCreatedBy(createdBy);
      entityVersion.setCreatedAt(createdAt);
      entityVersion.setLastUpdatedBy(lastUpdatedBy);
      entityVersion.setLastUpdatedAt(lastUpdatedAt);
      entityVersion.setAccountId(accountId);
      return entityVersion;
    }
  }

  public static final class EntityVersionKeys {
    private EntityVersionKeys() {}
    // Temporary
    public static final String appId = "appId";
    public static final String createdAt = "createdAt";
    public static final String uuid = "uuid";
  }
}

package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

/**
 * Created by peeyushaggarwal on 11/2/16.
 */
@Indexes(@Index(
    fields = { @Field("entityType")
               , @Field("entityUuid"), @Field("version") }, options = @IndexOptions(unique = true)))
@Entity(value = "entityVersions", noClassnameStored = true)
public class EntityVersionCollection extends EntityVersion {
  public static final class Builder {
    private EntityType entityType;
    private String entityUuid;
    private String entityData;
    private Integer version;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    public static Builder anEntityVersionCollection() {
      return new Builder();
    }

    public Builder withEntityType(EntityType entityType) {
      this.entityType = entityType;
      return this;
    }

    public Builder withEntityUuid(String entityUuid) {
      this.entityUuid = entityUuid;
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

    public Builder but() {
      return anEntityVersionCollection()
          .withEntityType(entityType)
          .withEntityUuid(entityUuid)
          .withEntityData(entityData)
          .withVersion(version)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    public EntityVersionCollection build() {
      EntityVersionCollection entityVersionCollection = new EntityVersionCollection();
      entityVersionCollection.setEntityType(entityType);
      entityVersionCollection.setEntityUuid(entityUuid);
      entityVersionCollection.setEntityData(entityData);
      entityVersionCollection.setVersion(version);
      entityVersionCollection.setUuid(uuid);
      entityVersionCollection.setAppId(appId);
      entityVersionCollection.setCreatedBy(createdBy);
      entityVersionCollection.setCreatedAt(createdAt);
      entityVersionCollection.setLastUpdatedBy(lastUpdatedBy);
      entityVersionCollection.setLastUpdatedAt(lastUpdatedAt);
      return entityVersionCollection;
    }
  }
}

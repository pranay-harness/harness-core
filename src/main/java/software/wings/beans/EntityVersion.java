package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

/**
 * Created by rishi on 10/13/16.
 */
@Indexes(@Index(
    fields = { @Field("entityType")
               , @Field("entityUuid"), @Field("version") }, options = @IndexOptions(unique = true)))
@Entity(value = "entityVersions", noClassnameStored = true)
public class EntityVersion extends Base {
  public static final Integer INITIAL_VERSION = 1;

  private EntityType entityType;
  private String entityUuid;
  private String entityData;
  private Integer version;

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

  public static final class EntityVersionBuilder {
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

    private EntityVersionBuilder() {}

    public static EntityVersionBuilder anEntityVersion() {
      return new EntityVersionBuilder();
    }

    public EntityVersionBuilder withEntityType(EntityType entityType) {
      this.entityType = entityType;
      return this;
    }

    public EntityVersionBuilder withEntityUuid(String entityUuid) {
      this.entityUuid = entityUuid;
      return this;
    }

    public EntityVersionBuilder withEntityData(String entityData) {
      this.entityData = entityData;
      return this;
    }

    public EntityVersionBuilder withVersion(Integer version) {
      this.version = version;
      return this;
    }

    public EntityVersionBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public EntityVersionBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public EntityVersionBuilder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public EntityVersionBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public EntityVersionBuilder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public EntityVersionBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public EntityVersion build() {
      EntityVersion entityVersion = new EntityVersion();
      entityVersion.setEntityType(entityType);
      entityVersion.setEntityUuid(entityUuid);
      entityVersion.setEntityData(entityData);
      entityVersion.setVersion(version);
      entityVersion.setUuid(uuid);
      entityVersion.setAppId(appId);
      entityVersion.setCreatedBy(createdBy);
      entityVersion.setCreatedAt(createdAt);
      entityVersion.setLastUpdatedBy(lastUpdatedBy);
      entityVersion.setLastUpdatedAt(lastUpdatedAt);
      return entityVersion;
    }
  }
}

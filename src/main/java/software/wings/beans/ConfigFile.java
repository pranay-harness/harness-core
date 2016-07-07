package software.wings.beans;

import static software.wings.beans.ChecksumType.MD5;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

import java.util.Objects;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 4/12/16.
 */
@Entity(value = "configFiles", noClassnameStored = true)
@Indexes(@Index(
    fields = { @Field("entityId")
               , @Field("templateId"), @Field("name") }, options = @IndexOptions(unique = true)))
public class ConfigFile extends BaseFile {
  /**
   * The constant DEFAULT_TEMPLATE_ID.
   */
  public static final String DEFAULT_TEMPLATE_ID = "__TEMPLATE_ID";

  @FormDataParam("templateId") @DefaultValue(DEFAULT_TEMPLATE_ID) private String templateId;

  @FormDataParam("entityType") @NotNull private EntityType entityType;

  @FormDataParam("entityId") @NotEmpty private String entityId;

  @FormDataParam("relativePath") private String relativePath;

  /**
   * The enum Entity type.
   */
  public enum EntityType {
    /**
     * Service entity type.
     */
    SERVICE, /**
              * Environment entity type.
              */
    ENVIRONMENT, /**
                  * Tag entity type.
                  */
    TAG, /**
          * Host entity type.
          */
    HOST
  }

  /**
   * Gets entity id.
   *
   * @return the entity id
   */
  public String getEntityId() {
    return entityId;
  }

  /**
   * Sets entity id.
   *
   * @param entityId the entity id
   */
  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  /**
   * Gets relative path.
   *
   * @return the relative path
   */
  public String getRelativePath() {
    return relativePath;
  }

  /**
   * Sets relative path.
   *
   * @param relativePath the relative path
   */
  public void setRelativePath(String relativePath) {
    this.relativePath = relativePath;
  }

  /**
   * Gets template id.
   *
   * @return the template id
   */
  public String getTemplateId() {
    return templateId;
  }

  /**
   * Sets template id.
   *
   * @param templateId the template id
   */
  public void setTemplateId(String templateId) {
    this.templateId = templateId;
  }

  /**
   * Gets entity type.
   *
   * @return the entity type
   */
  public EntityType getEntityType() {
    return entityType;
  }

  /**
   * Sets entity type.
   *
   * @param entityType the entity type
   */
  public void setEntityType(EntityType entityType) {
    this.entityType = entityType;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(templateId, entityType, entityId, relativePath);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final ConfigFile other = (ConfigFile) obj;
    return Objects.equals(this.templateId, other.templateId) && Objects.equals(this.entityType, other.entityType)
        && Objects.equals(this.entityId, other.entityId) && Objects.equals(this.relativePath, other.relativePath);
  }

  /**
   * The type Config file builder.
   */
  public static final class ConfigFileBuilder {
    private String templateId;
    private EntityType entityType;
    private String entityId;
    private String relativePath;
    private String fileUuid;
    private String name;
    private String mimeType;
    private long size;
    private ChecksumType checksumType = MD5;
    private String checksum;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private ConfigFileBuilder() {}

    /**
     * A config file config file builder.
     *
     * @return the config file builder
     */
    public static ConfigFileBuilder aConfigFile() {
      return new ConfigFileBuilder();
    }

    /**
     * With template id config file builder.
     *
     * @param templateId the template id
     * @return the config file builder
     */
    public ConfigFileBuilder withTemplateId(String templateId) {
      this.templateId = templateId;
      return this;
    }

    /**
     * With entity type config file builder.
     *
     * @param entityType the entity type
     * @return the config file builder
     */
    public ConfigFileBuilder withEntityType(EntityType entityType) {
      this.entityType = entityType;
      return this;
    }

    /**
     * With entity id config file builder.
     *
     * @param entityId the entity id
     * @return the config file builder
     */
    public ConfigFileBuilder withEntityId(String entityId) {
      this.entityId = entityId;
      return this;
    }

    /**
     * With relative path config file builder.
     *
     * @param relativePath the relative path
     * @return the config file builder
     */
    public ConfigFileBuilder withRelativePath(String relativePath) {
      this.relativePath = relativePath;
      return this;
    }

    /**
     * With file uuid config file builder.
     *
     * @param fileUuid the file uuid
     * @return the config file builder
     */
    public ConfigFileBuilder withFileUuid(String fileUuid) {
      this.fileUuid = fileUuid;
      return this;
    }

    /**
     * With name config file builder.
     *
     * @param name the name
     * @return the config file builder
     */
    public ConfigFileBuilder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With mime type config file builder.
     *
     * @param mimeType the mime type
     * @return the config file builder
     */
    public ConfigFileBuilder withMimeType(String mimeType) {
      this.mimeType = mimeType;
      return this;
    }

    /**
     * With size config file builder.
     *
     * @param size the size
     * @return the config file builder
     */
    public ConfigFileBuilder withSize(long size) {
      this.size = size;
      return this;
    }

    /**
     * With checksum type config file builder.
     *
     * @param checksumType the checksum type
     * @return the config file builder
     */
    public ConfigFileBuilder withChecksumType(ChecksumType checksumType) {
      this.checksumType = checksumType;
      return this;
    }

    /**
     * With checksum config file builder.
     *
     * @param checksum the checksum
     * @return the config file builder
     */
    public ConfigFileBuilder withChecksum(String checksum) {
      this.checksum = checksum;
      return this;
    }

    /**
     * With uuid config file builder.
     *
     * @param uuid the uuid
     * @return the config file builder
     */
    public ConfigFileBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id config file builder.
     *
     * @param appId the app id
     * @return the config file builder
     */
    public ConfigFileBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by config file builder.
     *
     * @param createdBy the created by
     * @return the config file builder
     */
    public ConfigFileBuilder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at config file builder.
     *
     * @param createdAt the created at
     * @return the config file builder
     */
    public ConfigFileBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by config file builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the config file builder
     */
    public ConfigFileBuilder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at config file builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the config file builder
     */
    public ConfigFileBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With active config file builder.
     *
     * @param active the active
     * @return the config file builder
     */
    public ConfigFileBuilder withActive(boolean active) {
      this.active = active;
      return this;
    }

    /**
     * But config file builder.
     *
     * @return the config file builder
     */
    public ConfigFileBuilder but() {
      return aConfigFile()
          .withTemplateId(templateId)
          .withEntityType(entityType)
          .withEntityId(entityId)
          .withRelativePath(relativePath)
          .withFileUuid(fileUuid)
          .withName(name)
          .withMimeType(mimeType)
          .withSize(size)
          .withChecksumType(checksumType)
          .withChecksum(checksum)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    /**
     * Build config file.
     *
     * @return the config file
     */
    public ConfigFile build() {
      ConfigFile configFile = new ConfigFile();
      configFile.setTemplateId(templateId);
      configFile.setEntityType(entityType);
      configFile.setEntityId(entityId);
      configFile.setRelativePath(relativePath);
      configFile.setFileUuid(fileUuid);
      configFile.setName(name);
      configFile.setMimeType(mimeType);
      configFile.setSize(size);
      configFile.setChecksumType(checksumType);
      configFile.setChecksum(checksum);
      configFile.setUuid(uuid);
      configFile.setAppId(appId);
      configFile.setCreatedBy(createdBy);
      configFile.setCreatedAt(createdAt);
      configFile.setLastUpdatedBy(lastUpdatedBy);
      configFile.setLastUpdatedAt(lastUpdatedAt);
      configFile.setActive(active);
      return configFile;
    }
  }
}

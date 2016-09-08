package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.utils.validation.Create;

import java.util.List;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;

/**
 * Created by anubhaw on 4/12/16.
 */
@Entity(value = "configFiles", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("entityId")
                           , @Field("templateId"), @Field("relativeFilePath") },
    options = @IndexOptions(unique = true)))
public class ConfigFile extends BaseFile {
  /**
   * The constant DEFAULT_TEMPLATE_ID.
   */
  public static final String DEFAULT_TEMPLATE_ID = "__TEMPLATE_ID";

  @FormDataParam("templateId") @DefaultValue(DEFAULT_TEMPLATE_ID) private String templateId;

  @FormDataParam("envId") @NotEmpty(groups = {Create.class}) private String envId;

  @FormDataParam("entityType") @NotNull(groups = {Create.class}) private EntityType entityType;

  @FormDataParam("entityId") @NotEmpty(groups = {Create.class}) private String entityId;

  @FormDataParam("parentConfigFileId") private String parentConfigFileId;

  @FormDataParam("relativeFilePath") private String relativeFilePath;

  private String overridePath;

  @Transient private List<String> versions;

  @Transient private ConfigFile overriddenConfigFile;

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
  public String getRelativeFilePath() {
    return relativeFilePath;
  }

  /**
   * Sets relative path.
   *
   * @param relativeFilePath the relative path
   */
  public void setRelativeFilePath(String relativeFilePath) {
    this.relativeFilePath = relativeFilePath;
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

  /**
   * Gets override path.
   *
   * @return the override path
   */
  public String getOverridePath() {
    return overridePath;
  }

  /**
   * Sets override path.
   *
   * @param overridePath the override path
   */
  public void setOverridePath(String overridePath) {
    this.overridePath = overridePath;
  }

  /**
   * Gets env id.
   *
   * @return the env id
   */
  public String getEnvId() {
    return envId;
  }

  /**
   * Sets env id.
   *
   * @param envId the env id
   */
  public void setEnvId(String envId) {
    this.envId = envId;
  }

  /**
   * Gets overridden config file.
   *
   * @return the overridden config file
   */
  public ConfigFile getOverriddenConfigFile() {
    return overriddenConfigFile;
  }

  /**
   * Sets overridden config file.
   *
   * @param overriddenConfigFile the overridden config file
   */
  public void setOverriddenConfigFile(ConfigFile overriddenConfigFile) {
    this.overriddenConfigFile = overriddenConfigFile;
  }

  /**
   * Gets versions.
   *
   * @return the versions
   */
  public List<String> getVersions() {
    return versions;
  }

  /**
   * Sets versions.
   *
   * @param versions the versions
   */
  public void setVersions(List<String> versions) {
    this.versions = versions;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(templateId, envId, entityType, entityId, parentConfigFileId, relativeFilePath, overridePath,
              versions, overriddenConfigFile);
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
    return Objects.equals(this.templateId, other.templateId) && Objects.equals(this.envId, other.envId)
        && Objects.equals(this.entityType, other.entityType) && Objects.equals(this.entityId, other.entityId)
        && Objects.equals(this.parentConfigFileId, other.parentConfigFileId)
        && Objects.equals(this.relativeFilePath, other.relativeFilePath)
        && Objects.equals(this.overridePath, other.overridePath) && Objects.equals(this.versions, other.versions)
        && Objects.equals(this.overriddenConfigFile, other.overriddenConfigFile);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("templateId", templateId)
        .add("envId", envId)
        .add("entityType", entityType)
        .add("entityId", entityId)
        .add("parentConfigFileId", parentConfigFileId)
        .add("relativeFilePath", relativeFilePath)
        .add("overridePath", overridePath)
        .add("versions", versions)
        .add("overriddenConfigFile", overriddenConfigFile)
        .toString();
  }

  /**
   * Gets parent config file id.
   *
   * @return the parent config file id
   */
  public String getParentConfigFileId() {
    return parentConfigFileId;
  }

  /**
   * Sets parent config file id.
   *
   * @param parentConfigFileId the parent config file id
   */
  public void setParentConfigFileId(String parentConfigFileId) {
    this.parentConfigFileId = parentConfigFileId;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String templateId;
    private String envId;
    private EntityType entityType;
    private String entityId;
    private String parentConfigFileId;
    private String relativeFilePath;
    private String overridePath;
    private List<String> versions;
    private ConfigFile overriddenConfigFile;
    private String name;
    private String fileUuid;
    private String fileName;
    private String mimeType;
    private long size;
    private ChecksumType checksumType = ChecksumType.MD5;
    private String checksum;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    /**
     * A config file builder.
     *
     * @return the builder
     */
    public static Builder aConfigFile() {
      return new Builder();
    }

    /**
     * With template id builder.
     *
     * @param templateId the template id
     * @return the builder
     */
    public Builder withTemplateId(String templateId) {
      this.templateId = templateId;
      return this;
    }

    /**
     * With env id builder.
     *
     * @param envId the env id
     * @return the builder
     */
    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    /**
     * With entity type builder.
     *
     * @param entityType the entity type
     * @return the builder
     */
    public Builder withEntityType(EntityType entityType) {
      this.entityType = entityType;
      return this;
    }

    /**
     * With entity id builder.
     *
     * @param entityId the entity id
     * @return the builder
     */
    public Builder withEntityId(String entityId) {
      this.entityId = entityId;
      return this;
    }

    /**
     * With parent config file id builder.
     *
     * @param parentConfigFileId the parent config file id
     * @return the builder
     */
    public Builder withParentConfigFileId(String parentConfigFileId) {
      this.parentConfigFileId = parentConfigFileId;
      return this;
    }

    /**
     * With relative file path builder.
     *
     * @param relativeFilePath the relative file path
     * @return the builder
     */
    public Builder withRelativeFilePath(String relativeFilePath) {
      this.relativeFilePath = relativeFilePath;
      return this;
    }

    /**
     * With override path builder.
     *
     * @param overridePath the override path
     * @return the builder
     */
    public Builder withOverridePath(String overridePath) {
      this.overridePath = overridePath;
      return this;
    }

    /**
     * With versions builder.
     *
     * @param versions the versions
     * @return the builder
     */
    public Builder withVersions(List<String> versions) {
      this.versions = versions;
      return this;
    }

    /**
     * With overridden config file builder.
     *
     * @param overriddenConfigFile the overridden config file
     * @return the builder
     */
    public Builder withOverriddenConfigFile(ConfigFile overriddenConfigFile) {
      this.overriddenConfigFile = overriddenConfigFile;
      return this;
    }

    /**
     * With name builder.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With file uuid builder.
     *
     * @param fileUuid the file uuid
     * @return the builder
     */
    public Builder withFileUuid(String fileUuid) {
      this.fileUuid = fileUuid;
      return this;
    }

    /**
     * With file name builder.
     *
     * @param fileName the file name
     * @return the builder
     */
    public Builder withFileName(String fileName) {
      this.fileName = fileName;
      return this;
    }

    /**
     * With mime type builder.
     *
     * @param mimeType the mime type
     * @return the builder
     */
    public Builder withMimeType(String mimeType) {
      this.mimeType = mimeType;
      return this;
    }

    /**
     * With size builder.
     *
     * @param size the size
     * @return the builder
     */
    public Builder withSize(long size) {
      this.size = size;
      return this;
    }

    /**
     * With checksum type builder.
     *
     * @param checksumType the checksum type
     * @return the builder
     */
    public Builder withChecksumType(ChecksumType checksumType) {
      this.checksumType = checksumType;
      return this;
    }

    /**
     * With checksum builder.
     *
     * @param checksum the checksum
     * @return the builder
     */
    public Builder withChecksum(String checksum) {
      this.checksum = checksum;
      return this;
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With active builder.
     *
     * @param active the active
     * @return the builder
     */
    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aConfigFile()
          .withTemplateId(templateId)
          .withEnvId(envId)
          .withEntityType(entityType)
          .withEntityId(entityId)
          .withParentConfigFileId(parentConfigFileId)
          .withRelativeFilePath(relativeFilePath)
          .withOverridePath(overridePath)
          .withVersions(versions)
          .withOverriddenConfigFile(overriddenConfigFile)
          .withName(name)
          .withFileUuid(fileUuid)
          .withFileName(fileName)
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
      configFile.setEnvId(envId);
      configFile.setEntityType(entityType);
      configFile.setEntityId(entityId);
      configFile.setParentConfigFileId(parentConfigFileId);
      configFile.setRelativeFilePath(relativeFilePath);
      configFile.setOverridePath(overridePath);
      configFile.setVersions(versions);
      configFile.setOverriddenConfigFile(overriddenConfigFile);
      configFile.setName(name);
      configFile.setFileUuid(fileUuid);
      configFile.setFileName(fileName);
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

package software.wings.beans;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

import java.util.Objects;
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
  public static final String DEFAULT_TEMPLATE_ID = "__TEMPLATE_ID";

  @FormDataParam("templateId") @DefaultValue(DEFAULT_TEMPLATE_ID) private String templateId;
  @FormDataParam("entityId") private String entityId;
  @FormDataParam("relativePath") private String relativePath;

  public String getEntityId() {
    return entityId;
  }

  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  public String getRelativePath() {
    return relativePath;
  }

  public void setRelativePath(String relativePath) {
    this.relativePath = relativePath;
  }

  public String getTemplateId() {
    return templateId;
  }

  public void setTemplateId(String templateId) {
    this.templateId = templateId;
  }

  /* (non-Javadoc)
   * @see software.wings.beans.BaseFile#hashCode()
   */
  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(templateId, entityId, relativePath);
  }

  /* (non-Javadoc)
   * @see software.wings.beans.BaseFile#equals(java.lang.Object)
   */
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
    return Objects.equals(this.templateId, other.templateId) && Objects.equals(this.entityId, other.entityId)
        && Objects.equals(this.relativePath, other.relativePath);
  }

  /**
   * The Class ConfigFileBuilder.
   */
  public static final class ConfigFileBuilder {
    private String templateId;
    private String entityId;
    private String relativePath;
    private String fileUuid;
    private String name;
    private String mimeType;
    private long size;
    private ChecksumType checksumType;
    private String checksum;
    private String uuid;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private ConfigFileBuilder() {}

    /**
     * A config file.
     *
     * @return the config file builder
     */
    public static ConfigFileBuilder aConfigFile() {
      return new ConfigFileBuilder();
    }

    /**
     * With template id.
     *
     * @param templateId the template id
     * @return the config file builder
     */
    public ConfigFileBuilder withTemplateId(String templateId) {
      this.templateId = templateId;
      return this;
    }

    /**
     * With entity id.
     *
     * @param entityId the entity id
     * @return the config file builder
     */
    public ConfigFileBuilder withEntityId(String entityId) {
      this.entityId = entityId;
      return this;
    }

    /**
     * With relative path.
     *
     * @param relativePath the relative path
     * @return the config file builder
     */
    public ConfigFileBuilder withRelativePath(String relativePath) {
      this.relativePath = relativePath;
      return this;
    }

    /**
     * With file uuid.
     *
     * @param fileUuid the file uuid
     * @return the config file builder
     */
    public ConfigFileBuilder withFileUuid(String fileUuid) {
      this.fileUuid = fileUuid;
      return this;
    }

    /**
     * With name.
     *
     * @param name the name
     * @return the config file builder
     */
    public ConfigFileBuilder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With mime type.
     *
     * @param mimeType the mime type
     * @return the config file builder
     */
    public ConfigFileBuilder withMimeType(String mimeType) {
      this.mimeType = mimeType;
      return this;
    }

    /**
     * With size.
     *
     * @param size the size
     * @return the config file builder
     */
    public ConfigFileBuilder withSize(long size) {
      this.size = size;
      return this;
    }

    /**
     * With checksum type.
     *
     * @param checksumType the checksum type
     * @return the config file builder
     */
    public ConfigFileBuilder withChecksumType(ChecksumType checksumType) {
      this.checksumType = checksumType;
      return this;
    }

    /**
     * With checksum.
     *
     * @param checksum the checksum
     * @return the config file builder
     */
    public ConfigFileBuilder withChecksum(String checksum) {
      this.checksum = checksum;
      return this;
    }

    /**
     * With uuid.
     *
     * @param uuid the uuid
     * @return the config file builder
     */
    public ConfigFileBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With created by.
     *
     * @param createdBy the created by
     * @return the config file builder
     */
    public ConfigFileBuilder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at.
     *
     * @param createdAt the created at
     * @return the config file builder
     */
    public ConfigFileBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by.
     *
     * @param lastUpdatedBy the last updated by
     * @return the config file builder
     */
    public ConfigFileBuilder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at.
     *
     * @param lastUpdatedAt the last updated at
     * @return the config file builder
     */
    public ConfigFileBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With active.
     *
     * @param active the active
     * @return the config file builder
     */
    public ConfigFileBuilder withActive(boolean active) {
      this.active = active;
      return this;
    }

    /**
     * But.
     *
     * @return the config file builder
     */
    public ConfigFileBuilder but() {
      return aConfigFile()
          .withTemplateId(templateId)
          .withEntityId(entityId)
          .withRelativePath(relativePath)
          .withFileUuid(fileUuid)
          .withName(name)
          .withMimeType(mimeType)
          .withSize(size)
          .withChecksumType(checksumType)
          .withChecksum(checksum)
          .withUuid(uuid)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    /**
     * Builds the.
     *
     * @return the config file
     */
    public ConfigFile build() {
      ConfigFile configFile = new ConfigFile();
      configFile.setTemplateId(templateId);
      configFile.setEntityId(entityId);
      configFile.setRelativePath(relativePath);
      configFile.setFileUuid(fileUuid);
      configFile.setName(name);
      configFile.setMimeType(mimeType);
      configFile.setSize(size);
      configFile.setChecksumType(checksumType);
      configFile.setChecksum(checksum);
      configFile.setUuid(uuid);
      configFile.setCreatedBy(createdBy);
      configFile.setCreatedAt(createdAt);
      configFile.setLastUpdatedBy(lastUpdatedBy);
      configFile.setLastUpdatedAt(lastUpdatedAt);
      configFile.setActive(active);
      return configFile;
    }
  }
}

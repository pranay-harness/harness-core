package software.wings.beans;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

import java.util.Objects;
import javax.ws.rs.DefaultValue;

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

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(templateId, entityId, relativePath);
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
    return Objects.equals(this.templateId, other.templateId) && Objects.equals(this.entityId, other.entityId)
        && Objects.equals(this.relativePath, other.relativePath);
  }

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

    public static ConfigFileBuilder aConfigFile() {
      return new ConfigFileBuilder();
    }

    public ConfigFileBuilder withTemplateId(String templateId) {
      this.templateId = templateId;
      return this;
    }

    public ConfigFileBuilder withEntityId(String entityId) {
      this.entityId = entityId;
      return this;
    }

    public ConfigFileBuilder withRelativePath(String relativePath) {
      this.relativePath = relativePath;
      return this;
    }

    public ConfigFileBuilder withFileUuid(String fileUuid) {
      this.fileUuid = fileUuid;
      return this;
    }

    public ConfigFileBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public ConfigFileBuilder withMimeType(String mimeType) {
      this.mimeType = mimeType;
      return this;
    }

    public ConfigFileBuilder withSize(long size) {
      this.size = size;
      return this;
    }

    public ConfigFileBuilder withChecksumType(ChecksumType checksumType) {
      this.checksumType = checksumType;
      return this;
    }

    public ConfigFileBuilder withChecksum(String checksum) {
      this.checksum = checksum;
      return this;
    }

    public ConfigFileBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public ConfigFileBuilder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public ConfigFileBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public ConfigFileBuilder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public ConfigFileBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public ConfigFileBuilder withActive(boolean active) {
      this.active = active;
      return this;
    }

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

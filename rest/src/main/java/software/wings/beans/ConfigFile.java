package software.wings.beans;

import static software.wings.beans.ConfigFile.Builder.aConfigFile;
import static software.wings.beans.EntityVersion.Builder.anEntityVersion;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.reinert.jjschema.SchemaIgnore;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.security.encryption.Encryptable;
import software.wings.utils.validation.Create;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;

/**
 * Created by anubhaw on 4/12/16.
 */
@Entity(value = "configFiles", noClassnameStored = true)
@Indexes(@Index(fields =
    {
      @Field("entityId")
      , @Field("templateId"), @Field("relativeFilePath"), @Field("configOverrideType"), @Field("instances"),
          @Field("configOverrideExpression")
    },
    options = @IndexOptions(
        unique = true, name = "entityId_1_templateId_1_relativeFilePath_1_OType_1_instances_1_OExpression_1")))
public class ConfigFile extends BaseFile implements Encryptable {
  /**
   * The constant DEFAULT_TEMPLATE_ID.
   */
  public static final String DEFAULT_TEMPLATE_ID = "__TEMPLATE_ID";

  @NotEmpty private String accountId;

  @FormDataParam("templateId") @DefaultValue(DEFAULT_TEMPLATE_ID) private String templateId;

  @FormDataParam("envId") @NotEmpty(groups = {Create.class}) private String envId;

  @FormDataParam("entityType") @NotNull(groups = {Create.class}) private EntityType entityType;

  @FormDataParam("entityId") @NotEmpty(groups = {Create.class}) private String entityId;

  @FormDataParam("description") private String description;

  @FormDataParam("parentConfigFileId") private String parentConfigFileId;

  @FormDataParam("relativeFilePath") private String relativeFilePath;

  @FormDataParam("targetToAllEnv") private boolean targetToAllEnv;

  @FormDataParam("defaultVersion") private int defaultVersion;

  private Map<String, EntityVersion> envIdVersionMap = Maps.newHashMap();

  @JsonIgnore @FormDataParam("envIdVersionMapString") private String envIdVersionMapString;

  @Transient @FormDataParam("setAsDefault") private boolean setAsDefault;

  @Transient @FormDataParam("notes") private String notes;

  private String overridePath;

  @NotNull(groups = {Create.class}) @FormDataParam("configOverrideType") private ConfigOverrideType configOverrideType;
  @FormDataParam("configOverrideExpression") private String configOverrideExpression;

  @FormDataParam("instances") private List<String> instances;

  @Transient private ConfigFile overriddenConfigFile;

  @FormDataParam("encrypted") private boolean encrypted = false;

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
   * Gets description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets description.
   *
   * @param description the description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Getter for property 'targetToAllEnv'.
   *
   * @return Value for property 'targetToAllEnv'.
   */
  public boolean isTargetToAllEnv() {
    return targetToAllEnv;
  }

  /**
   * Setter for property 'targetToAllEnv'.
   *
   * @param targetToAllEnv Value to set for property 'targetToAllEnv'.
   */
  public void setTargetToAllEnv(boolean targetToAllEnv) {
    this.targetToAllEnv = targetToAllEnv;
  }

  /**
   * Getter for property 'defaultVersion'.
   *
   * @return Value for property 'defaultVersion'.
   */
  public int getDefaultVersion() {
    return defaultVersion;
  }

  /**
   * Setter for property 'defaultVersion'.
   *
   * @param defaultVersion Value to set for property 'defaultVersion'.
   */
  public void setDefaultVersion(int defaultVersion) {
    this.defaultVersion = defaultVersion;
  }

  /**
   * Getter for property 'envIdVersionMap'.
   *
   * @return Value for property 'envIdVersionMap'.
   */
  public Map<String, EntityVersion> getEnvIdVersionMap() {
    return envIdVersionMap;
  }

  /**
   * Setter for property 'envIdVersionMap'.
   *
   * @param envIdVersionMap Value to set for property 'envIdVersionMap'.
   */
  public void setEnvIdVersionMap(Map<String, EntityVersion> envIdVersionMap) {
    this.envIdVersionMap = envIdVersionMap;
  }

  /**
   * Gets config override type.
   *
   * @return the config override type
   */
  public ConfigOverrideType getConfigOverrideType() {
    return configOverrideType;
  }

  /**
   * Sets config override type.
   *
   * @param configOverrideType the config override type
   */
  public void setConfigOverrideType(ConfigOverrideType configOverrideType) {
    this.configOverrideType = configOverrideType;
  }

  /**
   * Gets config override expression.
   *
   * @return the config override expression
   */
  public String getConfigOverrideExpression() {
    return configOverrideExpression;
  }

  /**
   * Sets config override expression.
   *
   * @param configOverrideExpression the config override expression
   */
  public void setConfigOverrideExpression(String configOverrideExpression) {
    this.configOverrideExpression = configOverrideExpression;
  }

  /**
   * Gets version for env.
   *
   * @param envId the env id
   * @return the version for env
   */
  @JsonIgnore
  public int getVersionForEnv(String envId) {
    return Optional.ofNullable(envIdVersionMap.get(envId))
        .orElse(anEntityVersion().withVersion(defaultVersion).build())
        .getVersion();
  }

  /**
   * Is set as default boolean.
   *
   * @return the boolean
   */
  @JsonIgnore
  public boolean isSetAsDefault() {
    return setAsDefault;
  }

  /**
   * Sets set as default.
   *
   * @param setAsDefault the set as default
   */
  @JsonProperty
  public void setSetAsDefault(boolean setAsDefault) {
    this.setAsDefault = setAsDefault;
  }

  /**
   * Gets notes.
   *
   * @return the notes
   */
  public String getNotes() {
    return notes;
  }

  /**
   * Sets notes.
   *
   * @param notes the notes
   */
  public void setNotes(String notes) {
    this.notes = notes;
  }

  /**
   * Getter for property 'envIdVersionMapString'.
   *
   * @return Value for property 'envIdVersionMapString'.
   */
  public String getEnvIdVersionMapString() {
    return envIdVersionMapString;
  }

  /**
   * Setter for property 'envIdVersionMapString'.
   *
   * @param envIdVersionMapString Value to set for property 'envIdVersionMapString'.
   */
  public void setEnvIdVersionMapString(String envIdVersionMapString) {
    this.envIdVersionMapString = envIdVersionMapString;
  }

  /**
   * Getter for property 'instances'.
   *
   * @return Value for property 'instances'.
   */
  public List<String> getInstances() {
    return instances;
  }

  /**
   * Setter for property 'instances'.
   *
   * @param instances Value to set for property 'instances'.
   */
  public void setInstances(List<String> instances) {
    this.instances = instances;
  }

  /**
   * Is encrypted boolean.
   *
   * @return the boolean
   */
  public boolean isEncrypted() {
    return encrypted;
  }

  /**
   * Sets encrypted.
   *
   * @param encrypted the encrypted
   */
  public void setEncrypted(boolean encrypted) {
    this.encrypted = encrypted;
  }

  @Override
  @SchemaIgnore
  public String getAccountId() {
    return accountId;
  }

  @Override
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("accountId", accountId)
        .add("templateId", templateId)
        .add("envId", envId)
        .add("entityType", entityType)
        .add("entityId", entityId)
        .add("description", description)
        .add("parentConfigFileId", parentConfigFileId)
        .add("relativeFilePath", relativeFilePath)
        .add("targetToAllEnv", targetToAllEnv)
        .add("defaultVersion", defaultVersion)
        .add("envIdVersionMap", envIdVersionMap)
        .add("envIdVersionMapString", envIdVersionMapString)
        .add("setAsDefault", setAsDefault)
        .add("notes", notes)
        .add("overridePath", overridePath)
        .add("configOverrideType", configOverrideType)
        .add("configOverrideExpression", configOverrideExpression)
        .add("instances", instances)
        .add("overriddenConfigFile", overriddenConfigFile)
        .add("encrypted", encrypted)
        .toString();
  }

  /**
   * The enum Config override type.
   */
  public enum ConfigOverrideType {
    /**
     * All config override type.
     */
    ALL, /**
          * Instances config override type.
          */
    INSTANCES, /**
                * Custom config override type.
                */
    CUSTOM
  }

  public ConfigFile clone() {
    return aConfigFile()
        .withAppId(getAppId())
        .withEnvId(getEnvId())
        .withEntityType(getEntityType())
        .withEntityId(getUuid())
        .withTemplateId(getTemplateId())
        .withFileName(getFileName())
        .withRelativeFilePath(getRelativeFilePath())
        .withTargetToAllEnv(isTargetToAllEnv())
        .withEncrypted(isEncrypted())
        .build();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    /**
     * The constant DEFAULT_TEMPLATE_ID.
     */
    private String name;
    private String fileUuid;
    private String fileName;
    private String mimeType;
    private long size;
    private ChecksumType checksumType = ChecksumType.MD5;
    private String checksum;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private String accountId;
    private String templateId;
    private String envId;
    private EntityType entityType;
    private String entityId;
    private String description;
    private String parentConfigFileId;
    private String relativeFilePath;
    private boolean targetToAllEnv;
    private int defaultVersion;
    private Map<String, EntityVersion> envIdVersionMap = Maps.newHashMap();
    private String envIdVersionMapString;
    private boolean setAsDefault;
    private String notes;
    private String overridePath;
    private ConfigOverrideType configOverrideType;
    private String configOverrideExpression;
    private List<String> instances;
    private ConfigFile overriddenConfigFile;
    private boolean encrypted = false;

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
    public Builder withCreatedBy(EmbeddedUser createdBy) {
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
    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
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
     * With account id builder.
     *
     * @param accountId the account id
     * @return the builder
     */
    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
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
     * With description builder.
     *
     * @param description the description
     * @return the builder
     */
    public Builder withDescription(String description) {
      this.description = description;
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
     * With target to all env builder.
     *
     * @param targetToAllEnv the target to all env
     * @return the builder
     */
    public Builder withTargetToAllEnv(boolean targetToAllEnv) {
      this.targetToAllEnv = targetToAllEnv;
      return this;
    }

    /**
     * With default version builder.
     *
     * @param defaultVersion the default version
     * @return the builder
     */
    public Builder withDefaultVersion(int defaultVersion) {
      this.defaultVersion = defaultVersion;
      return this;
    }

    /**
     * With env id version map builder.
     *
     * @param envIdVersionMap the env id version map
     * @return the builder
     */
    public Builder withEnvIdVersionMap(Map<String, EntityVersion> envIdVersionMap) {
      this.envIdVersionMap = envIdVersionMap;
      return this;
    }

    /**
     * With env id version map string builder.
     *
     * @param envIdVersionMapString the env id version map string
     * @return the builder
     */
    public Builder withEnvIdVersionMapString(String envIdVersionMapString) {
      this.envIdVersionMapString = envIdVersionMapString;
      return this;
    }

    /**
     * With set as default builder.
     *
     * @param setAsDefault the set as default
     * @return the builder
     */
    public Builder withSetAsDefault(boolean setAsDefault) {
      this.setAsDefault = setAsDefault;
      return this;
    }

    /**
     * With notes builder.
     *
     * @param notes the notes
     * @return the builder
     */
    public Builder withNotes(String notes) {
      this.notes = notes;
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
     * With config override type builder.
     *
     * @param configOverrideType the config override type
     * @return the builder
     */
    public Builder withConfigOverrideType(ConfigOverrideType configOverrideType) {
      this.configOverrideType = configOverrideType;
      return this;
    }

    /**
     * With config override expression builder.
     *
     * @param configOverrideExpression the config override expression
     * @return the builder
     */
    public Builder withConfigOverrideExpression(String configOverrideExpression) {
      this.configOverrideExpression = configOverrideExpression;
      return this;
    }

    /**
     * With instances builder.
     *
     * @param instances the instances
     * @return the builder
     */
    public Builder withInstances(List<String> instances) {
      this.instances = instances;
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
     * With encrypted builder.
     *
     * @param encrypted the encrypted
     * @return the builder
     */
    public Builder withEncrypted(boolean encrypted) {
      this.encrypted = encrypted;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aConfigFile()
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
          .withAccountId(accountId)
          .withTemplateId(templateId)
          .withEnvId(envId)
          .withEntityType(entityType)
          .withEntityId(entityId)
          .withDescription(description)
          .withParentConfigFileId(parentConfigFileId)
          .withRelativeFilePath(relativeFilePath)
          .withTargetToAllEnv(targetToAllEnv)
          .withDefaultVersion(defaultVersion)
          .withEnvIdVersionMap(envIdVersionMap)
          .withEnvIdVersionMapString(envIdVersionMapString)
          .withSetAsDefault(setAsDefault)
          .withNotes(notes)
          .withOverridePath(overridePath)
          .withConfigOverrideType(configOverrideType)
          .withConfigOverrideExpression(configOverrideExpression)
          .withInstances(instances)
          .withOverriddenConfigFile(overriddenConfigFile)
          .withEncrypted(encrypted);
    }

    /**
     * Build config file.
     *
     * @return the config file
     */
    public ConfigFile build() {
      ConfigFile configFile = new ConfigFile();
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
      configFile.setAccountId(accountId);
      configFile.setTemplateId(templateId);
      configFile.setEnvId(envId);
      configFile.setEntityType(entityType);
      configFile.setEntityId(entityId);
      configFile.setDescription(description);
      configFile.setParentConfigFileId(parentConfigFileId);
      configFile.setRelativeFilePath(relativeFilePath);
      configFile.setTargetToAllEnv(targetToAllEnv);
      configFile.setDefaultVersion(defaultVersion);
      configFile.setEnvIdVersionMap(envIdVersionMap);
      configFile.setEnvIdVersionMapString(envIdVersionMapString);
      configFile.setSetAsDefault(setAsDefault);
      configFile.setNotes(notes);
      configFile.setOverridePath(overridePath);
      configFile.setConfigOverrideType(configOverrideType);
      configFile.setConfigOverrideExpression(configOverrideExpression);
      configFile.setInstances(instances);
      configFile.setOverriddenConfigFile(overriddenConfigFile);
      configFile.setEncrypted(encrypted);
      return configFile;
    }
  }
}

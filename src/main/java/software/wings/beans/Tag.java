package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Transient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by anubhaw on 3/30/16.
 */
@Entity(value = "tags", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("envId")
                           , @Field("name") }, options = @IndexOptions(unique = true)))
public class Tag extends Base {
  @NotEmpty private String name;
  private String description;
  private String autoTaggingRule;
  private TagType tagType = TagType.TAGGED_HOST;
  private String rootTagId;
  private String parentTagId;
  @NotEmpty private String envId;
  @Reference(idOnly = true, ignoreMissing = true) private List<Tag> children = new ArrayList<>();
  @Transient private List<ConfigFile> configFiles = new ArrayList<>();

  /**
   * The enum Tag type.
   */
  public enum TagType {
    /**
     * Environment tag type.
     */
    ENVIRONMENT(false), // created by default. represent Environment level overrides
    /**
     * Untagged host tag type.
     */
    UNTAGGED_HOST(false), // created by default. represents all untagged hosts
    /**
     * The Tagged host.
     */
    TAGGED_HOST(true); // user created. represents tagged hosts by user

    private boolean modificationAllowed;

    TagType(boolean modificationAllowed) {
      this.modificationAllowed = modificationAllowed;
    }

    /**
     * Is modification allowed boolean.
     *
     * @return the boolean
     */
    public boolean isModificationAllowed() {
      return modificationAllowed;
    }
  }

  /**
   * Gets name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
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
   * Gets auto tagging rule.
   *
   * @return the auto tagging rule
   */
  public String getAutoTaggingRule() {
    return autoTaggingRule;
  }

  /**
   * Sets auto tagging rule.
   *
   * @param autoTaggingRule the auto tagging rule
   */
  public void setAutoTaggingRule(String autoTaggingRule) {
    this.autoTaggingRule = autoTaggingRule;
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
   * Gets children.
   *
   * @return the children
   */
  public List<Tag> getChildren() {
    return children;
  }

  /**
   * Sets children.
   *
   * @param children the children
   */
  public void setChildren(List<Tag> children) {
    this.children = children;
  }

  /**
   * Gets config files.
   *
   * @return the config files
   */
  public List<ConfigFile> getConfigFiles() {
    return configFiles;
  }

  /**
   * Sets config files.
   *
   * @param configFiles the config files
   */
  public void setConfigFiles(List<ConfigFile> configFiles) {
    this.configFiles = configFiles;
  }

  /**
   * Gets root tag id.
   *
   * @return the root tag id
   */
  public String getRootTagId() {
    return rootTagId;
  }

  /**
   * Sets root tag id.
   *
   * @param rootTagId the root tag id
   */
  public void setRootTagId(String rootTagId) {
    this.rootTagId = rootTagId;
  }

  /**
   * Gets parent tag.
   *
   * @return the parent tag
   */
  public String getParentTagId() {
    return parentTagId;
  }

  /**
   * Sets parent tag.
   *
   * @param parentTagId the parent tag
   */
  public void setParentTagId(String parentTagId) {
    this.parentTagId = parentTagId;
  }

  /**
   * Gets tag type.
   *
   * @return the tag type
   */
  public TagType getTagType() {
    return tagType;
  }

  /**
   * Sets tag type.
   *
   * @param tagType the tag type
   */
  public void setTagType(TagType tagType) {
    this.tagType = tagType;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(
              name, description, autoTaggingRule, tagType, rootTagId, parentTagId, envId, children, configFiles);
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
    final Tag other = (Tag) obj;
    return Objects.equals(this.name, other.name) && Objects.equals(this.description, other.description)
        && Objects.equals(this.autoTaggingRule, other.autoTaggingRule) && Objects.equals(this.tagType, other.tagType)
        && Objects.equals(this.rootTagId, other.rootTagId) && Objects.equals(this.parentTagId, other.parentTagId)
        && Objects.equals(this.envId, other.envId) && Objects.equals(this.children, other.children)
        && Objects.equals(this.configFiles, other.configFiles);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("description", description)
        .add("autoTaggingRule", autoTaggingRule)
        .add("tagType", tagType)
        .add("rootTagId", rootTagId)
        .add("parentTagId", parentTagId)
        .add("envId", envId)
        .add("children", children)
        .add("configFiles", configFiles)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String name;
    private String description;
    private String autoTaggingRule;
    private TagType tagType = TagType.TAGGED_HOST;
    private String rootTagId;
    private String parentTagId;
    private String envId;
    private List<Tag> children = new ArrayList<>();
    private List<ConfigFile> configFiles = new ArrayList<>();
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    /**
     * A tag builder.
     *
     * @return the builder
     */
    public static Builder aTag() {
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
     * With auto tagging rule builder.
     *
     * @param autoTaggingRule the auto tagging rule
     * @return the builder
     */
    public Builder withAutoTaggingRule(String autoTaggingRule) {
      this.autoTaggingRule = autoTaggingRule;
      return this;
    }

    /**
     * With tag type builder.
     *
     * @param tagType the tag type
     * @return the builder
     */
    public Builder withTagType(TagType tagType) {
      this.tagType = tagType;
      return this;
    }

    /**
     * With root tag id builder.
     *
     * @param rootTagId the root tag id
     * @return the builder
     */
    public Builder withRootTagId(String rootTagId) {
      this.rootTagId = rootTagId;
      return this;
    }

    /**
     * With parent tag id builder.
     *
     * @param parentTagId the parent tag id
     * @return the builder
     */
    public Builder withParentTagId(String parentTagId) {
      this.parentTagId = parentTagId;
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
     * With children builder.
     *
     * @param children the children
     * @return the builder
     */
    public Builder withChildren(List<Tag> children) {
      this.children = children;
      return this;
    }

    /**
     * With config files builder.
     *
     * @param configFiles the config files
     * @return the builder
     */
    public Builder withConfigFiles(List<ConfigFile> configFiles) {
      this.configFiles = configFiles;
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
      return aTag()
          .withName(name)
          .withDescription(description)
          .withAutoTaggingRule(autoTaggingRule)
          .withTagType(tagType)
          .withRootTagId(rootTagId)
          .withParentTagId(parentTagId)
          .withEnvId(envId)
          .withChildren(children)
          .withConfigFiles(configFiles)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    /**
     * Build tag.
     *
     * @return the tag
     */
    public Tag build() {
      Tag tag = new Tag();
      tag.setName(name);
      tag.setDescription(description);
      tag.setAutoTaggingRule(autoTaggingRule);
      tag.setTagType(tagType);
      tag.setRootTagId(rootTagId);
      tag.setParentTagId(parentTagId);
      tag.setEnvId(envId);
      tag.setChildren(children);
      tag.setConfigFiles(configFiles);
      tag.setUuid(uuid);
      tag.setAppId(appId);
      tag.setCreatedBy(createdBy);
      tag.setCreatedAt(createdAt);
      tag.setLastUpdatedBy(lastUpdatedBy);
      tag.setLastUpdatedAt(lastUpdatedAt);
      tag.setActive(active);
      return tag;
    }
  }
}

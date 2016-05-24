package software.wings.beans;

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
  private String name;
  private String description;
  private String autoTaggingRule;
  private boolean rootTag = false;
  private String rootTagId;
  private String envId;
  @Reference(idOnly = true, ignoreMissing = true) private List<Tag> children;

  @Transient private List<ConfigFile> configFiles = new ArrayList<>();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getAutoTaggingRule() {
    return autoTaggingRule;
  }

  public void setAutoTaggingRule(String autoTaggingRule) {
    this.autoTaggingRule = autoTaggingRule;
  }

  public boolean isRootTag() {
    return rootTag;
  }

  public void setRootTag(boolean rootTag) {
    this.rootTag = rootTag;
  }

  public String getEnvId() {
    return envId;
  }

  public void setEnvId(String envId) {
    this.envId = envId;
  }

  public List<Tag> getChildren() {
    return children;
  }

  public void setChildren(List<Tag> children) {
    this.children = children;
  }

  public List<ConfigFile> getConfigFiles() {
    return configFiles;
  }

  public void setConfigFiles(List<ConfigFile> configFiles) {
    this.configFiles = configFiles;
  }

  public String getRootTagId() {
    return rootTagId;
  }

  public void setRootTagId(String rootTagId) {
    this.rootTagId = rootTagId;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(name, description, autoTaggingRule, rootTag, rootTagId, envId, children, configFiles);
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
        && Objects.equals(this.autoTaggingRule, other.autoTaggingRule) && Objects.equals(this.rootTag, other.rootTag)
        && Objects.equals(this.rootTagId, other.rootTagId) && Objects.equals(this.envId, other.envId)
        && Objects.equals(this.children, other.children) && Objects.equals(this.configFiles, other.configFiles);
  }

  public static final class TagBuilder {
    private String name;
    private String description;
    private String autoTaggingRule;
    private boolean rootTag = false;
    private String rootTagId;
    private String envId;
    private List<Tag> children;
    private List<ConfigFile> configFiles = new ArrayList<>();
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private TagBuilder() {}

    public static TagBuilder aTag() {
      return new TagBuilder();
    }

    public TagBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public TagBuilder withDescription(String description) {
      this.description = description;
      return this;
    }

    public TagBuilder withAutoTaggingRule(String autoTaggingRule) {
      this.autoTaggingRule = autoTaggingRule;
      return this;
    }

    public TagBuilder withRootTag(boolean rootTag) {
      this.rootTag = rootTag;
      return this;
    }

    public TagBuilder withRootTagId(String rootTagId) {
      this.rootTagId = rootTagId;
      return this;
    }

    public TagBuilder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public TagBuilder withChildren(List<Tag> children) {
      this.children = children;
      return this;
    }

    public TagBuilder withConfigFiles(List<ConfigFile> configFiles) {
      this.configFiles = configFiles;
      return this;
    }

    public TagBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public TagBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public TagBuilder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public TagBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public TagBuilder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public TagBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public TagBuilder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public TagBuilder but() {
      return aTag()
          .withName(name)
          .withDescription(description)
          .withAutoTaggingRule(autoTaggingRule)
          .withRootTag(rootTag)
          .withRootTagId(rootTagId)
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

    public Tag build() {
      Tag tag = new Tag();
      tag.setName(name);
      tag.setDescription(description);
      tag.setAutoTaggingRule(autoTaggingRule);
      tag.setRootTag(rootTag);
      tag.setRootTagId(rootTagId);
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

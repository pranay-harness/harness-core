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
@Indexes(@Index(fields = { @Field("tagType")
                           , @Field("name") }, options = @IndexOptions(unique = true)))
public class Tag extends Base {
  private String name;
  private String description;
  private String autoTaggingRule;
  @Reference(idOnly = true, ignoreMissing = true) private TagType tagType;
  @Reference(idOnly = true, ignoreMissing = true) private List<Tag> linkedTags;

  @Transient private List<ConfigFile> configFiles = new ArrayList<>();

  public Tag() {}

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

  public String getTagString() {
    return tagType.getName() + ":" + name;
  }

  public TagType getTagType() {
    return tagType;
  }

  public void setTagType(TagType tagType) {
    this.tagType = tagType;
  }

  public List<Tag> getLinkedTags() {
    return linkedTags;
  }

  public void setLinkedTags(List<Tag> linkedTags) {
    this.linkedTags = linkedTags;
  }

  public List<ConfigFile> getConfigFiles() {
    return configFiles;
  }

  public void setConfigFiles(List<ConfigFile> configFiles) {
    this.configFiles = configFiles;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(name, description, autoTaggingRule, tagType, linkedTags, configFiles);
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
        && Objects.equals(this.linkedTags, other.linkedTags) && Objects.equals(this.configFiles, other.configFiles);
  }

  public static final class TagBuilder {
    private String name;
    private String description;
    private String autoTaggingRule;
    private TagType tagType;
    private List<Tag> linkedTags;
    private List<ConfigFile> configFiles;
    private String uuid;
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

    public TagBuilder withTagType(TagType tagType) {
      this.tagType = tagType;
      return this;
    }

    public TagBuilder withLinkedTags(List<Tag> linkedTags) {
      this.linkedTags = linkedTags;
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
          .withTagType(tagType)
          .withLinkedTags(linkedTags)
          .withConfigFiles(configFiles)
          .withUuid(uuid)
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
      tag.setTagType(tagType);
      tag.setLinkedTags(linkedTags);
      tag.setConfigFiles(configFiles);
      tag.setUuid(uuid);
      tag.setCreatedBy(createdBy);
      tag.setCreatedAt(createdAt);
      tag.setLastUpdatedBy(lastUpdatedBy);
      tag.setLastUpdatedAt(lastUpdatedAt);
      tag.setActive(active);
      return tag;
    }
  }
}

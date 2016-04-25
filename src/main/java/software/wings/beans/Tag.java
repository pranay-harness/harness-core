package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Reference;

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
  @Reference private TagType tagType;

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
}

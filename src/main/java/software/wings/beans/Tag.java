package software.wings.beans;

import org.mongodb.morphia.annotations.*;

/**
 * Created by anubhaw on 3/30/16.
 */

@Entity(value = "tags", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("tagType")
                           , @Field("tag") }, options = @IndexOptions(unique = true)))
public class Tag extends Base {
  private String tagType;
  private String tag;
  private String description;

  public Tag() {}

  public Tag(String tagType, String tag, String description) {
    this.tagType = tagType;
    this.tag = tag;
    this.description = description;
  }

  public String getTagType() {
    return tagType;
  }

  public void setTagType(String tagType) {
    this.tagType = tagType;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}

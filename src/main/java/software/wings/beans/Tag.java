package software.wings.beans;

import org.mongodb.morphia.annotations.*;

/**
 * Created by anubhaw on 3/30/16.
 */

@Entity(value = "tags", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("type")
                           , @Field("name"), @Field("envID") }, options = @IndexOptions(unique = true)))
public class Tag extends Base {
  private String type;
  private String name;
  private String description;
  private String autoTaggingRule;
  private String envID;

  public Tag() {}

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

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

  public String getEnvID() {
    return envID;
  }

  public void setEnvID(String envID) {
    this.envID = envID;
  }

  public String getTagString() {
    return type + ":" + name;
  }
}

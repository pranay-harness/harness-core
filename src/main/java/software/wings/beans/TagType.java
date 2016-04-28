package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

/**
 * Created by anubhaw on 4/25/16.
 */

@Entity(value = "tagTypes", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("name")
                           , @Field("envId") }, options = @IndexOptions(unique = true)))
public class TagType extends Base {
  public static final String HierarchyTagName = "hierarchy";
  public String name;
  private String envId;

  public TagType() {}

  public TagType(String name, String envId) {
    this.name = name;
    this.envId = envId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEnvId() {
    return envId;
  }

  public void setEnvId(String envId) {
    this.envId = envId;
  }
}

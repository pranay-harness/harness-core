package software.wings.beans;

import org.mongodb.morphia.annotations.*;

/**
 * Created by anubhaw on 4/12/16.
 */

@Entity(value = "configFiles", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("serviceID")
                           , @Field("name") }, options = @IndexOptions(unique = true)))
public class ConfigFile extends BaseFile {
  private String serviceID;
  private String relativePath;

  public ConfigFile() {}

  public ConfigFile(String serviceID, String fileName, String relativePath, String md5) {
    super(fileName, md5);
    this.serviceID = serviceID;
    this.relativePath = relativePath;
  }

  public String getServiceID() {
    return serviceID;
  }

  public void setServiceID(String serviceID) {
    this.serviceID = serviceID;
  }

  public String getRelativePath() {
    return relativePath;
  }

  public void setRelativePath(String relativePath) {
    this.relativePath = relativePath;
  }
}

package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;

/**
 * Created by anubhaw on 4/12/16.
 */

@Entity(value = "configFiles", noClassnameStored = true)
public class ConfigFile extends BaseFile {
  private String serviceID;
  private String relativePath;

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

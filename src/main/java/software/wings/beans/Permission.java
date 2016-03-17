package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;

/**
 * Created by anubhaw on 3/17/16.
 */

@Entity(value = "permissions", noClassnameStored = true)
public class Permission extends Base {
  private String action;
  private String accessType;
  private String envID;
  private String serviceID;

  public Permission() {}

  public Permission(String action, String accessType, String envID, String serviceID) {
    this.action = action;
    this.accessType = accessType;
    this.envID = envID;
    this.serviceID = serviceID;
  }

  public String getAction() {
    return action;
  }
  public void setAction(String action) {
    this.action = action.toUpperCase();
  }
  public String getAccessType() {
    return accessType;
  }
  public void setAccessType(String accessType) {
    this.accessType = accessType.toUpperCase();
  }
  public String getEnvID() {
    return envID;
  }
  public void setEnvID(String envID) {
    this.envID = envID;
  }
  public String getServiceID() {
    return serviceID;
  }
  public void setServiceID(String serviceID) {
    this.serviceID = serviceID;
  }
}

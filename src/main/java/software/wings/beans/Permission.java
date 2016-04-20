package software.wings.beans;

import org.mongodb.morphia.annotations.Embedded;

/**
 * Created by anubhaw on 3/17/16.
 */
@Embedded
public class Permission {
  private String resource;
  private String action;
  private String envID;
  private String serviceID;

  public Permission() {}

  public Permission(String resource, String action, String envID, String serviceID) {
    this.resource = resource;
    this.action = action;
    this.envID = envID;
    this.serviceID = serviceID;
  }

  public String getResource() {
    return resource;
  }

  public void setResource(String resource) {
    this.resource = resource.toUpperCase();
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action.toUpperCase();
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

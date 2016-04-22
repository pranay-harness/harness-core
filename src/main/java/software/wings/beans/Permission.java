package software.wings.beans;

import org.mongodb.morphia.annotations.Embedded;

/**
 * Created by anubhaw on 3/17/16.
 */
@Embedded
public class Permission {
  private String resource;
  private String action;
  private String envId;
  private String serviceId;

  public Permission() {}

  /**
   * Create a permission object with fields.
   * @param resource resource name.
   * @param action allowed action.
   * @param envId environment id.
   * @param serviceId service id.
   */
  public Permission(String resource, String action, String envId, String serviceId) {
    this.resource = resource;
    this.action = action;
    this.envId = envId;
    this.serviceId = serviceId;
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

  public String getEnvId() {
    return envId;
  }

  public void setEnvId(String envId) {
    this.envId = envId;
  }

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }
}

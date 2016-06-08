package software.wings.beans;

import org.mongodb.morphia.annotations.Embedded;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 3/17/16.
 */
@Embedded
public class Permission {
  private String resource;
  private String action;
  private String envId;
  private String serviceId;

  /**
   * Instantiates a new permission.
   */
  public Permission() {}

  /**
   * Create a permission object with fields.
   *
   * @param resource  resource name.
   * @param action    allowed action.
   * @param envId     environment id.
   * @param serviceId service id.
   */
  public Permission(String resource, String action, String envId, String serviceId) {
    this.resource = resource;
    this.action = action;
    this.envId = envId;
    this.serviceId = serviceId;
  }

  /**
   * Gets resource.
   *
   * @return the resource
   */
  public String getResource() {
    return resource;
  }

  /**
   * Sets resource.
   *
   * @param resource the resource
   */
  public void setResource(String resource) {
    this.resource = resource.toUpperCase();
  }

  /**
   * Gets action.
   *
   * @return the action
   */
  public String getAction() {
    return action;
  }

  /**
   * Sets action.
   *
   * @param action the action
   */
  public void setAction(String action) {
    this.action = action.toUpperCase();
  }

  /**
   * Gets env id.
   *
   * @return the env id
   */
  public String getEnvId() {
    return envId;
  }

  /**
   * Sets env id.
   *
   * @param envId the env id
   */
  public void setEnvId(String envId) {
    this.envId = envId;
  }

  /**
   * Gets service id.
   *
   * @return the service id
   */
  public String getServiceId() {
    return serviceId;
  }

  /**
   * Sets service id.
   *
   * @param serviceId the service id
   */
  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }
}

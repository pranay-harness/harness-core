package software.wings.security;

/**
 * Created by anubhaw on 3/10/16.
 */
public class PermissionAttribute {
  private ResourceType resourceType;
  private Action action;
  private PermissionScope scope;

  /**
   * Instantiates a new Permission attribute.
   *
   * @param permission the permission
   * @param scope      the scope
   */
  public PermissionAttribute(String permission, PermissionScope scope) {
    String[] permissionBits = permission.split(":");
    resourceType = ResourceType.valueOf(permissionBits[0]);
    action = Action.valueOf(permissionBits[1]);
    this.scope = scope;
  }

  /**
   * Gets resource type.
   *
   * @return the resource type
   */
  public ResourceType getResourceType() {
    return resourceType;
  }

  /**
   * Sets resource type.
   *
   * @param resourceType the resource type
   */
  public void setResourceType(ResourceType resourceType) {
    this.resourceType = resourceType;
  }

  /**
   * Gets action.
   *
   * @return the action
   */
  public Action getAction() {
    return action;
  }

  /**
   * Sets action.
   *
   * @param action the action
   */
  public void setAction(Action action) {
    this.action = action;
  }

  /**
   * Gets scope.
   *
   * @return the scope
   */
  public PermissionScope getScope() {
    return scope;
  }

  /**
   * Sets scope.
   *
   * @param scope the scope
   */
  public void setScope(PermissionScope scope) {
    this.scope = scope;
  }

  /**
   * The Enum Resource.
   */
  public enum ResourceType {
    /**
     * Any resource.
     */
    ANY, /**
          * App resource.
          */
    APPLICATION, /**
                  * Service resource.
                  */
    SERVICE, /**
              * Config resource.
              */
    CONFIGURATION, /**
                    * Env resource.
                    */
    ENVIRONMENT, /**
                  * Role resource.
                  */
    ROLE, /**
           * Host resource.
           */
    HOST, /**
           * Deployment resource.
           */
    DEPLOYMENT, /**
                 * Release resource.
                 */
    RELEASE, /**
              * Artifcats resource.
              */
    ARTIFACT, /**
               * User resource.
               */
    USER
  }

  /**
   * The Enum Action.
   */
  public enum Action {
    /**
     * All action.
     */
    ALL, /**
          * /**
          * Read action.
          */
    READ, /**
           * WRITE action.
           */
    WRITE
  }

  /**
   * The enum Permission type.
   */
  public enum PermissionScope {
    /**
     * App permission type.
     */
    APP, /**
          * Env permission type.
          */
    ENV
  }
}

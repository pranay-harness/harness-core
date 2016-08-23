package software.wings.security;

import static software.wings.security.PermissionAttribute.Action.ALL;
import static software.wings.security.PermissionAttribute.Action.READ;
import static software.wings.security.PermissionAttribute.Action.WRITE;
import static software.wings.security.PermissionAttribute.ResourceType.ANY;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;
import static software.wings.security.PermissionAttribute.ResourceType.PLATFORM;

/**
 * Created by anubhaw on 3/10/16.
 */
public enum PermissionAttribute {
  /**
   * App create permission attr.
   */
  APP_READ(APPLICATION, READ, true, false),
  /**
   * App delete permission attr.
   */
  APP_WRITE(APPLICATION, WRITE, true, false),
  /**
   * Platform create permission attr.
   */
  PLATFORM_READ(PLATFORM, READ, true, false),
  /**
   * Platform delete permission attr.
   */
  PLATFORM_WRITE(PLATFORM, WRITE, true, false),
  /**
   * Service permission attr.
   */
  SERVICE(ANY, ALL), /**
                      * Config permission attr.
                      */
  CONFIG(ANY, ALL), /**
                     * Env permission attr.
                     */
  ENV(ANY, ALL), /**
                  * Role permission attr.
                  */
  ROLE(ANY, ALL), /**
                   * User permission attr.
                   */
  USER(ANY, ALL), /**
                   * Deployment permission attr.
                   */
  DEPLOYMENT(ANY, ALL), /**
                         * Release permission attr.
                         */
  RELEASE(ANY, ALL), /**
                      * Delivery permission attr.
                      */
  DELIVERY(ANY, ALL), /**
                       * Artifacts permission attr.
                       */
  ARTIFACTS(ANY, ALL);

  private ResourceType resourceType;
  private Action action;
  private boolean onApp = true;
  private boolean onEnv = true;

  /**
   * Instantiates a new permission attr.
   *
   * @param resourceType the resource
   * @param action       the action
   */
  PermissionAttribute(ResourceType resourceType, Action action) {
    this.resourceType = resourceType;
    this.action = action;
  }

  /**
   * Instantiates a new permission attr.
   *
   * @param resourceType the resource
   * @param action       the action
   * @param onApp        the on app
   * @param onEnv        the on env
   */
  PermissionAttribute(ResourceType resourceType, Action action, boolean onApp, boolean onEnv) {
    this.resourceType = resourceType;
    this.action = action;
    this.onApp = onApp;
    this.onEnv = onEnv;
  }

  /**
   * Gets resource.
   *
   * @return the resource
   */
  public ResourceType getResourceType() {
    return resourceType;
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
   * Is on app boolean.
   *
   * @return the boolean
   */
  public boolean isOnApp() {
    return onApp;
  }

  /**
   * Is on env boolean.
   *
   * @return the boolean
   */
  public boolean isOnEnv() {
    return onEnv;
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
                  * Platform resource.
                  */
    PLATFORM, /**
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
}

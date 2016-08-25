package software.wings.security;

import static software.wings.security.PermissionAttribute.Action.READ;
import static software.wings.security.PermissionAttribute.Action.WRITE;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;
import static software.wings.security.PermissionAttribute.ResourceType.ARTIFACT;
import static software.wings.security.PermissionAttribute.ResourceType.CONFIGURATION;
import static software.wings.security.PermissionAttribute.ResourceType.DEPLOYMENT;
import static software.wings.security.PermissionAttribute.ResourceType.ENVIRONMENT;
import static software.wings.security.PermissionAttribute.ResourceType.PLATFORM;
import static software.wings.security.PermissionAttribute.ResourceType.RELEASE;
import static software.wings.security.PermissionAttribute.ResourceType.ROLE;
import static software.wings.security.PermissionAttribute.ResourceType.SERVICE;
import static software.wings.security.PermissionAttribute.ResourceType.USER;

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
   * Service read permission attribute.
   */
  SERVICE_READ(SERVICE, READ, true, false),
  /**
   * Service write permission attribute.
   */
  SERVICE_WRITE(SERVICE, WRITE, true, false),

  /**
   * Environment read permission attribute.
   */
  ENVIRONMENT_READ(ENVIRONMENT, READ, true, false),
  /**
   * Environment write permission attribute.
   */
  ENVIRONMENT_WRITE(ENVIRONMENT, WRITE, true, true),

  /**
   * Configuration read permission attribute.
   */
  CONFIGURATION_READ(CONFIGURATION, READ, true, false),
  /**
   * Configuration write permission attribute.
   */
  CONFIGURATION_WRITE(CONFIGURATION, WRITE, true, false),

  /**
   * Role read permission attribute.
   */
  ROLE_READ(ROLE, READ, true, false),
  /**
   * Role write permission attribute.
   */
  ROLE_WRITE(ROLE, WRITE, true, false),

  /**
   * User read permission attribute.
   */
  USER_READ(USER, READ, true, false),
  /**
   * User write permission attribute.
   */
  USER_WRITE(USER, WRITE, true, false),

  /**
   * Deployment read permission attribute.
   */
  DEPLOYMENT_READ(DEPLOYMENT, READ),
  /**
   * Deployment write permission attribute.
   */
  DEPLOYMENT_WRITE(DEPLOYMENT, WRITE),

  /**
   * Release read permission attribute.
   */
  RELEASE_READ(RELEASE, READ),
  /**
   * Release write permission attribute.
   */
  RELEASE_WRITE(RELEASE, WRITE),

  /**
   * Artifact read permission attribute.
   */
  ARTIFACT_READ(ARTIFACT, READ),
  /**
   * Artifact write permission attribute.
   */
  ARTIFACT_WRITE(ARTIFACT, WRITE);

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

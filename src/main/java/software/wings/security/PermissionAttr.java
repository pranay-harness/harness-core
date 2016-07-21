package software.wings.security;

import static software.wings.security.PermissionAttr.Action.ALL;
import static software.wings.security.PermissionAttr.Action.CREATE;
import static software.wings.security.PermissionAttr.Action.DELETE;
import static software.wings.security.PermissionAttr.Action.READ;
import static software.wings.security.PermissionAttr.Resource.ANY;
import static software.wings.security.PermissionAttr.Resource.APP;
import static software.wings.security.PermissionAttr.Resource.PLATFORM;

/**
 * Created by anubhaw on 3/10/16.
 */
public enum PermissionAttr {
  /**
   * App create permission attr.
   */
  APP_CREATE(APP, CREATE), /**
                            * App read permission attr.
                            */
  APP_READ(APP, READ, false, false), /**
                                      * App delete permission attr.
                                      */
  APP_DELETE(APP, DELETE), /**
                            * Platform create permission attr.
                            */
  PLATFORM_CREATE(PLATFORM, CREATE), /**
                                      * Platform read permission attr.
                                      */
  PLATFORM_READ(PLATFORM, READ, false, false), /**
                                                * Platform delete permission attr.
                                                */
  PLATFORM_DELETE(PLATFORM, DELETE), /**
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

  private Resource resource;
  private Action action;
  private boolean onApp = true;
  private boolean onEnv = true;

  /**
   * Instantiates a new permission attr.
   *
   * @param resource the resource
   * @param action   the action
   */
  PermissionAttr(Resource resource, Action action) {
    this.resource = resource;
    this.action = action;
  }

  /**
   * Instantiates a new permission attr.
   *
   * @param resource the resource
   * @param action   the action
   * @param onApp    the on app
   * @param onEnv    the on env
   */
  PermissionAttr(Resource resource, Action action, boolean onApp, boolean onEnv) {
    this.resource = resource;
    this.action = action;
    this.onApp = onApp;
    this.onEnv = onEnv;
  }

  /**
   * Gets resource.
   *
   * @return the resource
   */
  public Resource getResource() {
    return resource;
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
  public enum Resource {
    /**
     * Any resource.
     */
    ANY, /**
          * App resource.
          */
    APP, /**
          * Platform resource.
          */
    PLATFORM, /**
               * Service resource.
               */
    SERVICE, /**
              * Config resource.
              */
    CONFIG, /**
             * Env resource.
             */
    ENV, /**
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
              * Delivery resource.
              */
    DELIVERY, /**
               * Artifcats resource.
               */
    ARTIFCATS, /**
                * User resource.
                */
    USER;
  }

  /**
   * The Enum Action.
   */
  public enum Action {
    /**
     * All action.
     */
    ALL, /**
          * Create action.
          */
    CREATE, /**
             * Read action.
             */
    READ, /**
           * Delete action.
           */
    DELETE;
  }
}

package software.wings.security;

import static software.wings.security.PermissionAttribute.PermissionScope.ACCOUNT;
import static software.wings.security.PermissionAttribute.PermissionScope.APP;
import static software.wings.security.PermissionAttribute.PermissionScope.ENV;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Created by anubhaw on 3/10/16.
 */
public class PermissionAttribute {
  private static final Map<String, Action> methodActionMap =
      ImmutableMap.of("GET", Action.READ, "PUT", Action.UPDATE, "POST", Action.CREATE, "DELETE", Action.DELETE);
  private ResourceType resourceType;
  private Action action;
  private PermissionScope scope;

  public PermissionAttribute(ResourceType resourceType, Action action) {
    this.resourceType = resourceType;
    this.action = action;
    this.scope = resourceType.getActionPermissionScopeMap().get(action);
  }

  /**
   * Instantiates a new Permission attribute.
   *  @param permission the permission
   * @param scope      the scope
   * @param method
   */
  public PermissionAttribute(ResourceType permission, PermissionScope scope, String method) {
    resourceType = permission;
    this.scope = scope;
    this.action = methodActionMap.get(method);
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
     * App resource.
     */
    APPLICATION(APP), /**
                       * Service resource.
                       */
    SERVICE(APP), /**
                   * Config resource.
                   */
    CONFIGURATION(APP), /**
                         * Configuration Override resource.
                         */
    CONFIGURATION_OVERRIDE(ENV), /**
                                  * Configuration Override resource.
                                  */
    WORKFLOW(ENV), /**
                    * Env resource.
                    */
    ENVIRONMENT(APP, ENV, ENV, APP), /**
                                      * Role resource.
                                      */
    ROLE(APP), /**
                * Deployment resource.
                */
    DEPLOYMENT(ENV), /**
                      * Artifcats resource.
                      */
    ARTIFACT(APP), /**
                    * User resource.
                    */
    CLOUD(ACCOUNT), /**
                     * User resource.
                     */
    USER(ACCOUNT), /**
                    * User resource.
                    */
    SETTING(ACCOUNT);

    private ImmutableMap<Action, PermissionScope> actionPermissionScopeMap;

    ResourceType(PermissionScope permissionScope) {
      this(permissionScope, permissionScope);
    }

    ResourceType(PermissionScope readPermissionScope, PermissionScope writePermissionScope) {
      this(writePermissionScope, readPermissionScope, writePermissionScope, writePermissionScope);
    }

    ResourceType(PermissionScope createPermissionScope, PermissionScope readPermissionScope,
        PermissionScope updatePermissionScope, PermissionScope deletePermissionScope) {
      actionPermissionScopeMap = ImmutableMap.of(Action.CREATE, createPermissionScope, Action.READ, readPermissionScope,
          Action.UPDATE, updatePermissionScope, Action.DELETE, deletePermissionScope);
    }

    public ImmutableMap<Action, PermissionScope> getActionPermissionScopeMap() {
      return actionPermissionScopeMap;
    }
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
           * Update action.
           */
    UPDATE, /**
             * Delete action.
             */
    DELETE
  }

  /**
   * The enum Permission type.
   */
  public enum PermissionScope {
    /**
     * Account permission type.
     */
    ACCOUNT, /**
              * Multiple App permission type.
              */
    MULTI_APP, /**
                * App permission type.
                */
    APP, /**
          * Env permission type.
          */
    ENV, /**
          * Logged In permission type.
          */
    LOGGED_IN
  }
}

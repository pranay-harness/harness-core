package software.wings.security;

import static software.wings.security.PermissionAttribute.PermissionScope.ACCOUNT;
import static software.wings.security.PermissionAttribute.PermissionScope.APP;
import static software.wings.security.PermissionAttribute.PermissionScope.ENV;
import static software.wings.security.PermissionAttribute.PermissionScope.NONE;

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

  /**
   * Instantiates a new Permission attribute.
   *
   * @param resourceType the resource type
   * @param action       the action
   */
  public PermissionAttribute(ResourceType resourceType, Action action) {
    this.resourceType = resourceType;
    this.action = action;
    this.scope = resourceType.getActionPermissionScopeMap().get(action);
  }

  /**
   * Instantiates a new Permission attribute.
   *
   * @param permission the permission
   * @param scope      the scope
   * @param method     the method
   */
  public PermissionAttribute(ResourceType permission, PermissionScope scope, String method) {
    resourceType = permission;
    this.action = methodActionMap.get(method);
    this.scope = scope;
    if (scope == null || scope == NONE) {
      this.scope = resourceType.getActionPermissionScopeMap().get(action);
    }
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
    APPLICATION(APP),
    /**
     * Service resource.
     */
    SERVICE(APP),
    /**
     * Config resource.
     */
    CONFIGURATION(APP),
    /**
     * Configuration Override resource.
     */
    CONFIGURATION_OVERRIDE(ENV),
    /**
     * Configuration Override resource.
     */
    WORKFLOW(ENV),
    /**
     * Env resource.
     */
    ENVIRONMENT(APP, ENV, ENV, APP),
    /**
     * Role resource.
     */
    ROLE(ACCOUNT),
    /**
     * Deployment resource.
     */
    DEPLOYMENT(ENV),
    /**
     * Artifacts resource.
     */
    ARTIFACT(APP),
    /**
     * User resource.
     */
    CLOUD(ACCOUNT),
    /**
     * User resource.
     */
    USER(ACCOUNT),
    /**
     * CD resource.
     */
    CD(APP),
    /**
     * Pipeline resource.
     */
    PIPELINE(APP),
    /**
     * Setting resource.
     */
    SETTING(ACCOUNT),
    /**
     * App stack resource type.
     */
    APP_STACK(ACCOUNT),
    /**
     * Notificaion Group.
     */
    NOTIFICATION_GROUP(ACCOUNT),

    /**
     * Delegate resource type.
     */
    DELEGATE(PermissionScope.DELEGATE),
    /**
     * Delegate Scope resource type.
     */
    DELEGATE_SCOPE(PermissionScope.DELEGATE);

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

    /**
     * Gets action permission scope map.
     *
     * @return the action permission scope map
     */
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
    ALL,
    /**
     * Create action.
     */
    CREATE,
    /**
     * Read action.
     */
    READ,
    /**
     * Update action.
     */
    UPDATE,
    /**
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
    ACCOUNT,
    /**
     * Multiple App permission type.
     */
    APP,
    /**
     * Env permission type.
     */
    ENV,
    /**
     * Logged In permission type.
     */
    LOGGED_IN,
    /**
     * Delegate In permission type.
     */
    DELEGATE,

    /**
     * None permission scope.
     */
    NONE
  }
}

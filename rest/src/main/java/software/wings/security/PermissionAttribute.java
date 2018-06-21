package software.wings.security;

import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT;
import static software.wings.security.PermissionAttribute.PermissionType.APP;
import static software.wings.security.PermissionAttribute.PermissionType.ENV;
import static software.wings.security.PermissionAttribute.PermissionType.NONE;

import com.google.common.collect.ImmutableMap;

import lombok.Data;
import software.wings.exception.WingsException;

import java.util.Map;

/**
 * Created by anubhaw on 3/10/16.
 */
@Data
public class PermissionAttribute {
  private static final Map<String, Action> methodActionMap =
      ImmutableMap.of("GET", Action.READ, "PUT", Action.UPDATE, "POST", Action.CREATE, "DELETE", Action.DELETE);
  private ResourceType resourceType;
  private Action action;
  private PermissionType permissionType;

  // If the query / path parameter has a different name other than the default name for the permission type.
  private String parameterName;
  private String dbFieldName;
  private String dbCollectionName;
  private boolean skipAuth;

  /**
   * This constructor is used by old rbac code
   * @param resourceType
   * @param action
   */
  public PermissionAttribute(ResourceType resourceType, Action action) {
    this.resourceType = resourceType;
    this.action = action;
    this.permissionType = resourceType.getActionPermissionScopeMap().get(action);
  }

  /**
   * This constructor is used by old rbac code
   * @param resourceType
   * @param permissionType
   * @param method
   */
  public PermissionAttribute(ResourceType resourceType, PermissionType permissionType, String method) {
    this.resourceType = resourceType;
    this.action = methodActionMap.get(method);
    this.permissionType = permissionType;
    if (permissionType == null || permissionType == NONE) {
      this.permissionType = resourceType.getActionPermissionScopeMap().get(action);
    }
  }

  public PermissionAttribute(PermissionType permissionType, Action action) {
    this(null, permissionType, action, null, null, null, null, false);
  }

  public PermissionAttribute(ResourceType resourceType, PermissionType permissionType, Action action) {
    this(resourceType, permissionType, action, null, null, null, null, false);
  }

  /**
   *
   * @param resourceType
   * @param permissionType
   * @param action
   */
  public PermissionAttribute(ResourceType resourceType, PermissionType permissionType, Action action, String method) {
    this(resourceType, permissionType, action, method, null, null, null, false);
  }

  /**
   * Instantiates a new Permission attribute.
   *
   * @param resourceType the resource type
   * @param permissionType      the permissionType
   * @param method     the method
   */
  public PermissionAttribute(ResourceType resourceType, PermissionType permissionType, Action action, String method,
      String parameterName, String dbFieldName, String dbCollectionName, boolean skipAuth) {
    this.resourceType = resourceType;
    this.permissionType = permissionType;
    if (permissionType != null && permissionType != NONE) {
      if (action != null) {
        this.action = action;
      } else {
        if (method != null) {
          this.action = methodActionMap.get(method);
        } else {
          throw new WingsException("Either action or method has to be specified if permission type is specified");
        }
      }
    }

    this.parameterName = parameterName;
    this.dbFieldName = dbFieldName;
    this.dbCollectionName = dbCollectionName;
    this.skipAuth = skipAuth;
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
    DELEGATE(PermissionType.DELEGATE),
    /**
     * Delegate Scope resource type.
     */
    DELEGATE_SCOPE(PermissionType.DELEGATE),

    WHITE_LIST(ACCOUNT),

    SSO(ACCOUNT),

    API_KEY(ACCOUNT),

    PROVISIONER(APP),
    PREFERENCE(ACCOUNT);

    private ImmutableMap<Action, PermissionType> actionPermissionScopeMap;

    ResourceType(PermissionType permissionScope) {
      this(permissionScope, permissionScope);
    }

    ResourceType(PermissionType readPermissionScope, PermissionType writePermissionScope) {
      this(writePermissionScope, readPermissionScope, writePermissionScope, writePermissionScope);
    }

    ResourceType(PermissionType createPermissionScope, PermissionType readPermissionScope,
        PermissionType updatePermissionScope, PermissionType deletePermissionScope) {
      actionPermissionScopeMap = ImmutableMap.of(Action.CREATE, createPermissionScope, Action.READ, readPermissionScope,
          Action.UPDATE, updatePermissionScope, Action.DELETE, deletePermissionScope);
    }

    /**
     * Gets action permission permissionType map.
     *
     * @return the action permission permissionType map
     */
    public ImmutableMap<Action, PermissionType> getActionPermissionScopeMap() {
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
    DELETE,
    /**
     * Delete action.
     */
    EXECUTE,
    /**
     * default action.
     */
    DEFAULT
  }

  /**
   * The enum Permission type.
   */
  public enum PermissionType {
    /**
     * Account permission type.
     */
    ACCOUNT,
    /**
     * Logged In permission type.
     */
    LOGGED_IN,
    /**
     * Delegate In permission type.
     */
    DELEGATE,
    /**
     * None permission permissionType.
     */
    NONE,
    /**
     * App permission type.
     */
    APP,
    /**
     * All App permission types.
     */
    ALL_APP_ENTITIES,
    /**
     * Env permission type.
     */
    ENV,
    /**
     * Service permission permissionType
     */
    SERVICE,
    /**
     * Workflow permission permissionType
     */
    WORKFLOW,
    /**
     * Pipeline permission permissionType
     */
    PIPELINE,
    /**
     * Deployment permission permissionType
     */
    DEPLOYMENT,
    /**
     * Account permission type.
     */
    APPLICATION_CREATE_DELETE,
    /**
     * Account permission type.
     */
    USER_PERMISSION_MANAGEMENT,
    /**
     * Account permission type.
     */
    ACCOUNT_MANAGEMENT,
    /**
     * Provisioner permission permissionType
     */
    PROVISIONER,
  }
}

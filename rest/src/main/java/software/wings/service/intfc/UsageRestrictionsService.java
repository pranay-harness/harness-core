package software.wings.service.intfc;

import software.wings.beans.security.restrictions.RestrictionsSummary;
import software.wings.settings.UsageRestrictions;

/**
 * Usage restrictions related service apis. Account level entities (cloud providers, connectors, secrets) has concept of
 * usage restrictions. The user can restrict entities to certain apps and envs.
 *
 * @author rktummala on 06/05/2018
 */
public interface UsageRestrictionsService {
  /**
   * Derive the user restrictions from user permissions.
   * @param accountId account Id
   * @return Usage restrictions
   */
  UsageRestrictions getUsageRestrictionsFromUserPermissions(String accountId, boolean evaluateIfEditable);

  /**
   * Check if the user has access to an entity from the given context.
   * Access is determined from Usage restrictions, permissions and context.
   * @param usageRestrictions usage restrictions
   * @param accountId account id
   * @param appId app id
   * @param envId env id
   * @return boolean if the user needs to be provided access or not
   */
  boolean hasAccess(
      UsageRestrictions usageRestrictions, String accountId, String appId, String envId, String requestId);

  /**
   * Lists all the applications and environments that the user has update permissions on.
   * The update permissions could be on any of the env based permission types. (For example, permission types ENV /
   * WORKFLOW / PIPELINE)
   * @param accountId account id
   * @return Restrictions summary that has app and env info.
   */
  RestrictionsSummary listAppsWithEnvUpdatePermissions(String accountId);

  /**
   * Gets the default usage restrictions based on user permissions and the current app env context.
   * @param accountId account id
   * @param appId app id
   * @param envId env id
   * @return usage restrictions
   */
  UsageRestrictions getDefaultUsageRestrictions(String accountId, String appId, String envId);

  /**
   * Checks if the user can update / delete entity based on the user permissions and restrictions set on the entity.
   * @param accountId account id
   * @param entityUsageRestrictions entity usage restrictions
   * @return boolean
   */
  boolean userHasPermissionsToChangeEntity(String accountId, UsageRestrictions entityUsageRestrictions);

  /**
   * Checks if user can update an entity
   * @param oldUsageRestrictions old usage restrictions
   * @param newUsageRestrictions new usage restrictions
   * @param accountId account id
   */
  void validateUsageRestrictionsOnEntityUpdate(
      String accountId, UsageRestrictions oldUsageRestrictions, UsageRestrictions newUsageRestrictions);

  /**
   * Checks if user can create an entity.
   * @param newUsageRestrictions new usage restrictions in case of update
   * @param accountId account id
   */
  void validateUsageRestrictionsOnEntitySave(String accountId, UsageRestrictions newUsageRestrictions);
}

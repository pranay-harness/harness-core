package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Application;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ownership.OwnedByAccount;

import java.util.List;

/**
 * Application Service.
 *
 * @author Rishi
 */
public interface AppService extends OwnedByAccount, Exterminator {
  /**
   * Save.
   *
   * @param app the app
   * @return the application
   */
  Application save(Application app);

  PageResponse<Application> list(PageRequest<Application> req, boolean details);

  PageResponse<Application> list(PageRequest<Application> req);

  /**
   * Exist boolean.
   *
   * @param appId the app id
   * @return the boolean
   */
  boolean exist(@NotEmpty String appId);

  /**
   * Find by uuid.
   *
   * @param uuid the uuid
   * @return the application
   */
  Application get(@NotEmpty String uuid);

  /**
   * Retrieves the application with defaults
   * @param uuid
   * @return
   */
  Application getApplicationWithDefaults(@NotEmpty String uuid);

  /**
   * Get application.
   *
   * @param appId        the app id
   * @param details the details
   * @return the application
   */
  Application get(@NotEmpty String appId, boolean details);

  Application getAppByName(String accountId, String appName);

  /**
   * Update.
   *
   * @param app the app
   * @return the application
   */
  Application update(Application app);

  /**
   * Delete app.
   *
   * @param appId the app id
   */
  void delete(@NotEmpty String appId);

  /**
   * Prune owned from the app entities.
   *
   * @param appId the app id
   */
  void pruneDescendingEntities(@NotEmpty String appId);

  /**
   * Gets apps by account id.
   *
   * @param accountId the account id
   * @return the apps by account id
   */
  List<Application> getAppsByAccountId(String accountId);

  /**
   * Gets app ids by account id.
   *
   * @param accountId the account id
   * @return the app ids by account id
   */
  List<String> getAppIdsByAccountId(String accountId);

  /**
   * Gets app names by account id.
   *
   * @param accountId the account id
   * @return the app names by account id
   */
  List<String> getAppNamesByAccountId(String accountId);

  String getAccountIdByAppId(String appId);

  void delete(String appId, boolean syncFromGit);
}

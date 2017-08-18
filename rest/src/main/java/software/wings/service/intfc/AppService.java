package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Application;
import software.wings.beans.Setup.SetupStatus;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

import java.util.List;

/**
 * Application Service.
 *
 * @author Rishi
 */
public interface AppService {
  /**
   * Save.
   *
   * @param app the app
   * @return the application
   */
  Application save(Application app);

  /**
   * List page response.
   *
   * @param req the req
   * @return the page response
   */
  /* (non-Javadoc)
   * @see software.wings.service.intfc.AppService#list(software.wings.dl.PageRequest)
   */
  PageResponse<Application> list(PageRequest<Application> req);

  /**
   * List.
   *
   * @param req                the req
   * @param overview           the summary
   * @param numberOfExecutions the number of executions
   * @param overviewDays       the overview days
   * @return the page response
   */
  PageResponse<Application> list(
      PageRequest<Application> req, boolean overview, int numberOfExecutions, int overviewDays);

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
   * Get application.
   *
   * @param appId        the app id
   * @param status       the status
   * @param overview     the overview
   * @param overviewDays the overview days
   * @return the application
   */
  Application get(@NotEmpty String appId, SetupStatus status, boolean overview, int overviewDays);

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

  /**
   * Delete by acount id.
   *
   * @param accountId the account id
   */
  void deleteByAccountId(String accountId);
}

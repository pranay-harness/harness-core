package software.wings.service.intfc;

import software.wings.beans.Application;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

// TODO: Auto-generated Javadoc

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
   * List.
   *
   * @param req the req
   * @return the page response
   */
  PageResponse<Application> list(PageRequest<Application> req);

  /**
   * Find by uuid.
   *
   * @param uuid the uuid
   * @return the application
   */
  Application findByUuid(String uuid);

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
  void deleteApp(String appId);
}

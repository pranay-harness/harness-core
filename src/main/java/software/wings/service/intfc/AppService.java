package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.Setup.SetupStatus;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

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
   * @param req      the req
   * @param overview the summary
   * @return the page response
   */
  PageResponse<Application> list(PageRequest<Application> req, boolean overview, int numberOfExecutions);

  /**
   * Find by uuid.
   *
   * @param uuid the uuid
   * @return the application
   */
  Application get(String uuid);

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
   * Add environment.
   *
   * @param env the env
   */
  void addEnvironment(Environment env);

  /**
   * Add service.
   *
   * @param service the service
   */
  void addService(Service service);

  Application get(String appId, SetupStatus status);
}

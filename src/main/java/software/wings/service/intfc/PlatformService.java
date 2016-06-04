package software.wings.service.intfc;

import software.wings.beans.AppContainer;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

// TODO: Auto-generated Javadoc

/**
 * PlatformService.
 *
 * @author Rishi
 */
public interface PlatformService {
  /**
   * List.
   *
   * @param req the req
   * @return the page response
   */
  public PageResponse<AppContainer> list(PageRequest<AppContainer> req);

  /**
   * Creates the.
   *
   * @param platform the platform
   * @return the app container
   */
  public AppContainer create(AppContainer platform);

  /**
   * Update.
   *
   * @param platform the platform
   * @return the app container
   */
  public AppContainer update(AppContainer platform);
}

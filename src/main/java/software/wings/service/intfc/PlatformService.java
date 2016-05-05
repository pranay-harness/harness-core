package software.wings.service.intfc;

import software.wings.beans.AppContainer;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;

/**
 * PlatformService.
 *
 * @author Rishi
 */
public interface PlatformService {
  public PageResponse<AppContainer> list(PageRequest<AppContainer> req);

  public AppContainer create(AppContainer platform);

  public AppContainer update(AppContainer platform);
}

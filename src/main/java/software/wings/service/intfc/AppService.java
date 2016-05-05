package software.wings.service.intfc;

import software.wings.beans.Application;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;

import java.util.List;

/**
 * Application Service.
 *
 * @author Rishi
 */
public interface AppService {
  Application save(Application app);

  List<Application> list();

  PageResponse<Application> list(PageRequest<Application> req);

  Application findByUuid(String uuid);

  Application update(Application app);

  void deleteApp(String appId);
}

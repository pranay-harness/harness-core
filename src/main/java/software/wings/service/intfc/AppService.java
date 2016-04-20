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
  public Application save(Application app);

  public List<Application> list();

  public PageResponse<Application> list(PageRequest<Application> req);

  public Application findByUUID(String uuid);

  public Application update(Application app);
}

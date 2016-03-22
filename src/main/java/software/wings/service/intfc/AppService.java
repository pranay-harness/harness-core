package software.wings.service.intfc;

import java.util.List;

import software.wings.beans.Application;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;

/**
 *  Application Service.
 *
 *
 * @author Rishi
 *
 */
public interface AppService {
  public Application save(Application app);
  public List<Application> list();

  public PageResponse<Application> list(PageRequest<Application> req);
  public Application findByUUID(String uuid);
  public Application update(Application app);
}

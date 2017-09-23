package software.wings.service.intfc.yaml;

import software.wings.beans.Application;
import software.wings.beans.Service;

/**
 * Entity Update Service.
 *
 * @author bsollish
 */
public interface EntityUpdateService {
  public void appUpdate(Application app);

  public void serviceUpdate(Service service);
}

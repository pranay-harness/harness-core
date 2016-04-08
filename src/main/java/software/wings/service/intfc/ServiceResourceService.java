package software.wings.service.intfc;

import software.wings.beans.Service;

import java.util.List;

/**
 * Created by anubhaw on 3/28/16.
 */
public interface ServiceResourceService {
  public List<Service> list(String appID);
  public Service save(String appID, Service service);
  public Service findByUUID(String uuid);
  public Service update(Service service);
}

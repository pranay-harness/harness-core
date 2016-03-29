package software.wings.service.intfc;

import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Service;

/**
 * Created by anubhaw on 3/28/16.
 */
public interface ServiceResourceService {
  public PageResponse<Service> list(PageRequest<Service> pageRequest);
  public Service save(Service service);
  public Service findByUUID(String uuid);
  public Service update(Service service);
}

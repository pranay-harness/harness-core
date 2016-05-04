package software.wings.service.intfc;

import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Service;

/**
 * Created by anubhaw on 3/28/16.
 */
public interface ServiceResourceService {
  PageResponse<Service> list(PageRequest<Service> pageRequest);

  Service save(Service service);

  Service update(Service service);

  Service get(String serviceId);

  void delete(String serviceId);
}

package software.wings.service.intfc;

import software.wings.beans.Service;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

/**
 * Created by anubhaw on 3/28/16.
 */
public interface ServiceResourceService {
  PageResponse<Service> list(PageRequest<Service> pageRequest);

  Service save(Service service);

  Service update(Service service);

  Service get(String appId, String serviceId);

  void delete(String appId, String serviceId);
}

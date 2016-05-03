package software.wings.service.intfc;

import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Service;

/**
 * Created by anubhaw on 3/28/16.
 */
public interface ServiceResourceService {
  PageResponse<Service> list(String appId, PageRequest<Service> pageRequest);

  Service save(String appId, Service service);

  Service findByUuid(String uuid);

  Service update(Service service);

  Service get(String appId, String serviceId);

  void delete(String serviceId);
}

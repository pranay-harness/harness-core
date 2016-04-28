package software.wings.service.intfc;

import software.wings.beans.Service;

import java.util.List;

/**
 * Created by anubhaw on 3/28/16.
 */
public interface ServiceResourceService {
  List<Service> list(String appId);

  Service save(String appId, Service service);

  Service findByUuid(String uuid);

  Service update(Service service);

  Service get(String appId, String serviceId);
}

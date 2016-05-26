package software.wings.service.intfc;

import software.wings.beans.ServiceInstance;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

/**
 * Created by anubhaw on 5/26/16.
 */
public interface ServiceInstanceService {
  PageResponse<ServiceInstance> list(PageRequest<ServiceInstance> pageRequest);
  ServiceInstance save(ServiceInstance serviceInstance);
  ServiceInstance update(ServiceInstance serviceInstance);
  void delete(String appId, String envId, String instanceId);
  ServiceInstance get(String appId, String envId, String instanceId);
}

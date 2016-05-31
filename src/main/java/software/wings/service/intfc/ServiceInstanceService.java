package software.wings.service.intfc;

import software.wings.beans.Host;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

import java.util.List;

/**
 * Created by anubhaw on 5/26/16.
 */
public interface ServiceInstanceService {
  PageResponse<ServiceInstance> list(PageRequest<ServiceInstance> pageRequest);
  ServiceInstance save(ServiceInstance serviceInstance);
  ServiceInstance update(ServiceInstance serviceInstance);
  void delete(String appId, String envId, String instanceId);
  ServiceInstance get(String appId, String envId, String instanceId);
  void updateHostMappings(ServiceTemplate template, List<Host> addedHosts, List<Host> deletedHosts);
}

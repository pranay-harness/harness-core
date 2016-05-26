package software.wings.service.impl;

import software.wings.beans.ServiceInstance;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ServiceInstanceService;

import javax.inject.Inject;

/**
 * Created by anubhaw on 5/26/16.
 */

public class ServiceInstanceServiceImpl implements ServiceInstanceService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<ServiceInstance> list(PageRequest<ServiceInstance> pageRequest) {
    return wingsPersistence.query(ServiceInstance.class, pageRequest);
  }

  @Override
  public ServiceInstance save(ServiceInstance serviceInstance) {
    return wingsPersistence.saveAndGet(ServiceInstance.class, serviceInstance);
  }

  @Override
  public ServiceInstance update(ServiceInstance serviceInstance) {
    return wingsPersistence.saveAndGet(ServiceInstance.class, serviceInstance);
  }

  @Override
  public void delete(String appId, String envId, String instanceId) {
    wingsPersistence.delete(ServiceInstance.class, instanceId);
  }

  @Override
  public ServiceInstance get(String appId, String envId, String instanceId) {
    return wingsPersistence.get(ServiceInstance.class, instanceId);
  }
}

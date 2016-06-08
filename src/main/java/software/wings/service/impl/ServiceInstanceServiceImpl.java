package software.wings.service.impl;

import static software.wings.beans.ServiceInstance.ServiceInstanceBuilder.aServiceInstance;

import org.mongodb.morphia.query.Query;
import software.wings.beans.Host;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ServiceInstanceService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 5/26/16.
 */
public class ServiceInstanceServiceImpl implements ServiceInstanceService {
  @Inject private WingsPersistence wingsPersistence;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceInstanceService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<ServiceInstance> list(PageRequest<ServiceInstance> pageRequest) {
    return wingsPersistence.query(ServiceInstance.class, pageRequest);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceInstanceService#save(software.wings.beans.ServiceInstance)
   */
  @Override
  public ServiceInstance save(ServiceInstance serviceInstance) {
    return wingsPersistence.saveAndGet(ServiceInstance.class, serviceInstance);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceInstanceService#update(software.wings.beans.ServiceInstance)
   */
  @Override
  public ServiceInstance update(ServiceInstance serviceInstance) {
    return wingsPersistence.saveAndGet(ServiceInstance.class, serviceInstance);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceInstanceService#delete(java.lang.String, java.lang.String,
   * java.lang.String)
   */
  @Override
  public void delete(String appId, String envId, String instanceId) {
    wingsPersistence.delete(ServiceInstance.class, instanceId);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceInstanceService#get(java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public ServiceInstance get(String appId, String envId, String instanceId) {
    return wingsPersistence.get(ServiceInstance.class, instanceId);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceInstanceService#updateHostMappings(software.wings.beans.ServiceTemplate,
   * java.util.List, java.util.List)
   */
  @Override
  public void updateHostMappings(ServiceTemplate template, List<Host> addedHosts, List<Host> deletedHosts) {
    List<String> deletedHostIds = deletedHosts.stream().map(Host::getUuid).collect(Collectors.toList());

    Query<ServiceInstance> deleteQuery = wingsPersistence.createQuery(ServiceInstance.class)
                                             .field("appId")
                                             .equal(template.getAppId())
                                             .field("serviceTemplate")
                                             .equal(template.getUuid())
                                             .field("host")
                                             .hasAnyOf(deletedHostIds);
    wingsPersistence.delete(deleteQuery);

    List<ServiceInstance> serviceInstances = new ArrayList<>();
    addedHosts.forEach(host -> {
      serviceInstances.add(
          aServiceInstance()
              .withAppId(template.getAppId())
              .withEnvId(template.getEnvId()) // Fixme: do it one by one and ignore unique constraints failure
              .withServiceTemplate(template)
              .withHost(host)
              .build());
    });
    wingsPersistence.save(serviceInstances);
  }
}

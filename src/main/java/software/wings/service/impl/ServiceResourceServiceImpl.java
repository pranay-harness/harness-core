package software.wings.service.impl;

import com.google.inject.Inject;

import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ServiceResourceService;

/**
 * Created by anubhaw on 3/25/16.
 */
public class ServiceResourceServiceImpl implements ServiceResourceService {
  @Inject private WingsPersistence wingsPersistence;

  public PageResponse<Service> list(PageRequest<Service> pageRequest) {
    return wingsPersistence.query(Service.class, pageRequest);
  }

  public Service save(Service service) {
    return wingsPersistence.saveAndGet(Service.class, service);
  }

  public Service findByUuid(String uuid) {
    return wingsPersistence.get(Service.class, uuid);
  }

  public Service update(Service service) {
    return save(service);
  }
}

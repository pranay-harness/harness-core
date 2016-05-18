package software.wings.service.impl;

import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;

import com.google.common.collect.ImmutableMap;

import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.ServiceResourceService;

import javax.inject.Inject;

/**
 * Created by anubhaw on 3/25/16.
 */
public class ServiceResourceServiceImpl implements ServiceResourceService {
  private WingsPersistence wingsPersistence;
  private ConfigService configService;

  @Inject
  public ServiceResourceServiceImpl(WingsPersistence wingsPersistence, ConfigService configService) {
    this.wingsPersistence = wingsPersistence;
    this.configService = configService;
  }

  @Override
  public PageResponse<Service> list(PageRequest<Service> request) {
    return wingsPersistence.query(Service.class, request);
  }

  @Override
  public Service save(Service service) {
    Service savedService = wingsPersistence.saveAndGet(Service.class, service);
    wingsPersistence.addToList(Application.class, service.getAppId(), "services", savedService); // TODO: remove it
    return savedService;
  }

  @Override
  public Service update(Service service) {
    wingsPersistence.updateFields(Service.class, service.getUuid(),
        ImmutableMap.of("name", service.getName(), "description", service.getDescription(), "artifactType",
            service.getArtifactType(), "appContainer", service.getAppContainer()));
    return wingsPersistence.get(Service.class, service.getUuid());
  }

  @Override
  public Service get(String serviceId) {
    Service service = wingsPersistence.get(Service.class, serviceId);
    if (service != null) {
      service.setConfigFiles(configService.getConfigFilesForEntity(DEFAULT_TEMPLATE_ID, service.getUuid()));
    }
    return service;
  }

  @Override
  public void delete(String serviceId) {
    wingsPersistence.delete(Service.class, serviceId);
  }
}

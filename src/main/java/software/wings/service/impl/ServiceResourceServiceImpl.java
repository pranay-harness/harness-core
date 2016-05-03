package software.wings.service.impl;

import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;

import com.google.common.collect.ImmutableMap;

import software.wings.beans.Application;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.ServiceResourceService;

import javax.inject.Inject;

/**
 * Created by anubhaw on 3/25/16.
 */
public class ServiceResourceServiceImpl implements ServiceResourceService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FileService fileService;
  @Inject private ConfigService configService;

  @Override
  public PageResponse<Service> list(PageRequest<Service> request) {
    return wingsPersistence.query(Service.class, request);
  }

  public Service save(String appId, Service service) {
    Service savedService = wingsPersistence.saveAndGet(Service.class, service);
    wingsPersistence.addToList(Application.class, appId, "services", savedService);
    return savedService;
  }

  public Service findByUuid(String uuid) {
    Service service = wingsPersistence.get(Service.class, uuid);
    service.setConfigFiles(configService.getConfigFilesByEntityId(DEFAULT_TEMPLATE_ID, service.getUuid()));
    return service;
  }

  public Service update(Service service) {
    wingsPersistence.updateFields(Service.class, service.getUuid(),
        ImmutableMap.of("name", service.getName(), "description", service.getDescription(), "artifactType",
            service.getArtifactType()));
    return wingsPersistence.get(Service.class, service.getUuid());
  }

  @Override
  public Service get(String appId, String serviceId) {
    return findByUuid(serviceId);
  }
}

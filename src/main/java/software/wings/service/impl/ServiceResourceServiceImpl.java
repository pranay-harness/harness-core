package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.common.collect.ImmutableMap;

import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.ServiceResourceService;

import java.util.List;
import javax.inject.Inject;

/**
 * Created by anubhaw on 3/25/16.
 */
public class ServiceResourceServiceImpl implements ServiceResourceService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FileService fileService;

  @Override
  public List<Service> list(String appId) {
    Application application = wingsPersistence.createQuery(Application.class)
                                  .field(ID_KEY)
                                  .equal(appId)
                                  .retrievedFields(true, "services")
                                  .get();
    return application.getServices();
  }

  public Service save(String appId, Service service) {
    Service savedService = wingsPersistence.saveAndGet(Service.class, service);
    wingsPersistence.addToList(Application.class, appId, "services", savedService);
    return savedService;
  }

  public Service findByUuid(String uuid) {
    return wingsPersistence.get(Service.class, uuid);
  }

  public Service update(Service service) {
    wingsPersistence.updateFields(Service.class, service.getUuid(),
        ImmutableMap.of("name", service.getName(), "description", service.getDescription(), "artifactType",
            service.getArtifactType()));
    return wingsPersistence.get(Service.class, service.getUuid());
  }
}

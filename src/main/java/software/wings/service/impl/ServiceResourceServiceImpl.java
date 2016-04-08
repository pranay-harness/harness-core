package software.wings.service.impl;

import com.google.inject.Inject;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ServiceResourceService;

import java.util.List;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

/**
 * Created by anubhaw on 3/25/16.
 */

public class ServiceResourceServiceImpl implements ServiceResourceService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public List<Service> list(String appID) {
    Application application = wingsPersistence.createQuery(Application.class)
                                  .field(ID_KEY)
                                  .equal(appID)
                                  .retrievedFields(true, "services")
                                  .get();
    return application.getServices();
  }

  public Service save(String appID, Service service) {
    Service savedService = wingsPersistence.saveAndGet(Service.class, service);
    UpdateOperations<Application> updateOperations =
        wingsPersistence.createUpdateOperations(Application.class).add("services", savedService);
    Query<Application> updateQuery = wingsPersistence.createQuery(Application.class).field(ID_KEY).equal(appID);
    wingsPersistence.update(updateQuery, updateOperations);
    return savedService;
  }

  public Service findByUUID(String uuid) {
    return wingsPersistence.get(Service.class, uuid);
  }
  public Service update(Service service) {
    wingsPersistence.save(service);
    return service;
  }
}

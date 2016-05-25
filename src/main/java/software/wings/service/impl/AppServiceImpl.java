package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.codahale.metrics.annotation.Metered;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Application;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.SettingsService;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Application Service Implementation class.
 *
 * @author Rishi
 */
@Singleton
public class AppServiceImpl implements AppService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private SettingsService settingsService;

  @Override
  @Metered
  public Application save(Application app) {
    Application application = wingsPersistence.saveAndGet(Application.class, app);
    settingsService.createDefaultSettings(application.getUuid()); // FIXME: Should be at common place
    return application;
  }

  @Override
  public PageResponse<Application> list(PageRequest<Application> req) {
    return wingsPersistence.query(Application.class, req);
  }

  @Override
  public Application findByUuid(String uuid) {
    return wingsPersistence.get(Application.class, uuid);
  }

  @Override
  public Application update(Application app) {
    Query<Application> query = wingsPersistence.createQuery(Application.class).field(ID_KEY).equal(app.getUuid());
    UpdateOperations<Application> operations = wingsPersistence.createUpdateOperations(Application.class)
                                                   .set("name", app.getName())
                                                   .set("description", app.getDescription());
    wingsPersistence.update(query, operations);
    return wingsPersistence.get(Application.class, app.getUuid());
  }

  @Override
  public void deleteApp(String appId) {
    wingsPersistence.delete(Application.class, appId);
  }
}

package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.codahale.metrics.annotation.Metered;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Application Service Implementation class.
 *
 * @author Rishi
 */
@Singleton
public class AppServiceImpl implements AppService {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private WingsPersistence wingsPersistence;

  @Override
  @Metered
  public Application save(Application app) {
    return wingsPersistence.saveAndGet(Application.class, app);
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

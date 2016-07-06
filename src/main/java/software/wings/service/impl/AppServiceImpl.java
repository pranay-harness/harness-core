package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.codahale.metrics.annotation.Metered;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppContainerService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;

import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

// TODO: Auto-generated Javadoc

/**
 * Application Service Implementation class.
 *
 * @author Rishi
 */
@ValidateOnExecution
@Singleton
public class AppServiceImpl implements AppService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private SettingsService settingsService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private EnvironmentService environmentService;
  @Inject private AppContainerService appContainerService;
  @Inject private ExecutorService executorService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.AppService#save(software.wings.beans.Application)
   */
  @Override
  @Metered
  public Application save(Application app) {
    Application application = wingsPersistence.saveAndGet(Application.class, app);
    settingsService.createDefaultSettings(application.getUuid());
    environmentService.createDefaultEnvironments(application.getUuid());
    return application;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.AppService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<Application> list(PageRequest<Application> req) {
    return wingsPersistence.query(Application.class, req);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.AppService#get(java.lang.String)
   */
  @Override
  public Application get(String uuid) {
    return wingsPersistence.get(Application.class, uuid);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.AppService#update(software.wings.beans.Application)
   */
  @Override
  public Application update(Application app) {
    Query<Application> query = wingsPersistence.createQuery(Application.class).field(ID_KEY).equal(app.getUuid());
    UpdateOperations<Application> operations = wingsPersistence.createUpdateOperations(Application.class)
                                                   .set("name", app.getName())
                                                   .set("description", app.getDescription());
    wingsPersistence.update(query, operations);
    return wingsPersistence.get(Application.class, app.getUuid());
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.AppService#delete(java.lang.String)
   */
  @Override
  public void delete(String appId) {
    boolean deleted = wingsPersistence.delete(Application.class, appId);
    if (deleted) {
      executorService.submit(() -> {
        serviceResourceService.deleteByAppId(appId);
        environmentService.deleteByApp(appId);
        appContainerService.deleteByAppId(appId);
      });
    }
  }

  @Override
  public void addEnvironment(Environment env) {
    UpdateOperations<Application> updateOperations =
        wingsPersistence.createUpdateOperations(Application.class).add("environments", env);
    Query<Application> updateQuery =
        wingsPersistence.createQuery(Application.class).field(ID_KEY).equal(env.getAppId());
    wingsPersistence.update(updateQuery, updateOperations);
  }

  @Override
  public void addService(Service service) {
    wingsPersistence.addToList(Application.class, service.getAppId(), "services", service);
  }
}

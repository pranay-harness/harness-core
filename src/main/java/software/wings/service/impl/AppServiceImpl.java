package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ErrorCodes.INVALID_ARGUMENT;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.Setup.SetupStatus.INCOMPLETE;
import static software.wings.beans.SortOrder.Builder.aSortOrder;
import static software.wings.common.NotificationMessageResolver.ENTITY_DELETE_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.getDecoratedNotificationMessage;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.collect.ImmutableMap;

import com.codahale.metrics.annotation.Metered;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Notification;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.Setup.SetupStatus;
import software.wings.beans.SortOrder.OrderType;
import software.wings.common.NotificationMessageResolver;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppContainerService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SetupService;
import software.wings.service.intfc.WorkflowService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

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
  @Inject private SetupService setupService;
  @Inject private WorkflowService workflowService;
  @Inject private NotificationService notificationService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.AppService#save(software.wings.beans.Application)
   */
  @Override
  @Metered
  public Application save(Application app) {
    Application application = wingsPersistence.saveAndGet(Application.class, app);
    settingsService.createDefaultSettings(application.getUuid());
    environmentService.createDefaultEnvironments(application.getUuid());
    notificationService.sendNotificationAsync(
        anInformationNotification()
            .withAppId(application.getUuid())
            .withDisplayText(getDecoratedNotificationMessage(NotificationMessageResolver.ENTITY_CREATE_NOTIFICATION,
                ImmutableMap.of("ENTITY_TYPE", "Application", "ENTITY_NAME", application.getName())))
            .build());
    return get(application.getUuid());
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.AppService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<Application> list(PageRequest<Application> req, boolean overview, int numberOfExecutions) {
    PageResponse<Application> response = wingsPersistence.query(Application.class, req);
    if (overview) {
      response.getResponse().parallelStream().forEach(application -> {
        application.setRecentExecutions(
            workflowService
                .listExecutions(
                    aPageRequest()
                        .withLimit(Integer.toString(numberOfExecutions))
                        .addFilter(aSearchFilter().withField("appId", Operator.EQ, application.getUuid()).build())
                        .addOrder(aSortOrder().withField("createdAt", OrderType.DESC).build())
                        .build(),
                    false)
                .getResponse());
        application.setNotifications(getIncompleteActionableApplicationNotifications(application.getUuid()));
      });
    }
    return response;
  }

  private List<Notification> getIncompleteActionableApplicationNotifications(String appId) {
    return notificationService
        .list(aPageRequest()
                  .addFilter(aSearchFilter().withField("appId", Operator.EQ, appId).build())
                  .addFilter(aSearchFilter().withField("complete", Operator.EQ, false).build())
                  .addFilter(aSearchFilter().withField("actionable", Operator.EQ, true).build())
                  .build())
        .getResponse();
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.AppService#get(java.lang.String)
   */
  @Override
  public Application get(String uuid) {
    Application application = wingsPersistence.get(Application.class, uuid);
    if (application == null) {
      throw new WingsException(INVALID_ARGUMENT, "args", "Application doesn't exist");
    }
    application.setNotifications(getIncompleteActionableApplicationNotifications(application.getUuid()));
    return application;
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
    Application application = wingsPersistence.get(Application.class, appId);
    if (application == null) {
      throw new WingsException(INVALID_ARGUMENT, "args", "Application doesn't exist");
    }

    boolean deleted = wingsPersistence.delete(Application.class, appId);
    if (deleted) {
      executorService.submit(() -> {
        notificationService.sendNotificationAsync(
            anInformationNotification()
                .withAppId(application.getUuid())
                .withDisplayText(getDecoratedNotificationMessage(ENTITY_DELETE_NOTIFICATION,
                    ImmutableMap.of("ENTITY_TYPE", "Application", "ENTITY_NAME", application.getName())))
                .build());
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
    wingsPersistence.update(wingsPersistence.createQuery(Application.class).field(ID_KEY).equal(service.getAppId()),
        wingsPersistence.createUpdateOperations(Application.class).add("services", service));
  }

  @Override
  public void deleteService(Service service) {
    wingsPersistence.update(wingsPersistence.createQuery(Application.class).field(ID_KEY).equal(service.getAppId()),
        wingsPersistence.createUpdateOperations(Application.class).removeAll("services", service));
  }

  @Override
  public Application get(String appId, SetupStatus status) {
    Application application = get(appId);
    if (status == INCOMPLETE) {
      application.setSetup(setupService.getApplicationSetupStatus(application));
    }
    return application;
  }
}

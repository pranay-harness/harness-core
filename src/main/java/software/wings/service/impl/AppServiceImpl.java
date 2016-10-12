package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ErrorCodes.INVALID_ARGUMENT;
import static software.wings.beans.History.Builder.aHistory;
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
import software.wings.beans.EntityType;
import software.wings.beans.EventType;
import software.wings.beans.Notification;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Setup.SetupStatus;
import software.wings.beans.SortOrder.OrderType;
import software.wings.common.NotificationMessageResolver;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppContainerService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HistoryService;
import software.wings.service.intfc.InfrastructureService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.ReleaseService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SetupService;
import software.wings.service.intfc.WorkflowExecutionService;
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
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private NotificationService notificationService;
  @Inject private HistoryService historyService;
  @Inject private InfrastructureService infrastructureService;
  @Inject private WorkflowService workflowService;
  @Inject private ReleaseService releaseService;
  @Inject private ArtifactService artifactService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.AppService#save(software.wings.beans.Application)
   */
  @Override
  @Metered
  public Application save(Application app) {
    Application application = wingsPersistence.saveAndGet(Application.class, app);
    settingsService.createDefaultSettings(application.getUuid());
    infrastructureService.createDefaultInfrastructure(app.getUuid());
    environmentService.createDefaultEnvironments(application.getUuid());
    notificationService.sendNotificationAsync(
        anInformationNotification()
            .withAppId(application.getUuid())
            .withDisplayText(getDecoratedNotificationMessage(NotificationMessageResolver.ENTITY_CREATE_NOTIFICATION,
                ImmutableMap.of("ENTITY_TYPE", "Application", "ENTITY_NAME", application.getName())))
            .build());

    historyService.createAsync(aHistory()
                                   .withEventType(EventType.CREATED)
                                   .withAppId(app.getUuid())
                                   .withEntityType(EntityType.APPLICATION)
                                   .withEntityId(app.getUuid())
                                   .withEntityName(application.getName())
                                   .withEntityNewValue(application)
                                   .withShortDescription("Application " + application.getName() + " created")
                                   .withTitle("Application " + application.getName() + " created")
                                   .build());
    return get(application.getUuid(), INCOMPLETE, true);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.AppService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<Application> list(PageRequest<Application> req, boolean overview, int numberOfExecutions) {
    PageResponse<Application> response = wingsPersistence.query(Application.class, req);
    response.getResponse().parallelStream().forEach(application -> {
      application.setEnvironments(environmentService.getEnvByApp(application.getUuid()));
      application.setServices(serviceResourceService.findServicesByApp(application.getUuid()));
      if (overview) {
        application.setRecentExecutions(
            workflowExecutionService
                .listExecutions(
                    aPageRequest()
                        .withLimit(Integer.toString(numberOfExecutions))
                        .addFilter(aSearchFilter().withField("appId", Operator.EQ, application.getUuid()).build())
                        .addOrder(aSortOrder().withField("createdAt", OrderType.DESC).build())
                        .build(),
                    false)
                .getResponse());
        application.setNotifications(getIncompleteActionableApplicationNotifications(application.getUuid()));
      }
    });
    return response;
  }

  @Override
  public boolean exist(String appId) {
    return wingsPersistence.createQuery(Application.class).field(ID_KEY).equal(appId).getKey() != null;
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
        notificationService.deleteByApplication(appId);
        serviceResourceService.deleteByApp(appId);
        environmentService.deleteByApp(appId);
        workflowService.deleteWorkflowByApplication(appId);
        releaseService.deleteByApplication(appId);
        artifactService.deleteByApplication(appId);
        appContainerService.deleteByAppId(appId);
        historyService.deleteByApplication(appId);
        workflowService.deleteStateMachinesMyApplication(appId);
      });
    }

    historyService.createAsync(aHistory()
                                   .withEventType(EventType.DELETED)
                                   .withAppId(application.getUuid())
                                   .withEntityType(EntityType.APPLICATION)
                                   .withEntityId(application.getUuid())
                                   .withEntityName(application.getName())
                                   .withEntityNewValue(application)
                                   .withShortDescription("Application " + application.getName() + " created")
                                   .withTitle("Application " + application.getName() + " created")
                                   .build());
  }

  @Override
  public Application get(String appId, SetupStatus status, boolean overview) {
    Application application = get(appId);
    application.setEnvironments(environmentService.getEnvByApp(application.getUuid()));
    application.setServices(serviceResourceService.findServicesByApp(application.getUuid()));

    if (overview) {
      application.setNotifications(getIncompleteActionableApplicationNotifications(application.getUuid()));
    }
    if (status == INCOMPLETE) {
      application.setSetup(setupService.getApplicationSetupStatus(application));
    }
    return application;
  }
}

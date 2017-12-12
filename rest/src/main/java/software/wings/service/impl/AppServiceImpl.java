package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.ErrorCode.UNKNOWN_ERROR;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.ResponseMessage.Builder.aResponseMessage;
import static software.wings.beans.Role.Builder.aRole;
import static software.wings.beans.RoleType.APPLICATION_ADMIN;
import static software.wings.beans.RoleType.NON_PROD_SUPPORT;
import static software.wings.beans.RoleType.PROD_SUPPORT;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.Setup.SetupStatus.INCOMPLETE;
import static software.wings.beans.SortOrder.Builder.aSortOrder;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.name.Named;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.Notification;
import software.wings.beans.ResponseMessage;
import software.wings.beans.Role;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Setup.SetupStatus;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.stats.AppKeyStatistics;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.scheduler.ContainerSyncJob;
import software.wings.scheduler.PruneObjectJob;
import software.wings.scheduler.QuartzScheduler;
import software.wings.scheduler.StateMachineExecutionCleanupJob;
import software.wings.service.impl.yaml.YamlChangeSetHelper;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.OwnedByApplication;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.RoleService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SetupService;
import software.wings.service.intfc.StatisticsService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.utils.Validator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
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
  private static final Logger logger = LoggerFactory.getLogger(AppServiceImpl.class);

  private static final int SM_CLEANUP_POLL_INTERVAL = 60;
  private static final int INSTANCE_SYNC_POLL_INTERVAL = 600;

  @Inject private AlertService alertService;
  @Inject private ArtifactService artifactService;
  @Inject private EnvironmentService environmentService;
  @Inject private ExecutorService executorService;
  @Inject private InstanceService instanceService;
  @Inject private NotificationService notificationService;
  @Inject private PipelineService pipelineService;
  @Inject private RoleService roleService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private SettingsService settingsService;
  @Inject private SetupService setupService;
  @Inject private StatisticsService statisticsService;
  @Inject private TriggerService triggerService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private WorkflowService workflowService;
  @Inject private YamlChangeSetHelper yamlChangeSetHelper;
  @Inject private YamlDirectoryService yamlDirectoryService;

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.AppService#save(software.wings.beans.Application)
   */
  @Override
  public Application save(Application app) {
    Validator.notNullCheck("accountId", app.getAccountId());
    Application application =
        Validator.duplicateCheck(() -> wingsPersistence.saveAndGet(Application.class, app), "name", app.getName());
    createDefaultRoles(app);
    settingsService.createDefaultApplicationSettings(application.getUuid(), application.getAccountId());
    notificationService.sendNotificationAsync(
        anInformationNotification()
            .withAppId(application.getUuid())
            .withAccountId(application.getAccountId())
            .withNotificationTemplateId(NotificationMessageType.ENTITY_CREATE_NOTIFICATION.name())
            .withNotificationTemplateVariables(
                ImmutableMap.of("ENTITY_TYPE", "Application", "ENTITY_NAME", application.getName()))
            .build());
    addCronForStateMachineExecutionCleanup(application);
    addCronForContainerSync(application);

    yamlChangeSetHelper.applicationYamlChangeAsync(application, ChangeType.ADD);

    return get(application.getUuid(), INCOMPLETE, true, 0);
  }

  List<Role> createDefaultRoles(Application app) {
    return Lists.newArrayList(roleService.save(aRole()
                                                   .withAppId(Base.GLOBAL_APP_ID)
                                                   .withAccountId(app.getAccountId())
                                                   .withName(APPLICATION_ADMIN.getDisplayName())
                                                   .withRoleType(APPLICATION_ADMIN)
                                                   .withAllApps(false)
                                                   .withAppId(app.getUuid())
                                                   .withAppName(app.getName())
                                                   .build()),
        roleService.save(aRole()
                             .withAppId(Base.GLOBAL_APP_ID)
                             .withAccountId(app.getAccountId())
                             .withName(PROD_SUPPORT.getDisplayName())
                             .withRoleType(PROD_SUPPORT)
                             .withAllApps(false)
                             .withAppId(app.getUuid())
                             .withAppName(app.getName())
                             .build()),
        roleService.save(aRole()
                             .withAppId(Base.GLOBAL_APP_ID)
                             .withAccountId(app.getAccountId())
                             .withName(NON_PROD_SUPPORT.getDisplayName())
                             .withRoleType(NON_PROD_SUPPORT)
                             .withAllApps(false)
                             .withAppId(app.getUuid())
                             .withAppName(app.getName())
                             .build()));
  }

  void addCronForStateMachineExecutionCleanup(Application application) {
    JobDetail job = JobBuilder.newJob(StateMachineExecutionCleanupJob.class)
                        .withIdentity(application.getUuid(), StateMachineExecutionCleanupJob.GROUP)
                        .usingJobData("appId", application.getUuid())
                        .build();

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(application.getUuid(), StateMachineExecutionCleanupJob.GROUP)
            .withSchedule(
                SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(SM_CLEANUP_POLL_INTERVAL).repeatForever())
            .build();

    jobScheduler.scheduleJob(job, trigger);
  }

  void addCronForContainerSync(Application application) {
    JobDetail job = JobBuilder.newJob(ContainerSyncJob.class)
                        .withIdentity(application.getUuid(), ContainerSyncJob.GROUP)
                        .usingJobData("appId", application.getUuid())
                        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(application.getUuid(), ContainerSyncJob.GROUP)
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                            .withIntervalInSeconds(INSTANCE_SYNC_POLL_INTERVAL)
                                            .repeatForever())
                          .build();

    jobScheduler.scheduleJob(job, trigger);
  }

  void addCronForPruningDescendantObjects(String appId) {
    // If somehow this job was scheduled from before, we would like to reset it to start counting from now.
    jobScheduler.deleteJob(appId, PruneObjectJob.GROUP);

    JobDetail details = JobBuilder.newJob(PruneObjectJob.class)
                            .withIdentity(appId, PruneObjectJob.GROUP)
                            .usingJobData(PruneObjectJob.OBJECT_CLASS_KEY, Application.class.getCanonicalName())
                            .usingJobData(PruneObjectJob.APP_ID_KEY, appId)
                            .usingJobData(PruneObjectJob.OBJECT_ID_KEY, appId)
                            .build();

    Trigger trigger = PruneObjectJob.defaultTrigger(appId);

    jobScheduler.scheduleJob(details, trigger);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.AppService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<Application> list(PageRequest<Application> req) {
    return list(req, false, 0, 0);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.AppService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<Application> list(
      PageRequest<Application> req, boolean overview, int numberOfExecutions, int overviewDays) {
    PageResponse<Application> response = wingsPersistence.query(Application.class, req);

    if (overview) { // TODO: merge both overview block make service/env population part of overview option
      Map<String, AppKeyStatistics> applicationKeyStats = statisticsService.getApplicationKeyStats(
          response.stream().map(Application::getUuid).collect(Collectors.toList()), overviewDays);
      response.forEach(application -> {
        application.setAppKeyStatistics(
            applicationKeyStats.computeIfAbsent(application.getUuid(), s -> new AppKeyStatistics()));
      });
    }
    response.getResponse().parallelStream().forEach(application -> {
      try {
        application.setEnvironments(environmentService.getEnvByApp(application.getUuid()));
      } catch (Exception e) {
        logger.error("Failed to fetch environments for app {} ", application, e);
      }
      try {
        application.setServices(serviceResourceService.findServicesByApp(application.getUuid()));
      } catch (Exception e) {
        logger.error("Failed to fetch services for app {} ", application, e);
      }
      if (overview) {
        try {
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
        } catch (Exception e) {
          logger.error("Failed to fetch recent executions for app {} ", application, e);
        }
        try {
          application.setNotifications(getIncompleteActionableApplicationNotifications(application.getUuid()));
        } catch (Exception e) {
          logger.error("Failed to fetch notifications for app {} ", application, e);
        }
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
      throw new WingsException(INVALID_ARGUMENT, "args", "Application -" + uuid + " doesn't exist");
    }
    return application;
  }

  @Override
  public Application getAppByName(String accountId, String appName) {
    return wingsPersistence.createQuery(Application.class)
        .field("accountId")
        .equal(accountId)
        .field("name")
        .equal(appName)
        .get();
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.AppService#update(software.wings.beans.Application)
   */
  @Override
  public Application update(Application app) {
    Application savedApp = get(app.getUuid());
    Query<Application> query = wingsPersistence.createQuery(Application.class).field(ID_KEY).equal(app.getUuid());
    UpdateOperations<Application> operations = wingsPersistence.createUpdateOperations(Application.class)
                                                   .set("name", app.getName())
                                                   .set("description", app.getDescription());
    wingsPersistence.update(query, operations);
    Application updatedApp = get(app.getUuid());

    yamlChangeSetHelper.applicationUpdateYamlChangeAsync(savedApp, updatedApp);
    return updatedApp;
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

    // YAML is identified by name that can be reused after deletion. Pruning yaml eventual consistent
    // may result in deleting object from a new application created after the first one was deleted,
    // or preexisting being renamed to the vacated name. This is why we have to do this synchronously.
    application.setEntityYamlPath(yamlDirectoryService.getRootPathByApp(application));
    yamlChangeSetHelper.applicationYamlChange(application, ChangeType.DELETE);

    // First lets make sure that we have persisted a job that will prone the descendant objects
    addCronForPruningDescendantObjects(appId);

    // Do not add too much between these too calls (on top and bottom). We need to persist the job
    // before we delete the object to avoid leaving the objects unpruned in case of crash. Waiting
    // too much though will result in start the job before the object is deleted, this possibility is
    // handled, but this is still not good.

    // Now we are ready to delete the object.
    if (wingsPersistence.delete(Application.class, appId)) {
      notificationService.sendNotificationAsync(
          anInformationNotification()
              .withAppId(application.getUuid())
              .withAccountId(application.getAccountId())
              .withNotificationTemplateId(NotificationMessageType.ENTITY_DELETE_NOTIFICATION.name())
              .withNotificationTemplateVariables(
                  ImmutableMap.of("ENTITY_TYPE", "Application", "ENTITY_NAME", application.getName()))
              .build());
    }

    // Note that if we failed to delete the object we left without the yaml. Likely the users
    // will not reconsider and start using the object as they never intended to delete it, but
    // probably they will retry. This is why there is no reason for us to regenerate it at this
    // point. We should have the necessary APIs elsewhere, if we find the users want it.
  }

  @Override
  public void pruneDescendingObjects(String appId) {
    List<ResponseMessage> messages = new ArrayList<>();

    for (Field field : AppServiceImpl.class.getDeclaredFields()) {
      Object obj;
      try {
        obj = field.get(this);
      } catch (IllegalAccessException e) {
        continue;
      }

      if (obj instanceof OwnedByApplication) {
        try {
          OwnedByApplication descending = (OwnedByApplication) obj;
          descending.pruneByApplication(appId);
        } catch (WingsException e) {
          messages.addAll(e.getResponseMessageList());
        } catch (RuntimeException e) {
          messages.add(aResponseMessage().withCode(UNKNOWN_ERROR).withMessage(e.getMessage()).build());
        }
      }
    }

    if (!messages.isEmpty()) {
      throw new WingsException(messages, "Fail to prune some of the objects for app: " + appId, (Throwable) null);
    }
  }

  @Override
  public List<Application> getAppsByAccountId(String accountId) {
    List<Application> appList = new ArrayList<>();
    appList = wingsPersistence.createQuery(Application.class).field("accountId").equal(accountId).asList();
    return appList;
  }

  @Override
  public List<String> getAppIdsByAccountId(String accountId) {
    List<String> appIdList = new ArrayList<>();
    wingsPersistence.createQuery(Application.class)
        .field("accountId")
        .equal(accountId)
        .asKeyList()
        .forEach(applicationKey -> appIdList.add(applicationKey.getId().toString()));
    return appIdList;
  }

  @Override
  public List<String> getAppNamesByAccountId(String accountId) {
    List<String> appIdList = new ArrayList<>();
    wingsPersistence.createQuery(Application.class)
        .field("accountId")
        .equal(accountId)
        .asList()
        .forEach(application -> appIdList.add(application.getName().toString()));
    return appIdList;
  }

  @Override
  public void deleteByAccountId(String accountId) {
    wingsPersistence.createQuery(SettingAttribute.class)
        .field("accountId")
        .equal(accountId)
        .asKeyList()
        .forEach(key -> delete(key.getId().toString()));
  }

  void deleteCronForStateMachineExecutionCleanup(String appId) {
    jobScheduler.deleteJob(appId, StateMachineExecutionCleanupJob.GROUP);
  }

  @Override
  public Application get(String appId, SetupStatus status, boolean overview, int overviewDays) {
    Application application = get(appId);
    application.setEnvironments(environmentService.getEnvByApp(application.getUuid()));
    application.setServices(serviceResourceService.findServicesByApp(application.getUuid()));

    if (overview) {
      application.setNotifications(getIncompleteActionableApplicationNotifications(appId));
      application.setAppKeyStatistics(
          statisticsService.getApplicationKeyStats(Arrays.asList(appId), overviewDays).get(appId));
    }

    if (status == INCOMPLETE) {
      application.setSetup(setupService.getApplicationSetupStatus(application));
    }
    return application;
  }

  @Override
  public String getAccountIdByAppId(String appId) {
    if (appId == null || appId.isEmpty()) {
      return null;
    }

    Application app = get(appId);

    if (app == null) {
      return null;
    }

    return app.getAccountId();
  }
}

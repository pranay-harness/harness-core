package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
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

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.Notification;
import software.wings.beans.Role;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Setup.SetupStatus;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.stats.AppKeyStatistics;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.scheduler.JobScheduler;
import software.wings.scheduler.StateMachineExecutionCleanupJob;
import software.wings.service.intfc.AppContainerService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.RoleService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SetupService;
import software.wings.service.intfc.StatisticsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.utils.Validator;

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
  private static final String SM_CLEANUP_CRON_GROUP = "SM_CLEANUP_CRON_GROUP";
  private static final int SM_CLEANUP_POLL_INTERVAL = 60;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private SettingsService settingsService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private EnvironmentService environmentService;
  @Inject private AppContainerService appContainerService;
  @Inject private ExecutorService executorService;
  @Inject private SetupService setupService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private NotificationService notificationService;
  @Inject private WorkflowService workflowService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private ArtifactService artifactService;
  @Inject private StatisticsService statisticsService;
  @Inject private RoleService roleService;
  @Inject private JobScheduler jobScheduler;
  @Inject private PipelineService pipelineService;

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
    environmentService.createDefaultEnvironments(application.getUuid());
    notificationService.sendNotificationAsync(
        anInformationNotification()
            .withAppId(application.getUuid())
            .withAccountId(application.getAccountId())
            .withNotificationTemplateId(NotificationMessageType.ENTITY_CREATE_NOTIFICATION.name())
            .withNotificationTemplateVariables(
                ImmutableMap.of("ENTITY_TYPE", "Application", "ENTITY_NAME", application.getName()))
            .build());
    addCronForStateMachineExecutionCleanup(application);
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
                        .withIdentity(application.getUuid(), SM_CLEANUP_CRON_GROUP)
                        .usingJobData("appId", application.getUuid())
                        .build();

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(application.getUuid(), SM_CLEANUP_CRON_GROUP)
            .withSchedule(
                SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(SM_CLEANUP_POLL_INTERVAL).repeatForever())
            .build();

    jobScheduler.scheduleJob(job, trigger);
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
      response.forEach(application
          -> application.setAppKeyStatistics(
              applicationKeyStats.computeIfAbsent(application.getUuid(), s -> new AppKeyStatistics())));
    }
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
        notificationService.deleteByApplication(appId);
        notificationService.sendNotificationAsync(
            anInformationNotification()
                .withAppId(application.getUuid())
                .withAccountId(application.getAccountId())
                .withNotificationTemplateId(NotificationMessageType.ENTITY_DELETE_NOTIFICATION.name())
                .withNotificationTemplateVariables(
                    ImmutableMap.of("ENTITY_TYPE", "Application", "ENTITY_NAME", application.getName()))
                .build());

        environmentService.deleteByApp(appId);
        artifactStreamService.deleteByApplication(appId);
        artifactService.deleteByApplication(appId);
        workflowService.deleteWorkflowByApplication(appId);
        workflowService.deleteStateMachinesByApplication(appId);
        pipelineService.deletePipelineByApplication(appId);
        serviceResourceService.deleteByApp(appId);
        notificationService.sendNotificationAsync(
            anInformationNotification()
                .withAppId(application.getUuid())
                .withNotificationTemplateId(NotificationMessageType.ENTITY_DELETE_NOTIFICATION.name())
                .withNotificationTemplateVariables(
                    ImmutableMap.of("ENTITY_TYPE", "Application", "ENTITY_NAME", application.getName()))
                .build());
      });
      deleteCronForStateMachineExecutionCleanup(appId);
    }
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
    jobScheduler.deleteJob(appId, SM_CLEANUP_CRON_GROUP);
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
}

package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.ListUtil.trimList;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.Role.Builder.aRole;
import static software.wings.beans.RoleType.APPLICATION_ADMIN;
import static software.wings.beans.RoleType.NON_PROD_SUPPORT;
import static software.wings.beans.RoleType.PROD_SUPPORT;
import static software.wings.beans.Setup.SetupStatus.INCOMPLETE;
import static software.wings.dl.HQuery.excludeAuthority;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.utils.Validator.duplicateCheck;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.data.validator.EntityNameValidator;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.Notification;
import software.wings.beans.Role;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Setup.SetupStatus;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.StringValue;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.stats.AppKeyStatistics;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.scheduler.InstanceSyncJob;
import software.wings.scheduler.PruneEntityJob;
import software.wings.scheduler.QuartzScheduler;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.impl.yaml.YamlChangeSetHelper;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.NotificationService;
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
import software.wings.service.intfc.ownership.OwnedByApplication;
import software.wings.service.intfc.yaml.YamlDirectoryService;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
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

  @Inject private AlertService alertService;
  @Inject private ArtifactService artifactService;
  @Inject private AuthHandler authHandler;
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

  private void validateAppName(Application app) {
    if (app != null) {
      if (!EntityNameValidator.isValid(app.getName())) {
        throw new InvalidRequestException("App Name can only have characters -, _, a-z, A-Z, 0-9 and space");
      }
    }
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.AppService#save(software.wings.beans.Application)
   */
  @Override
  public Application save(Application app) {
    notNullCheck("accountId", app.getAccountId());
    validateAppName(app);
    app.setKeywords(trimList(app.generateKeywords()));
    Application application =
        duplicateCheck(() -> wingsPersistence.saveAndGet(Application.class, app), "name", app.getName());
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
    InstanceSyncJob.add(jobScheduler, application.getUuid());

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

    List<Application> applicationList = response.getResponse();
    List<String> appIdList =
        applicationList.parallelStream().map(application -> application.getUuid()).collect(toList());

    if (overview) { // TODO: merge both overview block make service/env population part of overview option
      Map<String, AppKeyStatistics> applicationKeyStats =
          statisticsService.getApplicationKeyStats(appIdList, overviewDays);
      applicationList.forEach(application
          -> application.setAppKeyStatistics(
              applicationKeyStats.computeIfAbsent(application.getUuid(), s -> new AppKeyStatistics())));
    }

    PermissionAttribute svcPermissionAttribute = new PermissionAttribute(PermissionType.SERVICE, Action.READ);
    authHandler.setEntityIdFilterIfUserAction(asList(svcPermissionAttribute), appIdList);

    PermissionAttribute envPermissionAttribute = new PermissionAttribute(PermissionType.ENV, Action.READ);
    authHandler.setEntityIdFilterIfUserAction(asList(envPermissionAttribute), appIdList);

    // Had to change the parallel stream to normal stream since we want
    applicationList.stream().forEach(application -> {
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
                  .listExecutions(aPageRequest()
                                      .withLimit(Integer.toString(numberOfExecutions))
                                      .addFilter(WorkflowExecution.APP_ID_KEY, Operator.EQ, application.getUuid())
                                      .addOrder("createdAt", OrderType.DESC)
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
    return wingsPersistence.createQuery(Application.class, excludeAuthority).filter(ID_KEY, appId).getKey() != null;
  }

  private List<Notification> getIncompleteActionableApplicationNotifications(String appId) {
    return notificationService
        .list(aPageRequest()
                  .addFilter(Notification.APP_ID_KEY, Operator.EQ, appId)
                  .addFilter("complete", Operator.EQ, false)
                  .addFilter("actionable", Operator.EQ, true)
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
      throw new WingsException(INVALID_ARGUMENT).addParam("args", "Application " + uuid + " doesn't exist");
    }
    return application;
  }

  @Override
  public Application getApplicationWithDefaults(String uuid) {
    Application application = get(uuid);

    List<SettingAttribute> settingAttributes =
        settingsService.listApplicationDefaults(application.getAccountId(), application.getUuid());
    application.setDefaults(
        settingAttributes.stream()
            .filter(settingAttribute -> settingAttribute.getValue() instanceof StringValue)
            .collect(Collectors.toMap(SettingAttribute::getName,
                settingAttribute -> ((StringValue) settingAttribute.getValue()).getValue(), (a, b) -> b)));
    return application;
  }

  @Override
  public Application getAppByName(String accountId, String appName) {
    return wingsPersistence.createQuery(Application.class).filter("accountId", accountId).filter("name", appName).get();
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.AppService#update(software.wings.beans.Application)
   */
  @Override
  public Application update(Application app) {
    Application savedApp = get(app.getUuid());
    Query<Application> query = wingsPersistence.createQuery(Application.class).filter(ID_KEY, app.getUuid());
    UpdateOperations<Application> operations =
        wingsPersistence.createUpdateOperations(Application.class).set("name", app.getName());

    List<String> keywords = trimList(app.generateKeywords());

    if (isNotEmpty(app.getDescription())) {
      operations.set("description", app.getDescription()).set("keywords", keywords);
    }

    wingsPersistence.update(query, operations);
    Application updatedApp = get(app.getUuid());

    yamlChangeSetHelper.applicationUpdateYamlChangeAsync(savedApp, updatedApp);
    return updatedApp;
  }

  @Override
  public void delete(String appId) {
    Application application = wingsPersistence.get(Application.class, appId);
    if (application == null) {
      return;
    }

    // YAML is identified by name that can be reused after deletion. Pruning yaml eventual consistent
    // may result in deleting object from a new application created after the first one was deleted,
    // or preexisting being renamed to the vacated name. This is why we have to do this synchronously.
    application.setEntityYamlPath(yamlDirectoryService.getRootPathByApp(application));
    yamlChangeSetHelper.applicationYamlChange(application, ChangeType.DELETE);

    // First lets make sure that we have persisted a job that will prone the descendant objects
    PruneEntityJob.addDefaultJob(jobScheduler, Application.class, appId, appId, Duration.ofSeconds(5));

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
  public void delete(String appId, String entityId) {
    delete(entityId);
  }

  @Override
  public void pruneDescendingEntities(@NotEmpty String appId) {
    List<OwnedByApplication> services =
        ServiceClassLocator.descendingServices(this, AppServiceImpl.class, OwnedByApplication.class);
    PruneEntityJob.pruneDescendingEntities(services, descending -> descending.pruneByApplication(appId));
  }

  @Override
  public List<Application> getAppsByAccountId(String accountId) {
    return wingsPersistence.createQuery(Application.class).filter("accountId", accountId).asList();
  }

  @Override
  public List<String> getAppIdsByAccountId(String accountId) {
    return wingsPersistence.createQuery(Application.class)
        .filter("accountId", accountId)
        .asKeyList()
        .stream()
        .map(applicationKey -> applicationKey.getId().toString())
        .collect(toList());
  }

  @Override
  public List<String> getAppNamesByAccountId(String accountId) {
    return wingsPersistence.createQuery(Application.class)
        .project(Application.NAME_KEY, true)
        .filter("accountId", accountId)
        .asList()
        .stream()
        .map(Application::getName)
        .collect(toList());
  }

  @Override
  public void deleteByAccountId(String accountId) {
    wingsPersistence.createQuery(SettingAttribute.class)
        .filter(SettingAttribute.ACCOUNT_ID_KEY, accountId)
        .asKeyList()
        .forEach(key -> delete(key.getId().toString()));
  }

  @Override
  public Application get(String appId, SetupStatus status, boolean overview, int overviewDays) {
    Application application = get(appId);

    List<String> appIdAsList = asList(appId);
    PermissionAttribute svcPermissionAttribute = new PermissionAttribute(PermissionType.SERVICE, Action.READ);
    authHandler.setEntityIdFilterIfUserAction(asList(svcPermissionAttribute), appIdAsList);

    PermissionAttribute envPermissionAttribute = new PermissionAttribute(PermissionType.ENV, Action.READ);
    authHandler.setEntityIdFilterIfUserAction(asList(envPermissionAttribute), appIdAsList);

    application.setEnvironments(environmentService.getEnvByApp(application.getUuid()));
    application.setServices(serviceResourceService.findServicesByApp(application.getUuid()));

    if (overview) {
      application.setNotifications(getIncompleteActionableApplicationNotifications(appId));
      application.setAppKeyStatistics(statisticsService.getApplicationKeyStats(asList(appId), overviewDays).get(appId));
    }

    if (status == INCOMPLETE) {
      application.setSetup(setupService.getApplicationSetupStatus(application));
    }
    return application;
  }

  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
  @Override
  public String getAccountIdByAppId(String appId) {
    if (isEmpty(appId)) {
      return null;
    }

    Application app = get(appId);

    if (app == null) {
      return null;
    }

    return app.getAccountId();
  }
}

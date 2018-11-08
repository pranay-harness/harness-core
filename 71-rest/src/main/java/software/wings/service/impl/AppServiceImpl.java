package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.ListUtils.trimList;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.Role.Builder.aRole;
import static software.wings.beans.RoleType.APPLICATION_ADMIN;
import static software.wings.beans.RoleType.NON_PROD_SUPPORT;
import static software.wings.beans.RoleType.PROD_SUPPORT;
import static software.wings.utils.Validator.duplicateCheck;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.data.validator.EntityNameValidator;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.limits.ActionType;
import io.harness.limits.LimitCheckerFactory;
import io.harness.limits.LimitEnforcementUtils;
import io.harness.limits.checker.StaticLimitCheckerWithDecrement;
import io.harness.scheduler.PersistentScheduler;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.EntityType;
import software.wings.beans.Event.Type;
import software.wings.beans.Role;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.dl.GenericDbCache;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.InstanceSyncJob;
import software.wings.scheduler.PruneEntityJob;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.RoleService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.StatisticsService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.service.intfc.ownership.OwnedByApplication;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.List;
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
  @Inject private InstanceService instanceService;
  @Inject private NotificationService notificationService;
  @Inject private PipelineService pipelineService;
  @Inject private RoleService roleService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private SettingsService settingsService;
  @Inject private StatisticsService statisticsService;
  @Inject private TriggerService triggerService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private WorkflowService workflowService;
  @Inject private YamlPushService yamlPushService;
  @Inject private GenericDbCache dbCache;
  @Inject private LimitCheckerFactory limitCheckerFactory;
  @Inject private YamlGitService yamlGitService;

  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler backgroundJobScheduler;
  @Inject @Named("ServiceJobScheduler") private PersistentScheduler serviceJobScheduler;

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
    StaticLimitCheckerWithDecrement checker = (StaticLimitCheckerWithDecrement) limitCheckerFactory.getInstance(
        new io.harness.limits.Action(app.getAccountId(), ActionType.CREATE_APPLICATION));

    return LimitEnforcementUtils.withLimitCheck(checker, () -> {
      validateAppName(app);
      app.setKeywords(trimList(app.generateKeywords()));

      Application application =
          duplicateCheck(() -> wingsPersistence.saveAndGet(Application.class, app), "name", app.getName());
      createDefaultRoles(app);
      settingsService.createDefaultApplicationSettings(
          application.getUuid(), application.getAccountId(), app.isSyncFromGit());
      sendNotification(application, NotificationMessageType.ENTITY_CREATE_NOTIFICATION);
      InstanceSyncJob.add(serviceJobScheduler, application.getUuid());

      // Save the Git Configuration for application if not null
      YamlGitConfig yamlGitConfig = app.getYamlGitConfig();
      if (yamlGitConfig != null) {
        setAppYamlGitConfigDefaults(application.getAccountId(), application.getUuid(), yamlGitConfig);

        // We are disabling git fullsync when the app is created on git side. The reason we have to to do this is that
        // the fullsync for an app deletes the original app folder and creates new one. If the app is created on git
        // side, then we dont need fullsync again.
        yamlGitService.save(yamlGitConfig, !app.isSyncFromGit());
      }

      yamlPushService.pushYamlChangeSet(app.getAccountId(), null, application, Type.CREATE, app.isSyncFromGit(), false);

      return get(application.getUuid());
    });
  }

  private void sendNotification(Application application, NotificationMessageType entityCreateNotification) {
    notificationService.sendNotificationAsync(anInformationNotification()
                                                  .withAppId(application.getUuid())
                                                  .withAccountId(application.getAccountId())
                                                  .withNotificationTemplateId(entityCreateNotification.name())
                                                  .withNotificationTemplateVariables(ImmutableMap.of("ENTITY_TYPE",
                                                      "Application", "ENTITY_NAME", application.getName()))
                                                  .build());
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
  public PageResponse<Application> list(PageRequest<Application> req, boolean details) {
    PageResponse<Application> response = wingsPersistence.query(Application.class, req);

    List<Application> applicationList = response.getResponse();
    if (isEmpty(applicationList)) {
      return response;
    }
    List<String> appIdList = applicationList.stream().map(application -> application.getUuid()).collect(toList());

    PermissionAttribute svcPermissionAttribute = new PermissionAttribute(PermissionType.SERVICE, Action.READ);
    authHandler.setEntityIdFilterIfUserAction(asList(svcPermissionAttribute), appIdList);

    PermissionAttribute envPermissionAttribute = new PermissionAttribute(PermissionType.ENV, Action.READ);
    authHandler.setEntityIdFilterIfUserAction(asList(envPermissionAttribute), appIdList);

    applicationList.forEach(application -> {
      // We cannot assume that the application always have accountId and applicationId
      // Adding null checks to take care of that
      if (isNotEmpty(application.getAccountId()) && isNotEmpty(application.getUuid())) {
        YamlGitConfig yamlGitConfig =
            yamlGitService.get(application.getAccountId(), application.getUuid(), EntityType.APPLICATION);
        application.setYamlGitConfig(yamlGitConfig);
      }
    });

    if (details) {
      applicationList.forEach(application -> {
        try {
          application.setEnvironments(environmentService.getEnvByApp(application.getUuid()));
        } catch (Exception e) {
          logger.error(format("Failed to fetch environments for app %s", application), e);
        }
        try {
          application.setServices(serviceResourceService.findServicesByApp(application.getUuid()));
        } catch (Exception e) {
          logger.error(format("Failed to fetch services for app %s", application), e);
        }
      });
    }
    return response;
  }

  @Override
  public PageResponse<Application> list(PageRequest<Application> req) {
    return list(req, false);
  }

  @Override
  public boolean exist(String appId) {
    return wingsPersistence.createQuery(Application.class, excludeAuthority).filter(ID_KEY, appId).getKey() != null;
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

    application.setYamlGitConfig(
        yamlGitService.get(application.getAccountId(), application.getUuid(), EntityType.APPLICATION));

    return application;
  }

  @Override
  public Application getApplicationWithDefaults(String uuid) {
    Application application = get(uuid);
    application.setDefaults(settingsService.listAppDefaults(application.getAccountId(), uuid));

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
    validateAppName(app);
    Application savedApp = get(app.getUuid());
    Query<Application> query = wingsPersistence.createQuery(Application.class).filter(ID_KEY, app.getUuid());
    List<String> keywords = trimList(app.generateKeywords());

    UpdateOperations<Application> operations =
        wingsPersistence.createUpdateOperations(Application.class).set("name", app.getName()).set("keywords", keywords);

    setUnset(operations, "description", app.getDescription());

    wingsPersistence.update(query, operations);
    dbCache.invalidate(Application.class, app.getUuid());

    updateAppYamlGitConfig(savedApp, app, !app.isSyncFromGit());

    Application updatedApp = get(app.getUuid());

    boolean isRename = !savedApp.getName().equals(app.getName());
    yamlPushService.pushYamlChangeSet(
        app.getAccountId(), savedApp, updatedApp, Type.UPDATE, app.isSyncFromGit(), isRename);

    return updatedApp;
  }

  @Override
  public void delete(String appId, boolean syncFromGit) {
    Application application = wingsPersistence.get(Application.class, appId);
    if (application == null) {
      return;
    }

    String accountId = this.getAccountIdByAppId(appId);
    StaticLimitCheckerWithDecrement checker = (StaticLimitCheckerWithDecrement) limitCheckerFactory.getInstance(
        new io.harness.limits.Action(accountId, ActionType.CREATE_APPLICATION));

    LimitEnforcementUtils.withCounterDecrement(checker, () -> {
      dbCache.invalidate(Application.class, appId);

      yamlPushService.pushYamlChangeSet(application.getAccountId(), application, null, Type.DELETE, syncFromGit, false);

      // First lets make sure that we have persisted a job that will prone the descendant objects
      PruneEntityJob.addDefaultJob(
          backgroundJobScheduler, Application.class, appId, appId, ofSeconds(5), ofSeconds(15));

      // Do not add too much between these too calls (on top and bottom). We need to persist the job
      // before we delete the object to avoid leaving the objects unpruned in case of crash. Waiting
      // too much though will result in start the job before the object is deleted, this possibility is
      // handled, but this is still not good.

      // Now we are ready to delete the object.
      if (wingsPersistence.delete(Application.class, appId)) {
        sendNotification(application, NotificationMessageType.ENTITY_DELETE_NOTIFICATION);
      }

      // Note that if we failed to delete the object we left without the yaml. Likely the users
      // will not reconsider and start using the object as they never intended to delete it, but
      // probably they will retry. This is why there is no reason for us to regenerate it at this
      // point. We should have the necessary APIs elsewhere, if we find the users want it.
      return true;
    });
  }

  @Override
  public void delete(String appId, String entityId) {
    delete(entityId, false);
  }

  @Override
  public void delete(String appId) {
    delete(appId, false);
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
    wingsPersistence.createQuery(Application.class)
        .filter(Application.ACCOUNT_ID_KEY, accountId)
        .asKeyList()
        .forEach(key -> delete(key.getId().toString()));
  }

  @Override
  public Application get(String appId, boolean details) {
    Application application = get(appId);

    if (details) {
      List<String> appIdAsList = asList(appId);
      PermissionAttribute svcPermissionAttribute = new PermissionAttribute(PermissionType.SERVICE, Action.READ);
      authHandler.setEntityIdFilterIfUserAction(asList(svcPermissionAttribute), appIdAsList);

      PermissionAttribute envPermissionAttribute = new PermissionAttribute(PermissionType.ENV, Action.READ);
      authHandler.setEntityIdFilterIfUserAction(asList(envPermissionAttribute), appIdAsList);

      application.setEnvironments(environmentService.getEnvByApp(application.getUuid()));
      application.setServices(serviceResourceService.findServicesByApp(application.getUuid()));
    }
    return application;
  }

  @Override
  public String getAccountIdByAppId(String appId) {
    if (isEmpty(appId)) {
      return null;
    }

    Application app = get(appId);

    return app.getAccountId();
  }

  private void updateAppYamlGitConfig(Application savedApp, Application app, boolean performFullSync) {
    YamlGitConfig savedYamlGitConfig = savedApp.getYamlGitConfig();

    YamlGitConfig yamlGitConfig = app.getYamlGitConfig();

    if (savedYamlGitConfig != null) {
      if (yamlGitConfig != null) {
        // Forcing the yamlGitConfig to have correct values set.
        setAppYamlGitConfigDefaults(savedApp.getAccountId(), savedApp.getUuid(), yamlGitConfig);
        // Its an update operation, but we don't want it to do full sync. That's we are directly calling the save with
        // fullsync disabled
        yamlGitService.save(yamlGitConfig, performFullSync);
      } else {
        yamlGitService.delete(savedApp.getAccountId(), savedApp.getUuid(), EntityType.APPLICATION);
      }
    } else {
      if (yamlGitConfig != null) {
        // Forcing the yamlGitConfig to have correct values set.
        setAppYamlGitConfigDefaults(savedApp.getAccountId(), savedApp.getUuid(), yamlGitConfig);
        yamlGitService.save(yamlGitConfig, performFullSync);
      }
    }
  }

  private void setAppYamlGitConfigDefaults(String accountId, String appId, YamlGitConfig yamlGitConfig) {
    yamlGitConfig.setAccountId(accountId);
    yamlGitConfig.setEntityId(appId);
    yamlGitConfig.setAppId(appId);
    yamlGitConfig.setEntityType(EntityType.APPLICATION);
  }
}

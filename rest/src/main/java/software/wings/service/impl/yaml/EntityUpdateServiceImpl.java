package software.wings.service.impl.yaml;

import static software.wings.beans.Base.GLOBAL_APP_ID;

import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.ServiceCommand;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.yaml.AppYamlResourceService;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.ServiceYamlResourceService;
import software.wings.service.intfc.yaml.SetupYamlResourceService;
import software.wings.service.intfc.yaml.YamlGitSyncService;
import software.wings.service.intfc.yaml.YamlResourceService;
import software.wings.yaml.YamlHelper;
import software.wings.yaml.gitSync.EntityUpdateEvent;
import software.wings.yaml.gitSync.EntityUpdateEvent.SourceType;
import software.wings.yaml.gitSync.EntityUpdateListEvent;
import software.wings.yaml.gitSync.GitSyncHelper;
import software.wings.yaml.gitSync.YamlGitSync;

import javax.inject.Inject;

/**
 * Entity Update Service Implementation.
 *
 * @author bsollish
 */
public class EntityUpdateServiceImpl implements EntityUpdateService {
  @Inject private YamlGitSyncService yamlGitSyncService;
  @Inject private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private AppYamlResourceService appYamlResourceService;
  @Inject private ServiceYamlResourceService serviceYamlResourceService;
  @Inject private YamlResourceService yamlResourceService;
  @Inject private SetupYamlResourceService setupYamlResourceService;

  @Inject private GitSyncHelper gitSyncHelper;

  // this method has to pass through to the gitSyncHelper to prevent a Guice "loop"
  public void queueEntityUpdateList(EntityUpdateListEvent entityUpdateListEvent) {
    gitSyncHelper.queueEntityUpdateList(entityUpdateListEvent);
  }

  public EntityUpdateEvent createEntityUpdateEvent(
      String entityId, String name, String accountId, String appId, Class klass, String yaml, SourceType sourceType) {
    // create an entity update event
    return EntityUpdateEvent.Builder.anEntityUpdateEvent()
        .withEntityId(entityId)
        .withName(name)
        .withAccountId(accountId)
        .withAppId(appId)
        .withClass(klass)
        .withSourceType(sourceType)
        .withYaml(yaml)
        .build();
  }

  public EntityUpdateEvent setupListUpdate(Account account, SourceType sourceType) {
    if (account == null) {
      // TODO - handle missing app
      return null;
    }

    String appId = GLOBAL_APP_ID;
    String accountId = account.getUuid();

    YamlGitSync ygs = yamlGitSyncService.get(appId, accountId, appId);

    // is it synced
    if (ygs != null) {
      // is it enabled
      if (ygs.isEnabled()) {
        String yaml = setupYamlResourceService.getSetup(accountId).getResource().getYaml();
        yaml = YamlHelper.cleanupYaml(yaml);
        return createEntityUpdateEvent(account.getUuid(), "setup", accountId, appId, Account.class, yaml, sourceType);
      }
    }

    return null;
  }

  public EntityUpdateEvent appListUpdate(Application app, SourceType sourceType) {
    if (app == null) {
      // TODO - handle missing app
      return null;
    }

    String appId = app.getUuid();
    String accountId = app.getAccountId();

    YamlGitSync ygs = yamlGitSyncService.get(appId, accountId, appId);

    // is it synced
    if (ygs != null) {
      // is it enabled
      if (ygs.isEnabled()) {
        String yaml = appYamlResourceService.getApp(appId).getResource().getYaml();
        yaml = YamlHelper.cleanupYaml(yaml);
        return createEntityUpdateEvent(
            app.getUuid(), app.getName(), accountId, appId, Application.class, yaml, sourceType);
      }
    }

    return null;
  }

  public EntityUpdateEvent serviceListUpdate(Service service, SourceType sourceType) {
    if (service == null) {
      // TODO - handle missing service
      return null;
    }

    String appId = service.getAppId();
    String accountId = appService.get(appId).getAccountId();

    // this may not be the full Service object with ServiceCommand and Config Variables, etc. - so we need to get it
    // again WITH details
    service = serviceResourceService.get(appId, service.getUuid(), true);

    YamlGitSync ygs = yamlGitSyncService.get(service.getUuid(), accountId, appId);

    // is it synced
    if (ygs != null) {
      // is it enabled
      if (ygs.isEnabled()) {
        String yaml = serviceYamlResourceService.getServiceYaml(service);
        yaml = YamlHelper.cleanupYaml(yaml);
        return createEntityUpdateEvent(
            service.getUuid(), service.getName(), accountId, appId, Service.class, yaml, sourceType);
      }
    }

    return null;
  }

  public EntityUpdateEvent serviceCommandListUpdate(ServiceCommand serviceCommand, SourceType sourceType) {
    if (serviceCommand == null) {
      // TODO - handle missing command
      return null;
    }

    String appId = serviceCommand.getAppId();
    String accountId = appService.get(appId).getAccountId();

    YamlGitSync ygs = yamlGitSyncService.get(serviceCommand.getUuid(), accountId, appId);

    // is it synced
    if (ygs != null) {
      // is it enabled
      if (ygs.isEnabled()) {
        String yaml = yamlResourceService.getServiceCommand(appId, serviceCommand.getUuid()).getResource().getYaml();
        yaml = YamlHelper.cleanupYaml(yaml);
        return createEntityUpdateEvent(serviceCommand.getUuid(), serviceCommand.getName(), accountId, appId,
            ServiceCommand.class, yaml, sourceType);
      }
    }

    return null;
  }

  public EntityUpdateEvent environmentListUpdate(Environment environment, SourceType sourceType) {
    if (environment == null) {
      // TODO - handle missing environment
      return null;
    }

    String appId = environment.getAppId();
    String accountId = appService.get(appId).getAccountId();

    YamlGitSync ygs = yamlGitSyncService.get(environment.getUuid(), accountId, appId);

    // is it synced
    if (ygs != null) {
      // is it enabled
      if (ygs.isEnabled()) {
        String yaml = yamlResourceService.getEnvironment(appId, environment.getUuid()).getResource().getYaml();
        yaml = YamlHelper.cleanupYaml(yaml);
        return createEntityUpdateEvent(
            environment.getUuid(), environment.getName(), accountId, appId, Environment.class, yaml, sourceType);
      }
    }

    return null;
  }

  public EntityUpdateEvent workflowListUpdate(Workflow workflow, SourceType sourceType) {
    if (workflow == null) {
      // TODO - handle missing workflow
      return null;
    }

    String appId = workflow.getAppId();
    String accountId = appService.get(appId).getAccountId();

    YamlGitSync ygs = yamlGitSyncService.get(workflow.getUuid(), accountId, appId);

    // is it synced
    if (ygs != null) {
      // is it enabled
      if (ygs.isEnabled()) {
        String yaml = yamlResourceService.getWorkflow(appId, workflow.getUuid()).getResource().getYaml();
        yaml = YamlHelper.cleanupYaml(yaml);
        return createEntityUpdateEvent(
            workflow.getUuid(), workflow.getName(), accountId, appId, Workflow.class, yaml, sourceType);
      }
    }

    return null;
  }

  public EntityUpdateEvent pipelineListUpdate(Pipeline pipeline, SourceType sourceType) {
    if (pipeline == null) {
      // TODO - handle missing pipeline
      return null;
    }

    String appId = pipeline.getAppId();
    String accountId = appService.get(appId).getAccountId();

    YamlGitSync ygs = yamlGitSyncService.get(pipeline.getUuid(), accountId, appId);

    // is it synced
    if (ygs != null) {
      // is it enabled
      if (ygs.isEnabled()) {
        String yaml = yamlResourceService.getPipeline(appId, pipeline.getUuid()).getResource().getYaml();
        yaml = YamlHelper.cleanupYaml(yaml);
        return createEntityUpdateEvent(
            pipeline.getUuid(), pipeline.getName(), accountId, appId, Pipeline.class, yaml, sourceType);
      }
    }

    return null;
  }

  public EntityUpdateEvent triggerListUpdate(ArtifactStream artifactStream, SourceType sourceType) {
    if (artifactStream == null) {
      // TODO - handle missing artfifactStream
      return null;
    }

    String appId = artifactStream.getAppId();
    String accountId = appService.get(appId).getAccountId();

    YamlGitSync ygs = yamlGitSyncService.get(artifactStream.getUuid(), accountId, appId);

    // is it synced
    if (ygs != null) {
      // is it enabled
      if (ygs.isEnabled()) {
        String yaml = yamlResourceService.getTrigger(appId, artifactStream.getUuid()).getResource().getYaml();
        yaml = YamlHelper.cleanupYaml(yaml);
        return createEntityUpdateEvent(artifactStream.getUuid(), artifactStream.getSourceName(), accountId, appId,
            ArtifactStream.class, yaml, sourceType);
      }
    }

    return null;
  }

  public EntityUpdateEvent settingAttributeListUpdate(SettingAttribute settingAttribute, SourceType sourceType) {
    if (settingAttribute == null) {
      // TODO - handle missing settingAttribute
      return null;
    }

    String appId = settingAttribute.getAppId();
    String accountId = appService.get(appId).getAccountId();

    YamlGitSync ygs = yamlGitSyncService.get(settingAttribute.getUuid(), accountId, appId);

    // is it synced
    if (ygs != null) {
      // is it enabled
      if (ygs.isEnabled()) {
        String yaml =
            yamlResourceService.getSettingAttribute(appId, settingAttribute.getUuid()).getResource().getYaml();
        yaml = YamlHelper.cleanupYaml(yaml);
        return createEntityUpdateEvent(settingAttribute.getUuid(), settingAttribute.getName(), accountId, appId,
            SettingAttribute.class, yaml, sourceType);
      }
    }

    return null;
  }
}

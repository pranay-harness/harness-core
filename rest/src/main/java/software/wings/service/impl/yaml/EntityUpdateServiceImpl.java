package software.wings.service.impl.yaml;

import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.ServiceCommand;
import software.wings.core.queue.Queue;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.yaml.AppYamlResourceService;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.ServiceYamlResourceService;
import software.wings.service.intfc.yaml.YamlGitSyncService;
import software.wings.service.intfc.yaml.YamlResourceService;
import software.wings.yaml.YamlHelper;
import software.wings.yaml.gitSync.EntityUpdateEvent;
import software.wings.yaml.gitSync.EntityUpdateEvent.SourceType;
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

  @Inject private Queue<EntityUpdateEvent> entityUpdateEventQueue;

  public void queueEntityUpdateEvent(
      String entityId, String name, String accountId, String appId, Class klass, String yaml) {
    // queue an entity update event
    EntityUpdateEvent entityUpdateEvent = EntityUpdateEvent.Builder.anEntityUpdateEvent()
                                              .withEntityId(entityId)
                                              .withName(name)
                                              .withAccountId(accountId)
                                              .withAppId(appId)
                                              .withClass(klass)
                                              .withSourceType(SourceType.ENTITY_UPDATE)
                                              .withYaml(yaml)
                                              .build();
    entityUpdateEventQueue.send(entityUpdateEvent);
  }

  public void appUpdate(Application app) {
    if (app == null) {
      // TODO - handle missing app
      return;
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
        queueEntityUpdateEvent(app.getUuid(), app.getName(), accountId, appId, Application.class, yaml);
      }
    }
  }

  public void serviceUpdate(Service service) {
    if (service == null) {
      // TODO - handle missing service
      return;
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
        queueEntityUpdateEvent(service.getUuid(), service.getName(), accountId, appId, Service.class, yaml);
      }
    }
  }

  public void serviceCommandUpdate(ServiceCommand serviceCommand) {
    if (serviceCommand == null) {
      // TODO - handle missing command
      return;
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
        queueEntityUpdateEvent(
            serviceCommand.getUuid(), serviceCommand.getName(), accountId, appId, ServiceCommand.class, yaml);
      }
    }
  }

  public void environmentUpdate(Environment environment) {
    if (environment == null) {
      // TODO - handle missing environment
      return;
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
        queueEntityUpdateEvent(environment.getUuid(), environment.getName(), accountId, appId, Environment.class, yaml);
      }
    }
  }

  public void workflowUpdate(Workflow workflow) {
    if (workflow == null) {
      // TODO - handle missing workflow
      return;
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
        queueEntityUpdateEvent(workflow.getUuid(), workflow.getName(), accountId, appId, Workflow.class, yaml);
      }
    }
  }

  public void pipelineUpdate(Pipeline pipeline) {
    if (pipeline == null) {
      // TODO - handle missing pipeline
      return;
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
        queueEntityUpdateEvent(pipeline.getUuid(), pipeline.getName(), accountId, appId, Pipeline.class, yaml);
      }
    }
  }

  public void triggerUpdate(ArtifactStream artifactStream) {
    if (artifactStream == null) {
      // TODO - handle missing artfifactStream
      return;
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
        queueEntityUpdateEvent(
            artifactStream.getUuid(), artifactStream.getSourceName(), accountId, appId, ArtifactStream.class, yaml);
      }
    }
  }

  public void settingAttributeUpdate(SettingAttribute settingAttribute) {
    if (settingAttribute == null) {
      // TODO - handle missing settingAttribute
      return;
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
        queueEntityUpdateEvent(
            settingAttribute.getUuid(), settingAttribute.getName(), accountId, appId, SettingAttribute.class, yaml);
      }
    }
  }
}

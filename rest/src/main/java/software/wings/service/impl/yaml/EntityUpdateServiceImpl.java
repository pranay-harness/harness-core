package software.wings.service.impl.yaml;

import software.wings.beans.Service;
import software.wings.core.queue.Queue;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.ServiceYamlResourceService;
import software.wings.service.intfc.yaml.YamlGitSyncService;
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
  @Inject private ServiceYamlResourceService serviceYamlResourceService;
  @Inject private Queue<EntityUpdateEvent> entityUpdateEventQueue;

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

        // queue an entity update event
        EntityUpdateEvent entityUpdateEvent = EntityUpdateEvent.Builder.anEntityUpdateEvent()
                                                  .withEntityId(service.getUuid())
                                                  .withName(service.getName())
                                                  .withAccountId(accountId)
                                                  .withAppId(appId)
                                                  .withClass(Service.class)
                                                  .withSourceType(SourceType.ENTITY_UPDATE)
                                                  .withYaml(yaml)
                                                  .build();
        entityUpdateEventQueue.send(entityUpdateEvent);
      }
    }
  }
}

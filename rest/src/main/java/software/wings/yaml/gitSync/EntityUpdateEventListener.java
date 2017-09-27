package software.wings.yaml.gitSync;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.intfc.yaml.YamlGitSyncService;
import software.wings.utils.Validator;

/**
 *
 * @author bsollish on 09/20/17
 *
 */
// TODO - we should no longer need this class
// public class EntityUpdateEventListener extends AbstractQueueListener<EntityUpdateEvent> {
public class EntityUpdateEventListener {
  private static final Logger logger = LoggerFactory.getLogger(EntityUpdateEventListener.class);
  @Inject private YamlGitSyncService yamlGitSyncService;

  /* (non-Javadoc)
   * @see software.wings.core.queue.AbstractQueueListener#onMessage(software.wings.core.queue.Queuable)
   */
  //@Override
  protected void onMessage(EntityUpdateEvent entityUpdateEvent) throws Exception {
    try {
      Validator.notNullCheck("EntityUpdateEvent", entityUpdateEvent);
      yamlGitSyncService.handleEntityUpdateEvent(entityUpdateEvent);
    } catch (Exception ex) {
      // We have to catch all kinds of runtime exceptions, log it and move on, otherwise the queue impl keeps retrying
      // forever in case of exception
      logger.error("*********************************** Exception while processing entity update event.", ex);
    }
  }
}

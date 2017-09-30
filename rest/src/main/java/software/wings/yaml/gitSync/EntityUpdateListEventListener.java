package software.wings.yaml.gitSync;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.core.queue.AbstractQueueListener;
import software.wings.service.intfc.yaml.YamlGitSyncService;
import software.wings.utils.Validator;

/**
 *
 * @author bsollish on 09/26/17
 *
 */
public class EntityUpdateListEventListener extends AbstractQueueListener<EntityUpdateListEvent> {
  private static final Logger logger = LoggerFactory.getLogger(EntityUpdateListEventListener.class);
  @Inject private YamlGitSyncService yamlGitSyncService;

  /* (non-Javadoc)
   * @see software.wings.core.queue.AbstractQueueListener#onMessage(software.wings.core.queue.Queuable)
   */
  @Override
  protected void onMessage(EntityUpdateListEvent entityUpdateListEvent) throws Exception {
    try {
      Validator.notNullCheck("EntityUpdateListEvent", entityUpdateListEvent);
      yamlGitSyncService.handleEntityUpdateListEvent(entityUpdateListEvent);
    } catch (Exception ex) {
      // We have to catch all kinds of runtime exceptions, log it and move on, otherwise the queue impl keeps retrying
      // forever in case of exception
      logger.error("*********************************** Exception while processing entity update list event.", ex);
    }
  }
}

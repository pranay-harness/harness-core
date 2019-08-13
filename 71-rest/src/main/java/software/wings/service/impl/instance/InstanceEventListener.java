package software.wings.service.impl.instance;

import com.google.inject.Inject;

import io.harness.queue.QueueListener;
import software.wings.api.InstanceEvent;

/**
 * The instance event that has info about the instance changes, which needs to be pushed
 * @author rktummala on 08/04/19
 *
 */
public class InstanceEventListener extends QueueListener<InstanceEvent> {
  @Inject private InstanceTimeScaleProcessor instanceTimeScaleProcessor;

  public InstanceEventListener() {
    super(false);
  }

  /* (non-Javadoc)
   * @see software.wings.core.queue.QueueListener#onMessage(software.wings.core.queue.Queuable)
   */
  @Override
  public void onMessage(InstanceEvent instanceEvent) {
    instanceTimeScaleProcessor.handleInstanceChanges(instanceEvent);
  }
}

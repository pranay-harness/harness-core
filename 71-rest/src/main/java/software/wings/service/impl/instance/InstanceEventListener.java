package software.wings.service.impl.instance;

import com.google.inject.Inject;

import io.harness.event.timeseries.processor.InstanceEventProcessor;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;
import software.wings.api.InstanceEvent;

/**
 * The instance event that has info about the instance changes, which needs to be pushed
 * @author rktummala on 08/04/19
 *
 */
public class InstanceEventListener extends QueueListener<InstanceEvent> {
  @Inject private InstanceEventProcessor instanceEventProcessor;

  @Inject
  public InstanceEventListener(QueueConsumer<InstanceEvent> queueConsumer) {
    super(queueConsumer, false);
  }

  /* (non-Javadoc)
   * @see software.wings.core.queue.QueueListener#onMessage(software.wings.core.queue.Queuable)
   */
  @Override
  public void onMessage(InstanceEvent instanceEvent) {
    instanceEventProcessor.processEvent(instanceEvent.getTimeSeriesBatchEventInfo());
  }
}

package software.wings.service.impl.event;

import com.google.inject.Inject;

import io.harness.event.timeseries.processor.DeploymentEventProcessor;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;
import software.wings.api.DeploymentTimeSeriesEvent;

/**
 * The instance
 * @author rktummala on 08/04/19
 *
 */
public class DeploymentTimeSeriesEventListener extends QueueListener<DeploymentTimeSeriesEvent> {
  @Inject private DeploymentEventProcessor deploymentEventProcessor;

  @Inject
  public DeploymentTimeSeriesEventListener(QueueConsumer<DeploymentTimeSeriesEvent> queueConsumer) {
    super(queueConsumer, false);
  }

  /* (non-Javadoc)
   * @see software.wings.core.queue.QueueListener#onMessage(software.wings.core.queue.Queuable)
   */
  @Override
  public void onMessage(DeploymentTimeSeriesEvent deploymentTimeSeriesEvent) {
    deploymentEventProcessor.processEvent(deploymentTimeSeriesEvent.getTimeSeriesEventInfo());
  }
}

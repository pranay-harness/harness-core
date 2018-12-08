package software.wings.service.impl.instance;

import com.google.inject.Inject;

import io.harness.queue.QueueListener;
import software.wings.api.DeploymentEvent;

/**
 * Receives all the completed phases and their deployment info and fetches from the corresponding servers.
 * The instance information is used in the service and infrastructure dashboards.
 * @author rktummala on 02/04/18
 *
 * For sender information,
 * @see software.wings.beans.CanaryWorkflowExecutionAdvisor
 */
public class DeploymentEventListener extends QueueListener<DeploymentEvent> {
  @Inject private InstanceHelper instanceHelper;

  public DeploymentEventListener() {
    super(false);
  }

  /* (non-Javadoc)
   * @see software.wings.core.queue.QueueListener#onMessage(software.wings.core.queue.Queuable)
   */
  @Override
  protected void onMessage(DeploymentEvent deploymentEvent) {
    instanceHelper.processDeploymentEvent(deploymentEvent);
  }
}

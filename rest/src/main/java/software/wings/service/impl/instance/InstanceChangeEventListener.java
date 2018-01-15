package software.wings.service.impl.instance;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.InstanceChangeEvent;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.core.queue.AbstractQueueListener;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.utils.Validator;

import java.util.List;
/**
 * Receives all the completed phases and their instance info and feeds it to the instance collection.
 * The instance information is used in the service and infrastructure dashboards.
 * @author rktummala on 08/24/17
 *
 * For sender information,
 * @see software.wings.beans.CanaryWorkflowExecutionAdvisor
 */
public class InstanceChangeEventListener extends AbstractQueueListener<InstanceChangeEvent> {
  private static final Logger logger = LoggerFactory.getLogger(InstanceChangeEventListener.class);
  @Inject private InstanceService instanceService;

  /* (non-Javadoc)
   * @see software.wings.core.queue.AbstractQueueListener#onMessage(software.wings.core.queue.Queuable)
   */
  @Override
  protected void onMessage(InstanceChangeEvent instanceChangeEvent) throws Exception {
    try {
      Validator.notNullCheck("InstanceChangeEvent", instanceChangeEvent);

      // Stop gap solution until the rewrite is done
      List<String> autoScalingGroupList = instanceChangeEvent.getAutoScalingGroupList();
      if (isNotEmpty(autoScalingGroupList)) {
        instanceService.deleteInstancesOfAutoScalingGroups(autoScalingGroupList, instanceChangeEvent.getAppId());
      }
      List<Instance> instanceList = instanceChangeEvent.getInstanceList();
      if (instanceList != null) {
        instanceService.saveOrUpdate(instanceList);
      }
    } catch (Exception ex) {
      // We have to catch all kinds of runtime exceptions, log it and move on, otherwise the queue impl keeps retrying
      // forever in case of exception
      logger.error("Exception while processing phase completion event.", ex);
    }
  }
}

package software.wings.service.impl.instance.sync;

import software.wings.service.impl.instance.sync.request.ContainerSyncRequest;
import software.wings.service.impl.instance.sync.response.ContainerSyncResponse;

/**
 * Interface for all types of container instance sync activities
 * @author rktummala on 09/01/17
 */
public interface ContainerSync {
  ContainerSyncResponse getInstances(ContainerSyncRequest syncRequest, String workflowId, String appId);
}

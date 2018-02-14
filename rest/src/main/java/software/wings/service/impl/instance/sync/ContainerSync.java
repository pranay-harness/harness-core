package software.wings.service.impl.instance.sync;

import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.service.impl.instance.sync.request.ContainerSyncRequest;
import software.wings.service.impl.instance.sync.response.ContainerSyncResponse;

import java.util.List;

/**
 * Interface for all types of container instance sync activities
 * @author rktummala on 09/01/17
 */
public interface ContainerSync {
  ContainerSyncResponse getInstances(ContainerSyncRequest syncRequest);

  ContainerSyncResponse getInstances(
      ContainerInfrastructureMapping containerInfraMapping, List<String> containerSvcNameList);
}

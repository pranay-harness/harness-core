package software.wings.service.impl.instance.sync;

import software.wings.service.impl.instance.sync.request.InstanceSyncRequest;
import software.wings.service.impl.instance.sync.response.InstanceSyncResponse;

/**
 * Base interface for all instance sync activities
 * @author rktummala on 09/01/17
 */
public interface InstanceSyncService { InstanceSyncResponse getInstances(InstanceSyncRequest syncRequest); }

package io.harness.batch.processing.writer;

import static io.harness.event.payloads.Lifecycle.EventType.EVENT_TYPE_START;
import static io.harness.event.payloads.Lifecycle.EventType.EVENT_TYPE_STOP;

import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.entities.ActiveInstance;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.service.intfc.ActiveInstanceService;
import io.harness.batch.processing.service.intfc.InstanceDataService;
import io.harness.event.payloads.Lifecycle;
import io.harness.exception.InvalidRequestException;
import io.harness.grpc.utils.HTimestamps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public abstract class EventWriter {
  @Autowired protected ActiveInstanceService activeInstanceService;
  @Autowired protected InstanceDataService instanceDataService;

  protected boolean createActiveInstance(String accountId, String instanceId, String clusterId) {
    ActiveInstance activeInstance = activeInstanceService.fetchActiveInstance(accountId, instanceId);
    if (null == activeInstance) {
      activeInstance =
          ActiveInstance.builder().accountId(accountId).instanceId(instanceId).clusterId(clusterId).build();
      return activeInstanceService.create(activeInstance);
    }
    return true;
  }

  protected boolean deleteActiveInstance(String accountId, String instanceId) {
    ActiveInstance activeInstance = activeInstanceService.fetchActiveInstance(accountId, instanceId);
    if (null != activeInstance) {
      return activeInstanceService.delete(activeInstance);
    }
    return true;
  }

  protected InstanceData fetchActiveInstanceData(String accountId, String instanceId) {
    List<InstanceState> instanceStates =
        new ArrayList<>(Arrays.asList(InstanceState.INITIALIZING, InstanceState.RUNNING));
    return instanceDataService.fetchActiveInstanceData(accountId, instanceId, instanceStates);
  }

  protected void updateInstanceDataLifecycle(String accountId, String instanceId, Lifecycle lifecycle) {
    Instant instanceTime = HTimestamps.toInstant(lifecycle.getTimestamp());
    InstanceState currentInstanceState = null;
    InstanceState updateInstanceState = null;

    if (lifecycle.getType() == EVENT_TYPE_START) {
      currentInstanceState = InstanceState.INITIALIZING;
      updateInstanceState = InstanceState.RUNNING;
    } else if (lifecycle.getType() == EVENT_TYPE_STOP) {
      currentInstanceState = InstanceState.RUNNING;
      updateInstanceState = InstanceState.STOPPED;
    }

    if (null != currentInstanceState && null != updateInstanceState) {
      InstanceData instanceData = instanceDataService.fetchActiveInstanceData(
          accountId, instanceId, new ArrayList<>(Arrays.asList(currentInstanceState)));
      if (null != instanceData) {
        instanceDataService.updateInstanceState(instanceData, instanceTime, updateInstanceState);
      } else {
        logger.info("Received past duplicate event {} {} {} ", accountId, instanceId, lifecycle.toString());
      }
    }
  }

  protected InstanceData fetchInstanceData(String accountId, String instanceId) {
    InstanceData ec2InstanceData = instanceDataService.fetchInstanceData(accountId, instanceId);
    if (null == ec2InstanceData) {
      logger.error("Instance detail not present {} ", instanceId);
      throw new InvalidRequestException("EC2 Instance detail not present");
    }
    return ec2InstanceData;
  }

  protected void handleLifecycleEvent(String accountId, Lifecycle lifecycle) {
    String instanceId = lifecycle.getInstanceId();

    boolean updateInstanceLifecycle = true;
    if (lifecycle.getType().equals(EVENT_TYPE_STOP)) {
      updateInstanceLifecycle = deleteActiveInstance(accountId, instanceId);
    }

    if (updateInstanceLifecycle) {
      logger.info("Updating instance lifecycle {} ", instanceId);
      updateInstanceDataLifecycle(accountId, instanceId, lifecycle);
    }
  }
}

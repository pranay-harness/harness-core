package io.harness.batch.processing.writer;

import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.entities.ActiveInstance;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.service.intfc.ActiveInstanceService;
import io.harness.batch.processing.service.intfc.InstanceDataService;
import io.harness.event.payloads.Lifecycle;
import io.harness.event.payloads.Lifecycle.EventType;
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
    Instant instanceTime = Instant.ofEpochMilli(lifecycle.getTimestamp().getSeconds() * 1000);
    InstanceState currentInstanceState = null;
    InstanceState updateInstanceState = null;

    if (lifecycle.getType() == EventType.START) {
      currentInstanceState = InstanceState.INITIALIZING;
      updateInstanceState = InstanceState.RUNNING;
    } else if (lifecycle.getType() == EventType.STOP) {
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
}

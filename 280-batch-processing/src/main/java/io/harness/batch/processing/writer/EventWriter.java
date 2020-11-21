package io.harness.batch.processing.writer;

import static io.harness.event.payloads.Lifecycle.EventType.EVENT_TYPE_START;
import static io.harness.event.payloads.Lifecycle.EventType.EVENT_TYPE_STOP;

import io.harness.batch.processing.service.intfc.InstanceDataService;
import io.harness.batch.processing.service.intfc.InstanceResourceService;
import io.harness.ccm.commons.beans.InstanceState;
import io.harness.ccm.commons.entities.InstanceData;
import io.harness.event.payloads.Lifecycle;
import io.harness.exception.InvalidRequestException;
import io.harness.grpc.utils.HTimestamps;

import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class EventWriter {
  @Autowired protected InstanceDataService instanceDataService;
  @Autowired protected CloudToHarnessMappingService cloudToHarnessMappingService;
  @Autowired protected InstanceResourceService instanceResourceService;

  protected InstanceData fetchActiveInstanceData(String accountId, String clusterId, String instanceId) {
    List<InstanceState> instanceStates =
        new ArrayList<>(Arrays.asList(InstanceState.INITIALIZING, InstanceState.RUNNING));
    return instanceDataService.fetchActiveInstanceData(accountId, clusterId, instanceId, instanceStates);
  }

  protected void updateInstanceDataLifecycle(
      String accountId, String clusterId, String instanceId, Lifecycle lifecycle) {
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
          accountId, clusterId, instanceId, new ArrayList<>(Arrays.asList(currentInstanceState)));
      if (null != instanceData
          && !(instanceData.getInstanceState() == InstanceState.RUNNING
              && instanceData.getUsageStartTime().isAfter(instanceTime))) {
        instanceDataService.updateInstanceState(instanceData, instanceTime, updateInstanceState);
      } else {
        log.info("Received past duplicate event {} {} {} ", accountId, instanceId, lifecycle.toString());
      }
    }
  }

  protected InstanceData fetchInstanceData(String accountId, String instanceId) {
    InstanceData ec2InstanceData = instanceDataService.fetchInstanceData(accountId, instanceId);
    if (null == ec2InstanceData) {
      log.error("Instance detail not present {} ", instanceId);
      throw new InvalidRequestException("EC2 Instance detail not present");
    }
    return ec2InstanceData;
  }

  protected void handleLifecycleEvent(String accountId, Lifecycle lifecycle) {
    String instanceId = getIdFromArn(lifecycle.getInstanceId());
    String clusterId = lifecycle.getClusterId();
    updateInstanceDataLifecycle(accountId, clusterId, instanceId, lifecycle);
  }

  protected Set<String> fetchActiveInstanceAtTime(String accountId, String clusterId, Instant startTime) {
    return instanceDataService.fetchClusterActiveInstanceIds(accountId, clusterId, startTime);
  }

  protected String getIdFromArn(String arn) {
    return arn.substring(arn.lastIndexOf('/') + 1);
  }
}

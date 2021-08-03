package io.harness.perpetualtask.instancesync;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.instancesync.InstanceSyncPerpetualTaskResponse;
import io.harness.instancesync.InstanceSyncResourceClient;
import io.harness.utils.RestCallToNGManagerClientUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.DX)
@Singleton
public class InstanceSyncResponsePublisher {
  @Inject private InstanceSyncResourceClient instanceSyncResourceClient;

  public void publishInstanceSyncResponseToNG(String accountIdentifier, String perpetualTaskId,
      InstanceSyncPerpetualTaskResponse instanceSyncPerpetualTaskResponse) {
    if (instanceSyncPerpetualTaskResponse == null) {
      log.error("Instance sync perpetual task response is null for accountIdentifier : {} and perpetualTaskId : {}",
          accountIdentifier, perpetualTaskId);
    }
    boolean response = false;
    int retry = 0;
    while (!response && retry < 3) {
      try {
        response = RestCallToNGManagerClientUtils.execute(instanceSyncResourceClient.sendPerpetualTaskResponse(
            accountIdentifier, perpetualTaskId, instanceSyncPerpetualTaskResponse));
      } catch (Exception exception) {
        log.error(
            "Error occured while sending instance sync perpetual task response from CG to NG for accountIdentifier : {} and perpetualTaskId : {}",
            accountIdentifier, perpetualTaskId, exception);
      }
      retry += 1;
    }

    log.info(
        "Successfully pushed instance sync perpetual task response from CG to NG for accountIdentifier : {} and perpetualTaskId : {}",
        accountIdentifier, perpetualTaskId);
  }
}

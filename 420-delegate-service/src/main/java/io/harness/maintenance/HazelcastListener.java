package io.harness.maintenance;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.hazelcast.core.HazelcastInstance;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class HazelcastListener implements MaintenanceListener {
  @Inject private HazelcastInstance hazelcastInstance;

  @Override
  public void onShutdown() {
    if (hazelcastInstance != null) {
      log.info("Shutdown started. Leaving hazelcast cluster.");
      hazelcastInstance.shutdown();
    }
  }

  @Override
  public void onEnterMaintenance() {
    // do nothing
  }

  @Override
  public void onLeaveMaintenance() {
    // do nothing
  }
}

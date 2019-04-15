package io.harness.maintenance;

import com.google.inject.Inject;

import com.hazelcast.core.HazelcastInstance;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HazelcastListener implements MaintenanceListener {
  @Inject private HazelcastInstance hazelcastInstance;

  @Override
  public void onShutdown() {
    logger.info("Shutdown started. Leaving hazelcast cluster.");
    hazelcastInstance.shutdown();
  }

  @Override
  public void onEnterMaintenance() {}

  @Override
  public void onLeaveMaintenance() {}
}

package io.harness.maintenance;

import com.google.inject.Inject;

import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HazelcastListener implements MaintenanceListener {
  private static final Logger logger = LoggerFactory.getLogger(HazelcastListener.class);

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

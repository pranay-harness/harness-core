package io.harness.lock;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import io.dropwizard.lifecycle.Managed;

public class ManageDistributedLockSvc implements Managed {
  private DistributedLockSvc distributedLockSvc;

  public ManageDistributedLockSvc(DistributedLockSvc distributedLockSvc) {
    this.distributedLockSvc = distributedLockSvc;
  }

  @Override
  public void start() throws Exception {
    if (!distributedLockSvc.isRunning()) {
      distributedLockSvc.startup();
    }
  }

  @Override
  public void stop() throws Exception {
    distributedLockSvc.shutdown();
  }
}

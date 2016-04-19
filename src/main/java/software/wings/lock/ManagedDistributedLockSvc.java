package software.wings.lock;

import com.deftlabs.lock.mongo.DistributedLock;
import com.deftlabs.lock.mongo.DistributedLockOptions;
import com.deftlabs.lock.mongo.DistributedLockSvc;

import io.dropwizard.lifecycle.Managed;

/**
 * Created by peeyushaggarwal on 4/18/16.
 */
public class ManagedDistributedLockSvc implements DistributedLockSvc, Managed {
  private DistributedLockSvc distributedLockSvc;
  private boolean start;

  public ManagedDistributedLockSvc(DistributedLockSvc distributedLockSvc) {
    this.distributedLockSvc = distributedLockSvc;
  }

  @Override
  public DistributedLock create(String s) {
    return distributedLockSvc.create(s);
  }

  @Override
  public DistributedLock create(String s, DistributedLockOptions distributedLockOptions) {
    return distributedLockSvc.create(s, distributedLockOptions);
  }

  @Override
  public void destroy(DistributedLock distributedLock) {
    distributedLockSvc.destroy(distributedLock);
  }

  @Override
  public void startup() {
    distributedLockSvc.startup();
  }

  @Override
  public void shutdown() {
    distributedLockSvc.shutdown();
  }

  @Override
  public boolean isRunning() {
    return distributedLockSvc.isRunning();
  }

  @Override
  public void start() throws Exception {
    if (!isRunning()) {
      startup();
    }
  }

  @Override
  public void stop() throws Exception {
    shutdown();
  }
}

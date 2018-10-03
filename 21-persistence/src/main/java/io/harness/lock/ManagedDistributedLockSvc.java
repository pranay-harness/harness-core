package io.harness.lock;

import com.deftlabs.lock.mongo.DistributedLock;
import com.deftlabs.lock.mongo.DistributedLockOptions;
import com.deftlabs.lock.mongo.DistributedLockSvc;
import io.dropwizard.lifecycle.Managed;

/**
 * Created by peeyushaggarwal on 4/18/16.
 */
public class ManagedDistributedLockSvc implements DistributedLockSvc, Managed {
  private DistributedLockSvc distributedLockSvc;

  /**
   * Instantiates a new managed distributed lock svc.
   *
   * @param distributedLockSvc the distributed lock svc
   */
  public ManagedDistributedLockSvc(DistributedLockSvc distributedLockSvc) {
    this.distributedLockSvc = distributedLockSvc;
  }

  /* (non-Javadoc)
   * @see com.deftlabs.lock.mongo.DistributedLockSvc#create(java.lang.String)
   */
  @Override
  public DistributedLock create(String s) {
    return distributedLockSvc.create(s);
  }

  /* (non-Javadoc)
   * @see com.deftlabs.lock.mongo.DistributedLockSvc#create(java.lang.String,
   * com.deftlabs.lock.mongo.DistributedLockOptions)
   */
  @Override
  public DistributedLock create(String s, DistributedLockOptions distributedLockOptions) {
    return distributedLockSvc.create(s, distributedLockOptions);
  }

  /* (non-Javadoc)
   * @see com.deftlabs.lock.mongo.DistributedLockSvc#destroy(com.deftlabs.lock.mongo.DistributedLock)
   */
  @Override
  public void destroy(DistributedLock distributedLock) {
    distributedLockSvc.destroy(distributedLock);
  }

  /* (non-Javadoc)
   * @see com.deftlabs.lock.mongo.DistributedLockSvc#startup()
   */
  @Override
  public void startup() {
    distributedLockSvc.startup();
  }

  /* (non-Javadoc)
   * @see com.deftlabs.lock.mongo.DistributedLockSvc#shutdown()
   */
  @Override
  public void shutdown() {
    distributedLockSvc.shutdown();
  }

  @Override
  public boolean isRunning() {
    return distributedLockSvc.isRunning();
  }

  /* (non-Javadoc)
   * @see io.dropwizard.lifecycle.Managed#start()
   */
  @Override
  public void start() throws Exception {
    if (!isRunning()) {
      startup();
    }
  }

  /* (non-Javadoc)
   * @see io.dropwizard.lifecycle.Managed#stop()
   */
  @Override
  public void stop() throws Exception {
    shutdown();
  }
}

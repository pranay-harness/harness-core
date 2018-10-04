package io.harness.rule;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.deftlabs.lock.mongo.DistributedLockSvcFactory;
import com.deftlabs.lock.mongo.DistributedLockSvcOptions;
import com.mongodb.MongoClient;
import io.harness.factory.ClosingFactory;

import java.io.Closeable;
import java.io.IOException;

public interface DistributedLockRuleMixin {
  default DistributedLockSvc distributedLockSvc(
      MongoClient mongoClient, String databaseName, ClosingFactory closingFactory) {
    DistributedLockSvcOptions distributedLockSvcOptions =
        new DistributedLockSvcOptions(mongoClient, databaseName, "locks");
    distributedLockSvcOptions.setEnableHistory(false);
    DistributedLockSvc distributedLockSvc = new DistributedLockSvcFactory(distributedLockSvcOptions).getLockSvc();
    if (!distributedLockSvc.isRunning()) {
      distributedLockSvc.startup();
    }

    closingFactory.addServer(new Closeable() {
      @Override
      public void close() throws IOException {
        distributedLockSvc.shutdown();
      }
    });

    return distributedLockSvc;
  }
}

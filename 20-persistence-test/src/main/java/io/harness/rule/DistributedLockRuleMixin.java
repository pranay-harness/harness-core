package io.harness.rule;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.deftlabs.lock.mongo.DistributedLockSvcOptions;
import com.deftlabs.lock.mongo.impl.SvcImpl;
import com.mongodb.MongoClient;
import io.harness.factory.ClosingFactory;

import java.io.Closeable;
import java.io.IOException;

public interface DistributedLockRuleMixin extends MongoRuleMixin {
  default DistributedLockSvc distributedLockSvc(
      MongoClient mongoClient, String databaseName, ClosingFactory closingFactory) {
    DistributedLockSvcOptions distributedLockSvcOptions =
        new DistributedLockSvcOptions(mongoClient, databaseName, "locks");
    distributedLockSvcOptions.setEnableHistory(false);
    DistributedLockSvc distributedLockSvc = new SvcImpl(distributedLockSvcOptions);
    distributedLockSvc.startup();

    closingFactory.addServer(new Closeable() {
      @Override
      public void close() throws IOException {
        distributedLockSvc.shutdown();
      }
    });

    return distributedLockSvc;
  }
}

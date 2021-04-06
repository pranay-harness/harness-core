package software.wings.scheduler;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistentIterable;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;

import software.wings.beans.security.AccessRequest;
import software.wings.service.intfc.AccessRequestService;
import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;

public class AccessRequestHandler implements Handler<AccessRequest> {
  private static final int ACCESS_REQUEST_CHECK_INTERVAL = 15;

  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private AccessRequestService accessRequestService;
  @Inject private MorphiaPersistenceProvider<AccessRequest> persistenceProvider;
  @Inject private AccountService accountService;
  private PersistenceIterator<PersistentIterable> accessRequestHandler;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("AccessRequestHandler")
            .poolSize(5)
            .interval(ofSeconds(5))
            .build(),
        AccessRequest.class,
        MongoPersistenceIterator.<AccessRequest, MorphiaFilterExpander<AccessRequest>>builder()
            .clazz(AccessRequest.class)
            .filterExpander(query -> query.filter(AccessRequest.AccessRequestKeys.accessActive, Boolean.TRUE))
            .fieldName(AccessRequest.AccessRequestKeys.nextIteration)
            .targetInterval(ofMinutes(15))
            .acceptableNoAlertDelay(ofMinutes(15))
            .acceptableExecutionTime(ofMinutes(10))
            .handler(this)
            .schedulingType(REGULAR)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }

  @Override
  public void handle(AccessRequest accessRequest) {
    accessRequestService.checkAndUpdateAccessRequests(accessRequest);
  }
}

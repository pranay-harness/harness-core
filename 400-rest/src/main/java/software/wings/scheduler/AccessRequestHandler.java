package software.wings.scheduler;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofHours;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import io.harness.cvng.state.CVNGVerificationTask;
import io.harness.execution.export.background.ExportExecutionsRequestHandler;
import io.harness.execution.export.request.ExportExecutionsRequest;
import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistentIterable;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.workers.background.AccountStatusBasedEntityProcessController;

import software.wings.alerts.AlertStatus;
import software.wings.beans.Account;
import software.wings.beans.BarrierInstance;
import software.wings.beans.DeletedEntity;
import software.wings.beans.alert.Alert;
import software.wings.beans.security.AccessRequest;
import software.wings.service.intfc.AccessRequestService;

import com.google.inject.Inject;

public class AccessRequestHandler implements Handler<AccessRequest> {
  private static final int ACCESS_REQUEST_CHECK_INTERVAL = 15;

  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private AccessRequestService accessRequestService;
  private PersistenceIterator<PersistentIterable> accessRequestHandler;

  public void registerIterators() {
    accessRequestHandler =
            persistenceIteratorFactory
                    .createPumpIteratorWithDedicatedThreadPool(PersistenceIteratorFactory.PumpExecutorOptions.builder()
                                    .name("AccessRequestHandler")
                                    .poolSize(3)
                                    .interval(ofMinutes(ACCESS_REQUEST_CHECK_INTERVAL))
                                    .build(),
                            AccessRequestHandler.class,
                            MongoPersistenceIterator.builder()
                                    .clazz(AccessRequest.class)
                                    .fieldName(AccessRequest.AccessRequestKeys.nextIteration)
                                    .filterExpander(query -> query.filter(AccessRequest.AccessRequestKeys.accessActive, Boolean.TRUE))
                                    .targetInterval(ofMinutes(15))
                                    .acceptableNoAlertDelay(ofMinutes(15))
                                    .acceptableExecutionTime(ofMinutes(15))
                                    .handler(this)
                                    .schedulingType(REGULAR)
                                    .redistribute(true));
  }

  @Override
  public void handle(AccessRequest entity) {}
}

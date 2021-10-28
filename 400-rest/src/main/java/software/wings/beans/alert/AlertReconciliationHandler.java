package software.wings.beans.alert;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofMinutes;

import io.harness.dms.DmsProxy;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.persistence.HPersistence;
import io.harness.workers.background.AccountStatusBasedEntityProcessController;

import software.wings.alerts.AlertStatus;
import software.wings.beans.alert.Alert.AlertKeys;
import software.wings.beans.alert.NoEligibleDelegatesAlertReconciliation.NoEligibleDelegatesAlertReconciliationKeys;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;

import com.google.inject.Inject;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AlertReconciliationHandler implements Handler<Alert> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;

  @Inject private DmsProxy dmsProxy;
  @Inject private AlertService alertService;

  @Inject private HPersistence persistence;
  @Inject private MorphiaPersistenceProvider<Alert> persistenceProvider;
  @Inject private AccountService accountService;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PumpExecutorOptions.builder().name("AlertReconciliation").poolSize(3).interval(Duration.ofMinutes(1)).build(),
        AlertReconciliationHandler.class,
        MongoPersistenceIterator.<Alert, MorphiaFilterExpander<Alert>>builder()
            .clazz(Alert.class)
            .fieldName(AlertKeys.alertReconciliation_nextIteration)
            .targetInterval(ofMinutes(10))
            .acceptableNoAlertDelay(ofMinutes(5))
            .handler(this)
            .entityProcessController(new AccountStatusBasedEntityProcessController<>(accountService))
            .filterExpander(query
                -> query.filter(AlertKeys.alertReconciliation_needed, Boolean.TRUE)
                       .field(AlertKeys.status)
                       .notEqual(AlertStatus.Closed))
            .schedulingType(REGULAR)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }

  @Override
  public void handle(Alert alert) {
    switch (alert.getType()) {
      case NoEligibleDelegates:
        handleNoEligibleDelegates(alert);
        break;
      default:
        persistence.update(
            alert, persistence.createUpdateOperations(Alert.class).unset(AlertKeys.alertReconciliation_needed));
        break;
    }
  }

  public void handleNoEligibleDelegates(Alert alert) {
    final boolean canAssign = dmsProxy.alertReconCanAssign(alert);
    if (canAssign) {
      alertService.close(alert);
      return;
    }

    persistence.update(alert,
        persistence.createUpdateOperations(Alert.class)
            .unset(AlertKeys.alertReconciliation_needed)
            .unset(AlertKeys.alertReconciliation + "." + NoEligibleDelegatesAlertReconciliationKeys.delegates));
  }
}

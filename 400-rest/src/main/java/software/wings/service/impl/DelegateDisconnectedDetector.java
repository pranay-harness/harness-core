package software.wings.service.impl;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.persistence.HQuery.excludeAuthority;

import static software.wings.beans.DelegateConnection.EXPIRY_TIME;

import static java.lang.System.currentTimeMillis;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.task.DelegateLogContext;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueController;
import io.harness.version.VersionInfoManager;

import software.wings.beans.Delegate;
import software.wings.beans.DelegateConnection;
import software.wings.beans.DelegateConnection.DelegateConnectionKeys;
import software.wings.service.intfc.DelegateService;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

@Slf4j
@TargetModule(Module._420_DELEGATE_SERVICE)
public class DelegateDisconnectedDetector implements Runnable {
  @Inject private DelegateService delegateService;
  @Inject private QueueController queueController;
  @Inject private HPersistence persistence;
  @Inject private VersionInfoManager versionInfoManager;

  @Override
  public void run() {
    if (queueController.isNotPrimary()) {
      return;
    }

    Query<DelegateConnection> delegateConnectionQuery =
        persistence.createQuery(DelegateConnection.class, excludeAuthority)
            .filter(DelegateConnectionKeys.disconnected, Boolean.FALSE)
            .field(DelegateConnectionKeys.lastHeartbeat)
            .lessThanOrEq(currentTimeMillis() - EXPIRY_TIME.toMillis());

    try (HIterator<DelegateConnection> delegateConnections = new HIterator<>(delegateConnectionQuery.fetch())) {
      for (DelegateConnection delegateConnection : delegateConnections) {
        Delegate delegate =
            delegateService.get(delegateConnection.getAccountId(), delegateConnection.getDelegateId(), false);

        if (versionInfoManager.getVersionInfo().getVersion().equals(delegateConnection.getVersion())) {
          disconnectedDetected(delegate.isPolllingModeEnabled(), delegateConnection);
          continue;
        }

        if (persistence.createQuery(DelegateConnection.class)
                .filter(DelegateConnectionKeys.accountId, delegateConnection.getAccountId())
                .filter(DelegateConnectionKeys.delegateId, delegateConnection.getDelegateId())
                .filter(DelegateConnectionKeys.version, versionInfoManager.getVersionInfo().getVersion())
                .count(HPersistence.upToOne)
            == 0) {
          disconnectedDetected(delegate.isPolllingModeEnabled(), delegateConnection);
        }
        persistence.delete(delegateConnection);
      }
    }
  }

  @VisibleForTesting
  public void disconnectedDetected(boolean polllingModeEnabled, DelegateConnection delegateConnection) {
    try (DelegateLogContext ignore = new DelegateLogContext(
             delegateConnection.getAccountId(), delegateConnection.getDelegateId(), OVERRIDE_ERROR)) {
      log.info("Delegate was detected as disconnected");
      if (!polllingModeEnabled) {
        log.error("Non-polling delegate was detected as disconnected");
      }
      delegateService.delegateDisconnected(
          delegateConnection.getAccountId(), delegateConnection.getDelegateId(), delegateConnection.getUuid());
    }
  }
}

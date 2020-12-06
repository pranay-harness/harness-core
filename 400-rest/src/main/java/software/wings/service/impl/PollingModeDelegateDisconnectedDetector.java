package software.wings.service.impl;

import static io.harness.persistence.HPersistence.returnNewOptions;
import static io.harness.persistence.HQuery.excludeAuthority;

import static software.wings.beans.DelegateConnection.EXPIRY_TIME;

import static java.lang.System.currentTimeMillis;

import io.harness.observer.Subject;

import software.wings.beans.Delegate;
import software.wings.beans.Delegate.DelegateKeys;
import software.wings.beans.DelegateConnection;
import software.wings.beans.DelegateConnection.DelegateConnectionKeys;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import java.util.List;
import lombok.Getter;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

public class PollingModeDelegateDisconnectedDetector implements Runnable {
  @Inject private WingsPersistence wingsPersistence;

  @Inject @Getter private Subject<DelegateObserver> subject = new Subject<>();

  @Override
  public void run() {
    List<Delegate> pollingDelegateIds = wingsPersistence.createQuery(Delegate.class, excludeAuthority)
                                            .filter(DelegateKeys.polllingModeEnabled, Boolean.TRUE)
                                            .project(DelegateKeys.uuid, true)
                                            .project(DelegateKeys.accountId, true)
                                            .asList();

    UpdateOperations updateOperations = wingsPersistence.createUpdateOperations(DelegateConnection.class)
                                            .set(DelegateConnectionKeys.disconnected, Boolean.TRUE);

    for (Delegate delegate : pollingDelegateIds) {
      Query<DelegateConnection> delegateConnectionQuery =
          wingsPersistence.createQuery(DelegateConnection.class)
              .filter(DelegateConnectionKeys.accountId, delegate.getAccountId())
              .filter(DelegateConnectionKeys.delegateId, delegate.getUuid())
              .filter(DelegateConnectionKeys.disconnected, Boolean.FALSE)
              .field(DelegateConnectionKeys.lastHeartbeat)
              .lessThanOrEq(currentTimeMillis() - EXPIRY_TIME.toMillis());

      DelegateConnection delegateConnection = null;
      do {
        delegateConnection =
            wingsPersistence.findAndModify(delegateConnectionQuery, updateOperations, returnNewOptions);
      } while (delegateConnection != null);
      subject.fireInform(DelegateObxxxxxxxx:onDisconnected, delegate.getAccountId(), delegate.getUuid());
    }
  }
}

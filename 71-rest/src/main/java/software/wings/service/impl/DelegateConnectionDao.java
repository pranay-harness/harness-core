package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HPersistence.upToOne;
import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static software.wings.beans.DelegateConnection.DEFAULT_EXPIRY_TIME_IN_MINUTES;
import static software.wings.beans.ManagerConfiguration.MATCH_ALL_VERSION;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.beans.DelegateConnectionHeartbeat;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.DelegateConnection;
import software.wings.beans.DelegateConnection.DelegateConnectionKeys;
import software.wings.beans.DelegateStatus;
import software.wings.beans.ManagerConfiguration;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class DelegateConnectionDao {
  @Inject private HPersistence persistence;

  public void delegateDisconnected(String accountId, String delegateConnectionId) {
    logger.info("Removing delegate connection delegateConnectionId: {}", delegateConnectionId);
    persistence.delete(DelegateConnection.class, delegateConnectionId);
  }

  public void registerHeartbeat(String accountId, String delegateId, DelegateConnectionHeartbeat heartbeat) {
    Query<DelegateConnection> query = persistence.createQuery(DelegateConnection.class)
                                          .filter(DelegateConnectionKeys.accountId, accountId)
                                          .filter(DelegateConnectionKeys.uuid, heartbeat.getDelegateConnectionId());

    UpdateOperations<DelegateConnection> updateOperations =
        persistence.createUpdateOperations(DelegateConnection.class)
            .set(DelegateConnectionKeys.accountId, accountId)
            .set(DelegateConnectionKeys.uuid, heartbeat.getDelegateConnectionId())
            .set(DelegateConnectionKeys.delegateId, delegateId)
            .set(DelegateConnectionKeys.version, heartbeat.getVersion())
            .set(DelegateConnectionKeys.lastHeartbeat, currentTimeMillis())
            .set(DelegateConnectionKeys.validUntil,
                Date.from(OffsetDateTime.now().plusMinutes(DEFAULT_EXPIRY_TIME_IN_MINUTES).toInstant()));

    persistence.upsert(query, updateOperations);
  }

  public Map<String, List<DelegateStatus.DelegateInner.DelegateConnectionInner>> obtainActiveDelegateConnections(
      String accountId) {
    List<DelegateConnection> delegateConnections = persistence.createQuery(DelegateConnection.class)
                                                       .filter(DelegateConnectionKeys.accountId, accountId)
                                                       .project(DelegateConnectionKeys.delegateId, true)
                                                       .project(DelegateConnectionKeys.version, true)
                                                       .project(DelegateConnectionKeys.lastHeartbeat, true)
                                                       .asList();

    return delegateConnections.stream().collect(Collectors.groupingBy(delegateConnection
        -> delegateConnection.getDelegateId(),
        Collectors.mapping(delegateConnection
            -> DelegateStatus.DelegateInner.DelegateConnectionInner.builder()
                   .uuid(delegateConnection.getUuid())
                   .lastHeartbeat(delegateConnection.getLastHeartbeat())
                   .version(delegateConnection.getVersion())
                   .build(),
            toList())));
  }

  public Set<String> obtainDisconnectedDelegates(String accountId) {
    Query<DelegateConnection> query =
        persistence.createQuery(DelegateConnection.class).filter(DelegateConnectionKeys.accountId, accountId);
    String primaryVersion = persistence.createQuery(ManagerConfiguration.class).get().getPrimaryVersion();
    if (isNotEmpty(primaryVersion) && !StringUtils.equals(primaryVersion, MATCH_ALL_VERSION)) {
      query.filter(DelegateConnectionKeys.version, primaryVersion);
    }
    return query.project(DelegateConnectionKeys.delegateId, true)
        .asList()
        .stream()
        .map(DelegateConnection::getDelegateId)
        .collect(toSet());
  }

  public String save(DelegateConnection delegateConnection) {
    return persistence.save(delegateConnection);
  }

  public List<DelegateConnection> list(String accountId, String delegateId) {
    return persistence.createQuery(DelegateConnection.class)
        .filter(DelegateConnectionKeys.accountId, accountId)
        .filter(DelegateConnectionKeys.delegateId, delegateId)
        .asList();
  }

  public boolean checkDelegateConnected(String accountId, String delegateId, String version) {
    return persistence.createQuery(DelegateConnection.class)
               .filter(DelegateConnectionKeys.accountId, accountId)
               .filter(DelegateConnectionKeys.delegateId, delegateId)
               .filter(DelegateConnectionKeys.version, version)
               .count(upToOne)
        > 0;
  }
}

package io.harness.ccm.health;

import com.google.inject.Inject;

import io.harness.ccm.cluster.entities.LastReceivedPublishedMessage;
import io.harness.ccm.cluster.entities.LastReceivedPublishedMessage.LastReceivedPublishedMessageKeys;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
public class LastReceivedPublishedMessageDao {
  private final HPersistence hPersistence;

  @Inject
  public LastReceivedPublishedMessageDao(HPersistence hPersistence) {
    this.hPersistence = hPersistence;
  }

  public LastReceivedPublishedMessage upsert(String accountId, String identifier) {
    return hPersistence.upsert(hPersistence.createQuery(LastReceivedPublishedMessage.class)
                                   .field(LastReceivedPublishedMessageKeys.accountId)
                                   .equal(accountId)
                                   .field(LastReceivedPublishedMessageKeys.identifier)
                                   .equal(identifier),
        hPersistence.createUpdateOperations(LastReceivedPublishedMessage.class)
            .set(LastReceivedPublishedMessageKeys.accountId, accountId)
            .set(LastReceivedPublishedMessageKeys.identifier, identifier)
            .set(LastReceivedPublishedMessageKeys.lastReceivedAt, Instant.now().toEpochMilli()),
        HPersistence.upsertReturnNewOptions);
  }

  public LastReceivedPublishedMessage get(String accountId, String identifier) {
    return hPersistence.createQuery(LastReceivedPublishedMessage.class)
        .field(LastReceivedPublishedMessageKeys.accountId)
        .equal(accountId)
        .field(LastReceivedPublishedMessageKeys.identifier)
        .equal(identifier)
        .get();
  }

  public Instant getFirstEventReceivedTime(String accountId) {
    Query<LastReceivedPublishedMessage> filteredQuery =
        hPersistence.createQuery(LastReceivedPublishedMessage.class)
            .filter(LastReceivedPublishedMessageKeys.accountId, accountId)
            .order(Sort.ascending(LastReceivedPublishedMessageKeys.createdAt));
    LastReceivedPublishedMessage lastReceivedPublishedMessage = filteredQuery.get();
    if (null != lastReceivedPublishedMessage) {
      logger.info("First event received time {}", lastReceivedPublishedMessage.getCreatedAt());
      return Instant.ofEpochMilli(lastReceivedPublishedMessage.getCreatedAt()).truncatedTo(ChronoUnit.DAYS);
    }
    return null;
  }
}
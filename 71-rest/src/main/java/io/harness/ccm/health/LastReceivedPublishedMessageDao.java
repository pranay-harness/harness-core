package io.harness.ccm.health;

import com.google.inject.Inject;

import io.harness.ccm.cluster.entities.LastReceivedPublishedMessage;
import io.harness.ccm.cluster.entities.LastReceivedPublishedMessage.LastReceivedPublishedMessageKeys;
import io.harness.persistence.HPersistence;

import java.time.Instant;

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
}
package io.harness.event.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.harness.ccm.cluster.entities.LastReceivedPublishedMessage;
import io.harness.ccm.cluster.entities.LastReceivedPublishedMessage.LastReceivedPublishedMessageKeys;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.service.intfc.LastReceivedPublishedMessageRepository;
import io.harness.persistence.HPersistence;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class LastReceivedPublishedMessageRepositoryImpl implements LastReceivedPublishedMessageRepository {
  private final HPersistence hPersistence;

  private Cache<CacheKey, Boolean> lastReceivedPublishedMessageCache =
      Caffeine.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).build();

  private static final String IDENTIFIER_KEY = "identifier";

  @Inject
  public LastReceivedPublishedMessageRepositoryImpl(HPersistence hPersistence) {
    this.hPersistence = hPersistence;
  }

  private static boolean containsIdentifierKey(PublishedMessage publishedMessage) {
    return publishedMessage.getAttributes().keySet().stream().anyMatch(s -> s.startsWith(IDENTIFIER_KEY));
  }

  @Value
  private static class CacheKey {
    private String accountId;
    private String identifier;
  }

  @Override
  public void updateLastReceivedPublishedMessages(List<PublishedMessage> publishedMessages) {
    publishedMessages.stream()
        .filter(LastReceivedPublishedMessageRepositoryImpl::containsIdentifierKey)
        .forEach(publishedMessage
            -> publishedMessage.getAttributes()
                   .entrySet()
                   .stream()
                   .filter(mapEntry -> mapEntry.getKey().startsWith(IDENTIFIER_KEY))
                   .forEach(identifier
                       -> updateLastReceivedPublishedMessage(publishedMessage.getAccountId(), identifier.getValue())));
  }

  private void updateLastReceivedPublishedMessage(String accountId, String identifier) {
    CacheKey cacheKey = new CacheKey(accountId, identifier);
    lastReceivedPublishedMessageCache.get(cacheKey, key -> upsert(key.getAccountId(), key.getIdentifier()) != null);
  }

  private LastReceivedPublishedMessage upsert(String accountId, String identifier) {
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
}

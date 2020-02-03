package io.harness.event.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.harness.ccm.health.LastReceivedPublishedMessageDao;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.service.intfc.LastReceivedPublishedMessageRepository;
import io.harness.grpc.IdentifierKeys;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class LastReceivedPublishedMessageRepositoryImpl implements LastReceivedPublishedMessageRepository {
  private final LastReceivedPublishedMessageDao lastReceivedPublishedMessageDao;

  private Cache<CacheKey, Boolean> lastReceivedPublishedMessageCache =
      Caffeine.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).build();

  @Inject
  public LastReceivedPublishedMessageRepositoryImpl(LastReceivedPublishedMessageDao lastReceivedPublishedMessageDao) {
    this.lastReceivedPublishedMessageDao = lastReceivedPublishedMessageDao;
  }

  private static boolean containsIdentifierKey(PublishedMessage publishedMessage) {
    return publishedMessage.getAttributes().keySet().stream().anyMatch(s -> s.startsWith(IdentifierKeys.PREFIX));
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
                   .filter(mapEntry -> mapEntry.getKey().startsWith(IdentifierKeys.PREFIX))
                   .forEach(identifier
                       -> updateLastReceivedPublishedMessage(publishedMessage.getAccountId(), identifier.getValue())));
  }

  private void updateLastReceivedPublishedMessage(String accountId, String identifier) {
    CacheKey cacheKey = new CacheKey(accountId, identifier);
    lastReceivedPublishedMessageCache.get(
        cacheKey, key -> lastReceivedPublishedMessageDao.upsert(key.getAccountId(), key.getIdentifier()) != null);
  }
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.service.impl;

import io.harness.ccm.commons.entities.batch.LastReceivedPublishedMessage;
import io.harness.ccm.commons.entities.batch.LatestClusterInfo;
import io.harness.ccm.commons.entities.events.PublishedMessage;
import io.harness.ccm.health.LastReceivedPublishedMessageDao;
import io.harness.event.service.intfc.LastReceivedPublishedMessageRepository;
import io.harness.grpc.IdentifierKeys;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

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
        cacheKey, key -> updateLatestClusterEvents(key.getAccountId(), key.getIdentifier()));
  }

  private boolean updateLatestClusterEvents(String accountId, String identifier) {
    LastReceivedPublishedMessage lastReceivedPublishedMessage = lastReceivedPublishedMessageDao.get(accountId);
    if (null == lastReceivedPublishedMessage) {
      LatestClusterInfo latestClusterInfo =
          LatestClusterInfo.builder().accountId(accountId).identifier(identifier).build();
      lastReceivedPublishedMessageDao.saveLatestClusterInfo(latestClusterInfo);
    }
    return lastReceivedPublishedMessageDao.upsert(accountId, identifier) != null;
  }
}

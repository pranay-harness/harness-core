/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.service.impl;

import static com.google.common.cache.CacheLoader.InvalidCacheLoadException;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.callback.DelegateCallback;
import io.harness.delegate.beans.DelegateCallbackRecord;
import io.harness.delegate.beans.DelegateCallbackRecord.DelegateCallbackRecordKeys;
import io.harness.exception.UnexpectedException;
import io.harness.persistence.HPersistence;
import io.harness.service.intfc.DelegateCallbackRegistry;
import io.harness.service.intfc.DelegateCallbackService;
import io.harness.service.intfc.DelegateTaskResultsProvider;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

@Singleton
@ValidateOnExecution
@Slf4j
public class DelegateCallbackRegistryImpl implements DelegateCallbackRegistry {
  @Inject private HPersistence persistence;

  private LoadingCache<String, DelegateCallbackService> delegateCallbackServiceCache =
      CacheBuilder.newBuilder()
          .maximumSize(1000)
          .expireAfterAccess(10, TimeUnit.MINUTES)
          .removalListener((RemovalListener<String, DelegateCallbackService>)
                               removalNotification -> removalNotification.getValue().destroy())
          .build(new CacheLoader<String, DelegateCallbackService>() {
            @Override
            public DelegateCallbackService load(String driverId) {
              return buildDelegateCallbackService(driverId);
            }
          });

  private LoadingCache<String, DelegateTaskResultsProvider> delegateTaskResultsProviderCache =
      CacheBuilder.newBuilder()
          .maximumSize(100)
          .expireAfterAccess(10, TimeUnit.MINUTES)
          .removalListener((RemovalListener<String, DelegateTaskResultsProvider>)
                               removalNotification -> removalNotification.getValue().destroy())
          .build(new CacheLoader<String, DelegateTaskResultsProvider>() {
            @Override
            public DelegateTaskResultsProvider load(String driverId) {
              return buildDelegateTaskResultsProvider(driverId);
            }
          });

  @Override
  public String ensureCallback(DelegateCallback delegateCallback) {
    try {
      byte[] bytes = delegateCallback.toByteArray();
      // TODO: use better hash-sum
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] hashInBytes = md.digest(bytes);
      String uuid = Base64.encodeBase64URLSafeString(hashInBytes);

      DelegateCallbackRecord.builder().uuid(uuid).callbackMetadata(bytes).build();

      persistence.upsert(
          persistence.createQuery(DelegateCallbackRecord.class).filter(DelegateCallbackRecordKeys.uuid, uuid),
          persistence.createUpdateOperations(DelegateCallbackRecord.class)
              .set(DelegateCallbackRecordKeys.uuid, uuid)
              .set(DelegateCallbackRecordKeys.callbackMetadata, bytes)
              .set(DelegateCallbackRecordKeys.validUntil, Date.from(OffsetDateTime.now().plusMonths(1).toInstant())));

      return uuid;
    } catch (NoSuchAlgorithmException e) {
      throw new UnexpectedException("Unexpected", e);
    }
  }

  @Override
  public DelegateCallbackService obtainDelegateCallbackService(String driverId) {
    if (isBlank(driverId)) {
      return null;
    }

    try {
      return delegateCallbackServiceCache.get(driverId);
    } catch (ExecutionException | InvalidCacheLoadException ex) {
      log.error("Unexpected error occurred while fetching callback service from cache.", ex);
      return null;
    }
  }

  @VisibleForTesting
  public DelegateCallbackService buildDelegateCallbackService(String driverId) {
    if (isBlank(driverId)) {
      return null;
    }

    DelegateCallbackRecord delegateCallbackRecord = persistence.get(DelegateCallbackRecord.class, driverId);
    if (delegateCallbackRecord == null) {
      return null;
    }

    try {
      DelegateCallback delegateCallback = DelegateCallback.parseFrom(delegateCallbackRecord.getCallbackMetadata());
      if (delegateCallback.hasMongoDatabase()) {
        return new MongoDelegateCallbackService(delegateCallback.getMongoDatabase());
      }
    } catch (InvalidProtocolBufferException | IllegalArgumentException e) {
      throw new UnexpectedException("Invalid callback metadata", e);
    }

    return null;
  }

  @Override
  public DelegateTaskResultsProvider obtainDelegateTaskResultsProvider(String driverId) {
    if (isBlank(driverId)) {
      return null;
    }

    try {
      return delegateTaskResultsProviderCache.get(driverId);
    } catch (ExecutionException | InvalidCacheLoadException ex) {
      log.error("Unexpected error occurred while fetching callback service from cache.", ex);
      return null;
    }
  }

  @Override
  public DelegateTaskResultsProvider buildDelegateTaskResultsProvider(String driverId) {
    if (isBlank(driverId)) {
      return null;
    }

    DelegateCallbackRecord delegateCallbackRecord = persistence.get(DelegateCallbackRecord.class, driverId);
    if (delegateCallbackRecord == null) {
      return null;
    }

    try {
      DelegateCallback delegateCallback = DelegateCallback.parseFrom(delegateCallbackRecord.getCallbackMetadata());
      if (delegateCallback.hasMongoDatabase()) {
        return new MongoDelegateTaskResultsProviderImpl(delegateCallback.getMongoDatabase());
      }
    } catch (InvalidProtocolBufferException | IllegalArgumentException e) {
      throw new UnexpectedException("Invalid callback metadata", e);
    }

    return null;
  }
}

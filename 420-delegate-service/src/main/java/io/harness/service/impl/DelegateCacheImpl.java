package io.harness.service.impl;

import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.Delegate.DelegateKeys;
import io.harness.persistence.HPersistence;
import io.harness.service.intfc.DelegateCache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;

@Singleton
@ValidateOnExecution
@Slf4j
public class DelegateCacheImpl implements DelegateCache {
  private static final int MAX_DELEGATE_META_INFO_ENTRIES = 10000;

  @Inject private HPersistence persistence;

  private LoadingCache<String, Optional<Delegate>> delegateCache =
      CacheBuilder.newBuilder()
          .maximumSize(MAX_DELEGATE_META_INFO_ENTRIES)
          .expireAfterWrite(1, TimeUnit.MINUTES)
          .build(new CacheLoader<String, Optional<Delegate>>() {
            @Override
            public Optional<Delegate> load(String delegateId) {
              return Optional.ofNullable(
                  persistence.createQuery(Delegate.class).filter(DelegateKeys.uuid, delegateId).get());
            }
          });

  @Override
  public Delegate get(String accountId, String delegateId, boolean forceRefresh) {
    try {
      if (forceRefresh) {
        delegateCache.refresh(delegateId);
      }
      Delegate delegate = delegateCache.get(delegateId).orElse(null);
      if (delegate != null && (delegate.getAccountId() == null || !delegate.getAccountId().equals(accountId))) {
        // TODO: this is serious, we should not return the delegate if the account is not the expected one
        //       just to be on the safe side, make sure that all such scenarios are first fixed
        log.error("Delegate account id mismatch", new Exception(""));
      }
      return delegate;
    } catch (ExecutionException e) {
      log.error("Execution exception", e);
    } catch (UncheckedExecutionException e) {
      log.error("Delegate not found exception", e);
    }
    return null;
  }
}

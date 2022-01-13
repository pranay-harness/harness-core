/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.dl;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.Account;
import software.wings.service.intfc.AccountService;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.PL)
@TargetModule(HarnessModule._960_PERSISTENCE)
public class GenericDbCache {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;
  private LoadingCache<String, Object> cache = CacheBuilder.newBuilder()
                                                   .maximumSize(10000)
                                                   .expireAfterWrite(1, TimeUnit.MINUTES)
                                                   .build(new CacheLoader<String, Object>() {
                                                     @Override
                                                     public Object load(String key) throws Exception {
                                                       int idx = key.lastIndexOf('~');
                                                       String className = key.substring(0, idx);
                                                       String uuid = key.substring(idx + 1);

                                                       // TODO We need to define a generic way of calling the
                                                       // service.get(), for that we need a locator class that maps
                                                       // entityClassName and service class. This special handling is
                                                       // needed since the accountService.get() also decrypts the
                                                       // license info.
                                                       if (Account.class.getCanonicalName().equals(className)) {
                                                         return accountService.get(uuid);
                                                       }
                                                       final Class<?> aClass = Class.forName(className);
                                                       return wingsPersistence.getDatastore(aClass).get(aClass, uuid);
                                                     }
                                                   });

  /**
   * Put.
   *
   * @param cls    the cls
   * @param objKey the obj key
   * @param value  the value
   */
  public void put(Class cls, String objKey, Object value) {
    cache.put(makeCacheKey(cls, objKey), value);
  }

  private String makeCacheKey(Class cls, String objKey) {
    return cls.getCanonicalName() + "~" + objKey;
  }

  /**
   * Gets the.
   *
   * @param <T>    the generic type
   * @param cls    the cls
   * @param objKey the obj key
   * @return the t
   */
  public <T> T get(Class<T> cls, String objKey) {
    try {
      return (T) cache.get(makeCacheKey(cls, objKey));
    } catch (Exception ex) {
      log.warn("Exception occurred in fetching key {}, {}, {}", cls.getSimpleName(), objKey, ex.getMessage());
    }
    return null;
  }

  /**
   * Invalidate.
   *
   * @param cls    the cls
   * @param objKey the obj key
   */
  public void invalidate(Class cls, String objKey) {
    cache.invalidate(makeCacheKey(cls, objKey));
  }
}

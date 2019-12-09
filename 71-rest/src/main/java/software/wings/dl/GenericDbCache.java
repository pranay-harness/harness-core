package software.wings.dl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.service.intfc.AccountService;

import java.util.concurrent.TimeUnit;

/**
 * Created by anubhaw on 3/18/16.
 */
@Singleton
@Slf4j
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
      logger.warn("Exception occurred in fetching key {}, {}", cls.getSimpleName(), objKey, ex);
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

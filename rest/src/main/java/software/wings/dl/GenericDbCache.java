package software.wings.dl;

import static java.lang.String.format;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.service.intfc.AccountService;

import java.util.concurrent.TimeUnit;

/**
 * Created by anubhaw on 3/18/16.
 */
@Singleton
public class GenericDbCache {
  private static final Logger logger = LoggerFactory.getLogger(GenericDbCache.class);
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;
  private LoadingCache<String, Object> cache =
      CacheBuilder.newBuilder()
          .maximumSize(10000)
          .expireAfterAccess(5, TimeUnit.MINUTES)
          .build(new CacheLoader<String, Object>() {
            @Override
            public Object load(String key) throws Exception {
              int idx = key.lastIndexOf('~');
              String className = key.substring(0, idx);
              String uuid = key.substring(idx + 1);

              // TODO We need to define a generic way of calling the service.get(),
              // for that we need a locator class that maps entityClassName and service class.
              // This special handling is needed since the accountService.get() also decrypts the license info.
              if (Account.class.getCanonicalName().equals(className)) {
                return accountService.get(uuid);
              }

              return wingsPersistence.getDatastore().get(Class.forName(className), uuid);
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
      logger.warn(format("Exception occurred in fetching key %s, %s", cls.getSimpleName(), objKey), ex);
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

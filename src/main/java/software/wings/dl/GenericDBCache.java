package software.wings.dl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Created by anubhaw on 3/18/16.
 */

public class GenericDBCache {
  @Inject private WingsPersistence wingsPersistence;

  private LoadingCache<String, Object> cache =
      CacheBuilder.newBuilder()
          .maximumSize(10000)
          .expireAfterAccess(30, TimeUnit.MINUTES)
          .build(new CacheLoader<String, Object>() {
            @Override
            public Object load(String key) throws Exception {
              int idx = key.lastIndexOf("~");
              String className = key.substring(0, idx);
              String uuid = key.substring(idx + 1);
              return wingsPersistence.getDatastore().get(Class.forName(className), uuid);
            }
          });

  public void put(Class cls, String objKey, Object value) {
    cache.put(makeCacheKey(cls, objKey), value);
  }

  public <T> T get(Class<T> cls, String objKey) {
    Object obj = null;
    try {
      obj = cache.get(makeCacheKey(cls, objKey));
    } catch (Exception e) {
    } // do nothing
    return (T) obj;
  }

  private String makeCacheKey(Class cls, String objKey) {
    return cls.getCanonicalName() + "~" + objKey;
  }
}

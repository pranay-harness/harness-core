package software.wings.dl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

/**
 * Created by anubhaw on 3/18/16.
 */
public class GenericDbCache {
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

  private String makeCacheKey(Class cls, String objKey) {
    return cls.getCanonicalName() + "~" + objKey;
  }

  public <T> T get(Class<T> cls, String objKey) {
    try {
      Object obj = cache.get(makeCacheKey(cls, objKey));
      return (T) obj;
    } catch (ExecutionException e) {
    } // do nothing
    return null;
  }
}

package software.wings.search.framework;

import org.mongodb.morphia.Key;
import org.mongodb.morphia.mapping.cache.EntityCache;
import org.mongodb.morphia.mapping.cache.EntityCacheStatistics;

public class NoopEntityCache implements EntityCache {
  @Override
  public Boolean exists(Key<?> key) {
    return false;
  }

  @Override
  public void flush() {}

  @Override
  public <T> T getEntity(Key<T> key) {
    return null;
  }

  @Override
  public <T> T getProxy(Key<T> key) {
    return null;
  }

  @Override
  public void notifyExists(Key<?> key, boolean b) {}

  @Override
  public <T> void putEntity(Key<T> key, T t) {}

  @Override
  public <T> void putProxy(Key<T> key, T t) {}

  @Override
  public EntityCacheStatistics stats() {
    return null;
  }
}

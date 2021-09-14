/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import org.mongodb.morphia.Key;
import org.mongodb.morphia.mapping.cache.EntityCache;
import org.mongodb.morphia.mapping.cache.EntityCacheStatistics;

/**
 * Implementation of entity cache required by
 * morphia mapper for conversion of DBObject to POJO.
 *
 * @author utkarsh
 */
@OwnedBy(PL)
public class NoopEntityCache implements EntityCache {
  @Override
  public Boolean exists(Key<?> key) {
    return false;
  }

  @Override
  public void flush() {
    // Do nothing because it is a noop cache.
  }

  @Override
  public <T> T getEntity(Key<T> key) {
    return null;
  }

  @Override
  public <T> T getProxy(Key<T> key) {
    return null;
  }

  @Override
  public void notifyExists(Key<?> key, boolean b) {
    // Do nothing because it is a noop cache.
  }

  @Override
  public <T> void putEntity(Key<T> key, T t) {
    // Do nothing because it is a noop cache.
  }

  @Override
  public <T> void putProxy(Key<T> key, T t) {
    // Do nothing because it is a noop cache.
  }

  @Override
  public EntityCacheStatistics stats() {
    return null;
  }
}

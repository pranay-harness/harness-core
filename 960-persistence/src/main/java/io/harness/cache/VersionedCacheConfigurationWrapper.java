/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cache;

import javax.cache.configuration.Configuration;

public class VersionedCacheConfigurationWrapper<K, V> implements Configuration<K, V> {
  private Configuration<VersionedKey<K>, V> jcacheConfig;
  private String version;

  public VersionedCacheConfigurationWrapper(Configuration<VersionedKey<K>, V> jcacheConfig, String version) {
    this.jcacheConfig = jcacheConfig;
    this.version = version;
  }

  public String getVersion() {
    return version;
  }

  public Configuration<VersionedKey<K>, V> getInternalCacheConfig() {
    return jcacheConfig;
  }

  @Override
  public Class<K> getKeyType() {
    return (Class<K>) Object.class;
  }

  @Override
  public Class<V> getValueType() {
    return jcacheConfig.getValueType();
  }

  @Override
  public boolean isStoreByValue() {
    return jcacheConfig.isStoreByValue();
  }
}

/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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

/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cache;

import java.time.Duration;
import java.util.List;

public interface DistributedStore {
  // Returns the stored cache - whatever changeHash
  <T extends Distributable> T get(long algorithmId, long structureHash, String key, List<String> params);

  // Returns the stored cache - only with exact changeHash
  <T extends Distributable> T get(
      long contextHash, long algorithmId, long structureHash, String key, List<String> params);

  //  Inserts or updates the cache for entity
  <T extends Distributable> void upsert(T entity, Duration ttl);

  //  Inserts or updates the cache for entity with downgrade option
  <T extends Distributable> void upsert(T entity, Duration ttl, boolean downgrade);
}

/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.distribution.barrier;

import static java.util.Collections.synchronizedMap;

import java.util.Map;
import org.apache.commons.collections.map.LRUMap;

public class InprocBarrierRegistry implements BarrierRegistry {
  private Map<BarrierId, Forcer> map = synchronizedMap(new LRUMap(1000));

  @Override
  public void save(BarrierId id, Forcer forcer) throws UnableToSaveBarrierException {
    if (map.putIfAbsent(id, forcer) != null) {
      throw new UnableToSaveBarrierException("The barrier with this id already exists");
    }
  }

  @Override
  public Barrier load(BarrierId id) throws UnableToLoadBarrierException {
    return Barrier.builder().id(id).forcer(map.get(id)).build();
  }
}

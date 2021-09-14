/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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

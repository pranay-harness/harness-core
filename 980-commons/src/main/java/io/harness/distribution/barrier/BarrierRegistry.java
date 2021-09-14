/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.distribution.barrier;

public interface BarrierRegistry {
  void save(BarrierId id, Forcer forcer) throws UnableToSaveBarrierException;
  Barrier load(BarrierId id) throws UnableToLoadBarrierException;
}

package io.harness.distribution.barrier;

public interface BarrierRegistry {
  void save(BarrierId id, Forcer forcer) throws UnableToSaveBarrierException;
  Barrier load(BarrierId id) throws UnableToLoadBarrierException;
}

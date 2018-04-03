package io.harness.distribution.barrier;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class Forcer {
  private ForcerId id;
  private Map<String, Object> metadata;
  private List<Forcer> children;

  public enum State {
    // The forcer is absent for the barrier purposes
    ABSENT,

    // The forcer is moving forward towards the barrier
    APPROACHING,

    // The forcer arrived at the barrier and apples pushing force to it
    ARRIVED,

    // The forcer abandoned the effort to push the barrier
    ABANDONED
  }
}

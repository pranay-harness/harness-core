package io.harness.distribution.barrier;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class Forcer {
  private ForcerId id;
  private List<Forcer> children;
  enum State {
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

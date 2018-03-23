package io.harness.distribution.barrier;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class Forcer {
  private ForcerId id;
  private List<Forcer> children;

  enum State { RUNNING, SUCCEEDED, FAILED }

  State state;
}

package io.harness.presentation;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.modes.ExecutionMode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@EqualsAndHashCode
public class Subgraph implements Serializable {
  @Getter private ExecutionMode mode;
  @Getter private final List<GraphVertex> vertices = new ArrayList<>();

  public Subgraph(ExecutionMode mode) {
    this.mode = mode;
  }
}

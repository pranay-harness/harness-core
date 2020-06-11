package io.harness.presentation;

import io.harness.facilitator.modes.ExecutionMode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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

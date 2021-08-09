package io.harness.persistence;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Store {
  private String name;
}

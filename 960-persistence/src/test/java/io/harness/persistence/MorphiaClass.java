package io.harness.persistence;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MorphiaClass implements MorphiaInterface {
  private String test;
}

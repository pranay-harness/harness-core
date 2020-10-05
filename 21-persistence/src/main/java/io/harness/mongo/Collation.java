package io.harness.mongo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Collation {
  String locale;
  int strength;
}

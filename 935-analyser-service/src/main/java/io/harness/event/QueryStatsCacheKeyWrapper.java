package io.harness.event;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QueryStatsCacheKeyWrapper {
  String hash;
  long count;
}

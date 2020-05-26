package io.harness.cvng.beans;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CVHistogram {
  String query;
  long intervalMs;
  @Singular("addBar") List<Bar> bars;
  @Value
  @Builder
  public static class Bar {
    long timestamp;
    long count;
  }
}

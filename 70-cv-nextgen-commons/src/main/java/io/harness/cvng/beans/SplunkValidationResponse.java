package io.harness.cvng.beans;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class SplunkValidationResponse {
  Histogram histogram;
  SplunkSampleResponse samples;
  String errorMessage;
  long queryDurationMillis;

  @Value
  @Builder
  public static class SplunkSampleResponse {
    List<SampleLog> rawSampleLogs;
    Map<String, String> sample;
    String splunkQuery;
    String errorMessage;
  }
  @Value
  @Builder
  public static class SampleLog {
    String raw;
    long timestamp;
  }
  @Value
  @Builder
  public static class Histogram {
    String query;
    long intervalMs;
    @Singular("addBar") List<Bar> bars;
    String errorMessage;
    String splunkQuery;
    long count;
    @Value
    @Builder
    public static class Bar {
      long timestamp;
      long count;
    }
  }
}

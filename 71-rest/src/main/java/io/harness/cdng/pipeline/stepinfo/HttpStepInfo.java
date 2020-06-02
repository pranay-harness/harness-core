package io.harness.cdng.pipeline.stepinfo;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.pipeline.CDStepInfo;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("http")
public class HttpStepInfo implements CDStepInfo {
  String name;
  String type;
  String identifier;
  int retry;
  int timeout;
  HttpSpec spec;

  @Value
  @Builder
  public static class HttpSpec {
    String url;
    String body;
    String header;
    String method;
    int socketTimeoutMillis;
  }
}

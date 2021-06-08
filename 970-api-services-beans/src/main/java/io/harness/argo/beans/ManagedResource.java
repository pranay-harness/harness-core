package io.harness.argo.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManagedResource {
  String kind;
  String name;
  String namespace;
  String targetState;
  String liveState;
  String normalizedLiveState;
  String predictedLiveState;
}

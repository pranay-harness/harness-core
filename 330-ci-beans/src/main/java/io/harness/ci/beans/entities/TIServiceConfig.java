package io.harness.ci.beans.entities;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

@Value
@Getter
@Setter
@Builder
public class TIServiceConfig {
  String baseUrl;
  String globalToken;
}

package io.harness.ng;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.PL)
public class BaseUrls {
  @JsonProperty("currentGenUiUrl") String currentGenUiUrl;
  @JsonProperty("nextGenUiUrl") String nextGenUiUrl;
  @JsonProperty("nextGenAuthUiUrl") String nextGenAuthUiUrl;
}

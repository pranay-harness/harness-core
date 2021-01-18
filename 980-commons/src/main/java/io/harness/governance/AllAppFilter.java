package io.harness.governance;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("ALL")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AllAppFilter extends ApplicationFilter {
  @JsonCreator
  public AllAppFilter(@JsonProperty("filterType") BlackoutWindowFilterType blackoutWindowFilterType,
      @JsonProperty("envSelection") EnvironmentFilter envSelection) {
    super(blackoutWindowFilterType, envSelection);
  }
}

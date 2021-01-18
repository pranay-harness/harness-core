package io.harness.governance;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("ALL_NON_PROD")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AllNonProdEnvFilter extends EnvironmentFilter {
  @JsonCreator
  public AllNonProdEnvFilter(@JsonProperty("filterType") EnvironmentFilterType filterType) {
    super(filterType);
  }
}

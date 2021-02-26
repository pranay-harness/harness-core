package io.harness.governance;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("ALL_PROD")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AllProdEnvFilter extends EnvironmentFilter {
  @JsonCreator
  public AllProdEnvFilter(@JsonProperty("filterType") EnvironmentFilterType filterType) {
    super(filterType);
  }
}

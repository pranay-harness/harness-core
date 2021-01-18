package io.harness.governance;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Data;

@JsonTypeName("CUSTOM")
@Data
public class CustomEnvFilter extends EnvironmentFilter {
  private List<String> environments;
  @JsonCreator
  public CustomEnvFilter(@JsonProperty("filterType") EnvironmentFilterType filterType,
      @JsonProperty("environments") List<String> environments) {
    super(filterType);
    this.environments = environments;
  }
}
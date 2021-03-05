package io.harness.governance;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@JsonTypeName("CUSTOM")
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomEnvFilter extends EnvironmentFilter {
  private List<String> environments;
  @JsonCreator
  @Builder
  public CustomEnvFilter(@JsonProperty("filterType") EnvironmentFilterType filterType,
      @JsonProperty("environments") List<String> environments) {
    super(filterType);
    this.environments = environments;
  }
}
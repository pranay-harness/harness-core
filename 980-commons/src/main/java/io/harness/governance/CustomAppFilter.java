package io.harness.governance;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Data;

@JsonTypeName("CUSTOM")
@Data
public class CustomAppFilter extends ApplicationFilter {
  private List<String> apps;

  @JsonCreator
  public CustomAppFilter(@JsonProperty("filterType") BlackoutWindowFilterType blackoutWindowFilterType,
      @JsonProperty("envSelection") EnvironmentFilter envSelection, @JsonProperty("apps") List<String> apps) {
    super(blackoutWindowFilterType, envSelection);
    this.apps = apps;
  }
}

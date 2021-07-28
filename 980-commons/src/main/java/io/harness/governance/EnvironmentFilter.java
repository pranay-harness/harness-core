package io.harness.governance;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, property = "filterType", include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = CustomEnvFilter.class, name = "CUSTOM")
  , @JsonSubTypes.Type(value = AllEnvFilter.class, name = "ALL"),
      @JsonSubTypes.Type(value = AllProdEnvFilter.class, name = "ALL_PROD"),
      @JsonSubTypes.Type(value = AllNonProdEnvFilter.class, name = "ALL_NON_PROD")
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@OwnedBy(HarnessTeam.CDC)
public class EnvironmentFilter {
  private EnvironmentFilterType filterType;
  public enum EnvironmentFilterType { ALL_PROD, ALL_NON_PROD, ALL, CUSTOM }

  @JsonCreator
  public EnvironmentFilter(@JsonProperty("filterType") EnvironmentFilterType filterType) {
    this.filterType = filterType;
  }
}

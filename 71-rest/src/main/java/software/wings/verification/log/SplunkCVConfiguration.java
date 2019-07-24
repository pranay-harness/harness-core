package software.wings.verification.log;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.github.reinert.jjschema.Attributes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import software.wings.stencils.DefaultValue;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "SplunkCVConfigurationKeys")
public class SplunkCVConfiguration extends LogsCVConfiguration {
  private boolean isAdvancedQuery;

  @Attributes(required = true, title = "Host Name Field") @DefaultValue("hostname") private String hostnameField;

  @Attributes(title = "Is advanced query", required = false)
  @DefaultValue("false")
  @JsonProperty(value = "isAdvancedQuery")
  public boolean isAdvancedQuery() {
    return isAdvancedQuery;
  }

  public void setAdvancedQuery(boolean advancedQuery) {
    this.isAdvancedQuery = advancedQuery;
  }
  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonPropertyOrder({"type", "harnessApiVersion"})
  public static class SplunkCVConfigurationYaml extends LogsCVConfigurationYaml {
    private boolean isAdvancedQuery;
    private String hostnameField;
  }
}

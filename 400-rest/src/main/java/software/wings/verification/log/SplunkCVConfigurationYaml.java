package software.wings.verification.log;

import software.wings.stencils.DefaultValue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.sun.tracing.dtrace.Attributes;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"type", "harnessApiVersion"})
public class SplunkCVConfigurationYaml extends LogsCVConfigurationYaml {
  private boolean isAdvancedQuery;
  private String hostnameField;

  @Attributes(title = "Is advanced query", required = false)
  @DefaultValue("false")
  @JsonProperty(value = "isAdvancedQuery")
  public boolean isAdvancedQuery() {
    return isAdvancedQuery;
  }

  public void setAdvancedQuery(boolean advancedQuery) {
    this.isAdvancedQuery = advancedQuery;
  }
}

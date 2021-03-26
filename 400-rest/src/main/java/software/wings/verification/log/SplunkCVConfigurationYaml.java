package software.wings.verification.log;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.stencils.DefaultValue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.github.reinert.jjschema.Attributes;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
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

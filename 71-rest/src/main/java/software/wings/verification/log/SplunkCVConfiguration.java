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
import software.wings.service.impl.analysis.DataCollectionInfoV2;
import software.wings.service.impl.splunk.SplunkDataCollectionInfoV2;
import software.wings.stencils.DefaultValue;
import software.wings.verification.CVConfiguration;

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

  @Override
  public CVConfiguration deepCopy() {
    SplunkCVConfiguration clonedConfig = new SplunkCVConfiguration();
    super.copy(clonedConfig);
    clonedConfig.setQuery(this.getQuery());
    clonedConfig.setAdvancedQuery(this.isAdvancedQuery());
    clonedConfig.setHostnameField(this.getHostnameField());
    return clonedConfig;
  }

  @Override
  public DataCollectionInfoV2 toDataCollectionInfo() {
    SplunkDataCollectionInfoV2 splunkDataCollectionInfoV2 =
        SplunkDataCollectionInfoV2.builder().query(getQuery()).hostnameField(getHostnameField()).build();
    fillDataCollectionInfoWithCommonFields(splunkDataCollectionInfoV2);
    return splunkDataCollectionInfoV2;
  }

  @Override
  public boolean isCVTaskBasedCollectionEnabled() {
    return true;
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

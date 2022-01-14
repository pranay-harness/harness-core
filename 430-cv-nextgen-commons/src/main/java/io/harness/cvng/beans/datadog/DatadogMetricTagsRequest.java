package io.harness.cvng.beans.datadog;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.DataCollectionRequest;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@JsonTypeName("DATADOG_METRIC_TAGS")
@Data
@SuperBuilder
@NoArgsConstructor
@OwnedBy(CV)
@FieldNameConstants(innerTypeName = "DatadogMetricTagsRequestKeys")
@EqualsAndHashCode(callSuper = true)
public class DatadogMetricTagsRequest extends DatadogRequest {
  private static final String DSL =
      DataCollectionRequest.readDSL("datadog-metric-tags.datacollection", DatadogMetricTagsRequest.class);

  private String metric;

  @Override
  public String getDSL() {
    return DSL;
  }

  @Override
  public Map<String, Object> fetchDslEnvVariables() {
    Map<String, Object> commonVariables = super.fetchDslEnvVariables();
    commonVariables.put(DatadogMetricTagsRequestKeys.metric, metric);
    return commonVariables;
  }
}

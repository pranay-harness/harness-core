package io.harness.cvng.beans.stackdriver;

import static io.harness.cvng.utils.StackdriverUtils.checkForNullAndReturnValue;

import io.harness.cvng.beans.stackdriver.StackDriverMetricDefinition.Aggregation.AggregationKeys;
import io.harness.cvng.beans.stackdriver.StackDriverMetricDefinition.StackDriverMetricDefinitionKeys;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.Instant;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@JsonTypeName("STACKDRIVER_SAMPLE_DATA")
@SuperBuilder
@Data
@NoArgsConstructor
@FieldNameConstants(innerTypeName = "StackdriverSampleDataRequestKeys")
public class StackdriverSampleDataRequest extends StackdriverRequest {
  private static final String timestampFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'";
  private static final String timestampFormatKey = "timestampFormat";
  Instant startTime;
  Instant endTime;
  StackDriverMetricDefinition metricDefinition;
  public static final String DSL =
      StackdriverDashboardRequest.readDSL("stackdriver-sample-data.datacollection", StackdriverDashboardRequest.class);
  @Override
  public String getDSL() {
    return DSL;
  }

  @Override
  public String getBaseUrl() {
    return "https://monitoring.googleapis.com/v3/projects/";
  }

  @Override
  public Map<String, Object> fetchDslEnvVariables() {
    StackDriverMetricDefinition.Aggregation aggregation = metricDefinition.getAggregation();
    StackdriverCredential credential = StackdriverCredential.fromGcpConnector(getConnectorConfigDTO());
    Map<String, Object> dslEnvVariables = getCommonEnvVariables(credential);
    dslEnvVariables.put(
        AggregationKeys.alignmentPeriod, checkForNullAndReturnValue(aggregation.getAlignmentPeriod(), "60s"));
    dslEnvVariables.put(
        AggregationKeys.crossSeriesReducer, checkForNullAndReturnValue(aggregation.getCrossSeriesReducer(), ""));
    dslEnvVariables.put(
        AggregationKeys.perSeriesAligner, checkForNullAndReturnValue(aggregation.getPerSeriesAligner(), ""));

    dslEnvVariables.put(StackDriverMetricDefinitionKeys.filter, metricDefinition.getFilter());

    dslEnvVariables.put(StackdriverSampleDataRequestKeys.startTime, startTime.toString());
    dslEnvVariables.put(StackdriverSampleDataRequestKeys.endTime, endTime.toString());
    dslEnvVariables.put(timestampFormatKey, timestampFormat);

    return dslEnvVariables;
  }
}

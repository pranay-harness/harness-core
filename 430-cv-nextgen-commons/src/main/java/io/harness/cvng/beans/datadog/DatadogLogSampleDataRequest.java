package io.harness.cvng.beans.datadog;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.stackdriver.StackdriverLogRequest;
import io.harness.cvng.utils.StackdriverUtils;
import io.harness.delegate.beans.cvng.datadog.DatadogUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.harness.annotations.dev.HarnessTeam.CV;
import static io.harness.cvng.utils.StackdriverUtils.Scope.LOG_SCOPE;

@JsonTypeName("DATADOG_LOG_SAMPLE_DATA")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@FieldNameConstants(innerTypeName = "DatadogLogSampleDataRequestKeys")
@OwnedBy(CV)
public class DatadogLogSampleDataRequest extends DatadogRequest {

  public static final String DSL = DatadogLogSampleDataRequest.readDSL(
      "datadog-logs.datacollection", DatadogLogSampleDataRequest.class);

  Long from;
  Long to;
  List<String> indexes;
  String query;
  Long limit;

  @Override
  public String getDSL() {
    return DSL;
  }

  @Override
  public Map<String, Object> fetchDslEnvVariables() {
    Map<String, Object> dslEnvVariables = super.fetchDslEnvVariables();
    dslEnvVariables.put(DatadogLogSampleDataRequestKeys.query, query);
    dslEnvVariables.put(DatadogLogSampleDataRequestKeys.from, from);
    dslEnvVariables.put(DatadogLogSampleDataRequestKeys.to, to);
    dslEnvVariables.put(DatadogLogSampleDataRequestKeys.limit, limit);
    return dslEnvVariables;
  }
}

package io.harness.cvng.beans.datadog;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.DataCollectionRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

import java.util.Map;

import static io.harness.annotations.dev.HarnessTeam.CV;

@JsonTypeName("DATADOG_ACTIVE_METRICS")
@Data
@SuperBuilder
@NoArgsConstructor
@OwnedBy(CV)
@FieldNameConstants(innerTypeName = "DatadogActiveMetricsRequestKeys")
@EqualsAndHashCode(callSuper = true)
public class DatadogActiveMetricsRequest extends DatadogRequest {
    private static final String DSL =
            DataCollectionRequest.readDSL("datadog-active-metrics.datacollection", DatadogActiveMetricsRequest.class);

    private long from;
    private String host;
    private String tagFilter;
    @Override
    public String getDSL() {
        return DSL;
    }

    @Override
    public Map<String, Object> fetchDslEnvVariables() {
        Map<String, Object> commonVariables = super.fetchDslEnvVariables();
        commonVariables.put(DatadogActiveMetricsRequestKeys.from, from);
        return commonVariables;
    }
}

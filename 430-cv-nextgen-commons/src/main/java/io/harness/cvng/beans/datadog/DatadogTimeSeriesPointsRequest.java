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

@JsonTypeName("DATADOG_TIME_SERIES_POINTS")
@Data
@SuperBuilder
@NoArgsConstructor
@OwnedBy(CV)
@FieldNameConstants(innerTypeName = "DatadogTimeSeriesPointsRequestKeys")
@EqualsAndHashCode(callSuper = true)
public class DatadogTimeSeriesPointsRequest extends DatadogRequest {
    private static final String DSL =
            DataCollectionRequest.readDSL("datadog-time-series-points.datacollection", DatadogTimeSeriesPointsRequest.class);

    private Long from;
    private Long to;
    private String query;

    @Override
    public String getDSL() {
        return DSL;
    }

    @Override
    public Map<String, Object> fetchDslEnvVariables() {
        Map<String, Object> commonVariables = super.fetchDslEnvVariables();
        commonVariables.put(DatadogTimeSeriesPointsRequestKeys.from, from);
        commonVariables.put(DatadogTimeSeriesPointsRequestKeys.to, to);
        commonVariables.put(DatadogTimeSeriesPointsRequestKeys.query, query);
        return commonVariables;
    }
}

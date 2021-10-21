package io.harness.cvng.beans.datadog;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.DataCollectionRequest;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

import static io.harness.annotations.dev.HarnessTeam.CV;

@JsonTypeName("DATADOG_TIME_SERIES_POINTS")
@Data
@SuperBuilder
@NoArgsConstructor
@OwnedBy(CV)
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
        commonVariables.put("from", from);
        commonVariables.put("to", to);
        commonVariables.put("query", query);

        return commonVariables;
    }
}

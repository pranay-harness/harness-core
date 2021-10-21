package io.harness.cvng.beans.datadog;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.DataCollectionRequest;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

import static io.harness.annotations.dev.HarnessTeam.CV;

@JsonTypeName("DATADOG_DASHBOARD_DETAILS")
@Data
@SuperBuilder
@NoArgsConstructor
@OwnedBy(CV)
public class DatadogDashboardDetailsRequest extends DatadogRequest {
    private static final String DSL =
            DataCollectionRequest.readDSL("datadog-dashboard-details.datacollection", DatadogDashboardDetailsRequest.class);
    private String dashboardId;
    @Override
    public String getDSL() {
        return DSL;
    }

    @Override
    public Map<String, Object> fetchDslEnvVariables() {
        Map<String, Object> commonEnvVariables = super.fetchDslEnvVariables();
        commonEnvVariables.put("dashboardId", dashboardId);
        return commonEnvVariables;
    }
}

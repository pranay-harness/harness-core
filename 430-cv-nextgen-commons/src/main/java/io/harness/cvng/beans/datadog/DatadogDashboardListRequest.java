package io.harness.cvng.beans.datadog;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.cvng.beans.stackdriver.StackdriverDashboardRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

import static io.harness.annotations.dev.HarnessTeam.CV;

@JsonTypeName("DATADOG_DASHBOARD_LIST")
@SuperBuilder
@NoArgsConstructor
@OwnedBy(CV)
@EqualsAndHashCode(callSuper = true)
public class DatadogDashboardListRequest extends DatadogRequest {

    private static final String DSL =
            DataCollectionRequest.readDSL("datadog-dashboard-list.datacollection", DatadogDashboardListRequest.class);

    @Override
    public String getDSL() {
        return DSL;
    }

}

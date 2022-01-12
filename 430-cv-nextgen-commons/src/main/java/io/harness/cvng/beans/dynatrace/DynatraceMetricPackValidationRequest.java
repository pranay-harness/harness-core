package io.harness.cvng.beans.dynatrace;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.cvng.beans.MetricPackDTO;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.harness.annotations.dev.HarnessTeam.CV;

@JsonTypeName("DYNATRACE_VALIDATION_REQUEST")
@Data
@SuperBuilder
@NoArgsConstructor
@OwnedBy(CV)
public class DynatraceMetricPackValidationRequest extends DynatraceRequest {
    public static final String DSL = DataCollectionRequest.readDSL(
            "dynatrace-metric-pack-validation.datacollection", DynatraceMetricPackValidationRequest.class);

    private static final String RESOLUTION_PARAM = "1m";
    private String serviceId;
    private MetricPackDTO metricPack;

    @Override
    public String getDSL() {
        return DSL;
    }

    @Override
    public Map<String, Object> fetchDslEnvVariables() {
        Map<String, Object> commonEnvVariables = super.fetchDslEnvVariables();
        commonEnvVariables.put("entitySelector", "type(\"SERVICE\"),entityId(".concat(serviceId).concat(")"));
        commonEnvVariables.put("from", "now-3h");
        commonEnvVariables.put("resolution", "1m");

        List<Map<String, String>> metricsToValidate =
                metricPack.getMetrics().stream()
                        .map(metricDefinitionDTO -> {
                            Map<String, String> queryData = new HashMap<>();
                            queryData.put("query", metricDefinitionDTO.getPath());
                            queryData.put("metricName", metricDefinitionDTO.getName());
                            return queryData;
                        })
                        .collect(Collectors.toList());
        commonEnvVariables.put("metricsToValidate", metricsToValidate);
        return commonEnvVariables;
    }
}

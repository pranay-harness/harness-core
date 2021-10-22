package io.harness.cvng.core.beans.monitoredService.healthSouceSpec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.beans.monitoredService.HealthSource;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.MetricPackService;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.List;
@Data
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatadogLogHealthSourceSpec extends HealthSourceSpec {

    @Override
    public HealthSource.CVConfigUpdateResult getCVConfigUpdateResult(String accountId, String orgIdentifier,
                                                                     String projectIdentifier, String environmentRef,
                                                                     String serviceRef, String identifier, String name,
                                                                     List<CVConfig> existingCVConfigs, MetricPackService metricPackService) {
        return null;
    }

    @Override
    public DataSourceType getType() {
        return DataSourceType.DATADOG_LOG;
    }
}

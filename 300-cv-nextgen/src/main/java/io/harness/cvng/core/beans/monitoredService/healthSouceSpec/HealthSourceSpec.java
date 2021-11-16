package io.harness.cvng.core.beans.monitoredService.healthSouceSpec;

import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.beans.monitoredService.HealthSource.CVConfigUpdateResult;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.data.validator.EntityIdentifier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.NotEmpty;

@SuperBuilder
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AppDynamicsHealthSourceSpec.class, name = "AppDynamics")
  , @JsonSubTypes.Type(value = NewRelicHealthSourceSpec.class, name = "NewRelic"),
      @JsonSubTypes.Type(value = StackdriverLogHealthSourceSpec.class, name = "StackdriverLog"),
      @JsonSubTypes.Type(value = SplunkHealthSourceSpec.class, name = "Splunk"),
      @JsonSubTypes.Type(value = PrometheusHealthSourceSpec.class, name = "Prometheus"),
      @JsonSubTypes.Type(value = StackdriverMetricHealthSourceSpec.class, name = "Stackdriver"),
      @JsonSubTypes.Type(value = DatadogMetricHealthSourceSpec.class, name = "DatadogMetrics"),
      @JsonSubTypes.Type(value = DatadogLogHealthSourceSpec.class, name = "DatadogLog")
})
public abstract class HealthSourceSpec {
  @NotEmpty @EntityIdentifier(allowScoped = true) String connectorRef;
  public abstract CVConfigUpdateResult getCVConfigUpdateResult(String accountId, String orgIdentifier,
      String projectIdentifier, String environmentRef, String serviceRef, String identifier, String name,
      List<CVConfig> existingCVConfigs, MetricPackService metricPackService);
  @JsonIgnore public abstract DataSourceType getType();
  public void validate() {}
}

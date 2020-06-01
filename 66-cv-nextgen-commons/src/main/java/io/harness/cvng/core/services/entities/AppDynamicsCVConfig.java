package io.harness.cvng.core.services.entities;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cvng.models.DataSourceType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@JsonTypeName("APP_DYNAMICS")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AppDynamicsCVConfig extends MetricCVConfig {
  private long tierId;
  private long applicationId;
  private String applicationName;
  private String tierName;
  @Override
  public DataSourceType getType() {
    return DataSourceType.APP_DYNAMICS;
  }
}

package io.harness.cvng.core.services.entities;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cvng.models.DataSourceType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
@JsonTypeName("SPLUNK")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SplunkCVConfig extends LogCVConfig {
  @Override
  public DataSourceType getType() {
    return DataSourceType.SPLUNK;
  }
}

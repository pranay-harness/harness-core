package io.harness.cvng.core.services.entities;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cvng.beans.DataSourceType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
@JsonTypeName("SPLUNK")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SplunkCVConfig extends LogCVConfig {
  private String serviceInstanceIdentifier;
  @Override
  public DataSourceType getType() {
    return DataSourceType.SPLUNK;
  }
}

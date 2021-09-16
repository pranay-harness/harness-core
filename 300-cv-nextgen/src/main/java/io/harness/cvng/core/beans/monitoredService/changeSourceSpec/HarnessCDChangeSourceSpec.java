package io.harness.cvng.core.beans.monitoredService.changeSourceSpec;

import io.harness.cvng.beans.change.ChangeSourceType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HarnessCDChangeSourceSpec extends ChangeSourceSpec {
  @Override
  public ChangeSourceType getType() {
    return ChangeSourceType.HARNESS_CD;
  }
}

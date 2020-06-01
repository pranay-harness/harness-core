package io.harness.cvng.core.services.entities;

import io.harness.cvng.beans.TimeRange;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class LogCVConfig extends CVConfig {
  private TimeRange baseline;
  private String query;
}

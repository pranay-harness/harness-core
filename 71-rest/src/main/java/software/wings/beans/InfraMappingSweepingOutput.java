package software.wings.beans;

import io.harness.pms.sdk.core.data.SweepingOutput;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InfraMappingSweepingOutput implements SweepingOutput {
  private String infraMappingId;
}

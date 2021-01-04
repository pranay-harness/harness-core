package software.wings.beans;

import io.harness.pms.sdk.core.data.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("infraMappingSweepingOutput")
public class InfraMappingSweepingOutput implements SweepingOutput {
  private String infraMappingId;

  @Override
  public String getType() {
    return "infraMappingSweepingOutput";
  }
}

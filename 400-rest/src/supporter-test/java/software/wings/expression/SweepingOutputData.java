package software.wings.expression;

import io.harness.pms.sdk.core.data.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("sweepingOutputData")
public class SweepingOutputData implements SweepingOutput {
  String text;

  @Override
  public String getType() {
    return "sweepingOutputData";
  }
}

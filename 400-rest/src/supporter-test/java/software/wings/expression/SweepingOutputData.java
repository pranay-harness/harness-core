package software.wings.expression;

import io.harness.pms.sdk.core.data.SweepingOutput;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SweepingOutputData implements SweepingOutput {
  String text;
}

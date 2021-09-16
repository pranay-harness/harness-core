package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

@OwnedBy(CDC)
@TargetModule(HarnessModule._815_CG_TRIGGERS)
@Data
@Builder
public class CustomPayloadExpression {
  String expression;
  String value;
}

package software.wings.service.impl.instance;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(PL)
@TargetModule(HarnessModule._957_CG_BEANS)
public class Status {
  boolean success;
  String errorMessage;
  boolean retryable;
}

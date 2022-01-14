package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@TargetModule(HarnessModule._957_CG_BEANS)
public class ApprovalAuthorization {
  private boolean authorized;
}

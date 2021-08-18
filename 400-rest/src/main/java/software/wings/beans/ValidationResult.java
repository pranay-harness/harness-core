package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TargetModule(HarnessModule._957_CG_BEANS)
@OwnedBy(CDC)
@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class ValidationResult {
  private boolean valid;
  private String errorMessage;
}

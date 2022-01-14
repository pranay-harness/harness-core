package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(CDC)
@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class ValidationResult {
  private boolean valid;
  private String errorMessage;
}

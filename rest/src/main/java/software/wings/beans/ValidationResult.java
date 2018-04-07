package software.wings.beans;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class ValidationResult {
  private boolean valid;
  private String errorMessage;
}

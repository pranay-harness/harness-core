package io.harness.shell;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@AllArgsConstructor
public class ShellExecutionRequest {
  @NotEmpty private String scriptString;
  private String workingDirectory;
  @Default private long timeoutSeconds = 60;
}
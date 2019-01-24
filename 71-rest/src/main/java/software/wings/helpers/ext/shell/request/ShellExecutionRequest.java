package software.wings.helpers.ext.shell.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@AllArgsConstructor
@Builder
public class ShellExecutionRequest {
  @NotEmpty private String scriptString;
  private String workingDirectory;
  @Builder.Default private long timeoutMillis = 60000;
}
package software.wings.core.local.executors;

import io.harness.shell.ScriptType;

import software.wings.core.ssh.executors.ScriptExecutionContext;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
public class ShellExecutorConfig implements ScriptExecutionContext {
  @NotEmpty private String accountId;
  @NotEmpty private String appId;
  @NotEmpty private String executionId;
  @NotEmpty private String commandUnitName;
  private String workingDirectory;
  private final Map<String, String> environment;
  private String kubeConfigContent;
  private ScriptType scriptType;
}

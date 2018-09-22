package software.wings.core.local.executors;

import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.Map;

@Data
@Builder
public class ShellExecutorConfig {
  @NotEmpty private String accountId;
  @NotEmpty private String appId;
  @NotEmpty private String executionId;
  @NotEmpty private String commandUnitName;
  private String workingDirectory;
  private final Map<String, String> environment;
  private String kubeConfigContent;
}

package software.wings.beans.command;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The type Code deploy params.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
public class CodeDeployParams {
  private String applicationName;
  private String deploymentConfigurationName;
  private String deploymentGroupName;
  private String region;
  private String bucket;
  private String key;
  private String bundleType;
  private boolean ignoreApplicationStopFailures;
  private boolean enableAutoRollback;
  private List<String> autoRollbackConfigurations;
  private String fileExistsBehavior;
  private int timeout;
}

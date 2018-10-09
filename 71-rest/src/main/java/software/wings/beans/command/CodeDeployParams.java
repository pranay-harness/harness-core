package software.wings.beans.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The type Code deploy params.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

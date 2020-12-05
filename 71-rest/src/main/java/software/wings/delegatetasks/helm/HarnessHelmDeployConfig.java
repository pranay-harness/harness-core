package software.wings.delegatetasks.helm;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * This is generated from harness specific section of
 * value.yaml read from git repo cpnfigured by client
 *
 * harness:
 *    helm:
 *        chart:
 *             url: google.com
 *        timeout:10  // this is a pseudo field
 */
@Data
@Builder
@AllArgsConstructor
@TargetModule(Module._930_DELEGATE_TASKS)
public class HarnessHelmDeployConfig {
  private HelmDeployChartSpec helmDeployChartSpec;
}

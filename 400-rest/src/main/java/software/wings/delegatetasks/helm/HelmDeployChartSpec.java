package software.wings.delegatetasks.helm;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * This is generated from harness specific section of
 * value.yaml read from git repo cpnfigured by client
 * Harness
 *   helm:
 *      chart:
 *         url: www.google.com
 *         name: name
 *         version: 1.0.1
 *
 */
@Data
@Builder
@AllArgsConstructor
@TargetModule(Module._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class HelmDeployChartSpec {
  private String url;
  private String name;
  private String version;
}

package software.wings.delegatetasks.helm;

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
@Builder
@AllArgsConstructor
@Data
public class HarnessHelmDeployConfig {
  private HelmDeployChartSpec helmDeployChartSpec;
}

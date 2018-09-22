package software.wings.delegatetasks.helm;

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
@Builder
@AllArgsConstructor
@Data
public class HelmDeployChartSpec {
  private String url;
  private String name;
  private String version;
}

package software.wings.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * This is used as request for capturing aws deployment deploymentInfo.
 * @author rktummala on 02/04/18
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class AwsCodeDeployDeploymentInfo extends DeploymentInfo {
  private String deploymentGroupName;
  private String applicationName;
  private String deploymentId;
  // This is Revision Localtion, S3 artifact link
  private String key;
}

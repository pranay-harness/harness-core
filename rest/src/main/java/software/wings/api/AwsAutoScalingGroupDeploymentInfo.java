package software.wings.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * This is used as request for capturing auto scaling group name and relevant deploymentInfo.
 * @author rktummala on 02/04/18
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class AwsAutoScalingGroupDeploymentInfo extends DeploymentInfo {
  private String autoScalingGroupName;
}

package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

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
@OwnedBy(CDP)
public class AwsAutoScalingGroupDeploymentInfo extends DeploymentInfo {
  private String autoScalingGroupName;
}

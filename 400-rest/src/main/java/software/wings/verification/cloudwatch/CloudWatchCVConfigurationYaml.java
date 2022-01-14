package software.wings.verification.cloudwatch;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.service.impl.cloudwatch.CloudWatchMetric;
import software.wings.verification.CVConfigurationYaml;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The type Yaml.
 */
@TargetModule(HarnessModule._955_CG_YAML)
@Data
@JsonPropertyOrder({"type", "harnessApiVersion"})
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CV)
public final class CloudWatchCVConfigurationYaml extends CVConfigurationYaml {
  private Map<String, List<CloudWatchMetric>> loadBalancerMetrics;
  private Map<String, List<CloudWatchMetric>> ecsMetrics;
  private Map<String, List<CloudWatchMetric>> lambdaFunctionsMetrics;
  private List<String> ec2InstanceNames;
  private List<CloudWatchMetric> ec2Metrics;
  private String region;
}

package software.wings.verification.cloudwatch;

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
@Data
@JsonPropertyOrder({"type", "harnessApiVersion"})
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class CloudWatchCVConfigurationYaml extends CVConfigurationYaml {
  private Map<String, List<CloudWatchMetric>> loadBalancerMetrics;
  private Map<String, List<CloudWatchMetric>> ecsMetrics;
  private Map<String, List<CloudWatchMetric>> lambdaFunctionsMetrics;
  private List<String> ec2InstanceNames;
  private List<CloudWatchMetric> ec2Metrics;
  private String region;
}

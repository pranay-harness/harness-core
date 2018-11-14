package software.wings.verification.cloudwatch;

import com.github.reinert.jjschema.Attributes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.service.impl.cloudwatch.CloudWatchMetric;
import software.wings.verification.CVConfiguration;

import java.util.List;
import java.util.Map;

/**
 * Cloud watch configuration for CV 24x7
 * Created by Pranjal on 11/08/2018
 */
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloudWatchCVServiceConfiguration extends CVConfiguration {
  @Attributes(title = "ELB Metrics") private Map<String, List<CloudWatchMetric>> loadBalancerMetrics;

  @Attributes(title = "EC2 Metrics") private List<CloudWatchMetric> ec2Metrics;

  @Attributes(title = "Region") private String region;
}

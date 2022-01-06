/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.verification.cloudwatch;

import software.wings.service.impl.cloudwatch.CloudWatchMetric;
import software.wings.verification.CVConfiguration;

import com.github.reinert.jjschema.Attributes;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Cloud watch configuration for CV 24x7
 * Created by Pranjal on 11/08/2018
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CloudWatchCVServiceConfiguration extends CVConfiguration {
  @Attributes(title = "ELB Metrics") private Map<String, List<CloudWatchMetric>> loadBalancerMetrics;

  @Attributes(title = "ECS Metrics") private Map<String, List<CloudWatchMetric>> ecsMetrics;

  @Attributes(title = "Lambda Functions Metrics") private Map<String, List<CloudWatchMetric>> lambdaFunctionsMetrics;

  @Attributes(title = "EC2 Instance Names") private List<String> ec2InstanceNames;

  @Attributes(title = "EC2 Metrics") private List<CloudWatchMetric> ec2Metrics;

  @Attributes(title = "Region") private String region;

  @Override
  public CVConfiguration deepCopy() {
    CloudWatchCVServiceConfiguration clonedConfig = new CloudWatchCVServiceConfiguration();
    super.copy(clonedConfig);
    clonedConfig.setEc2InstanceNames(this.getEc2InstanceNames());
    clonedConfig.setEc2Metrics(this.getEc2Metrics());
    clonedConfig.setEcsMetrics(this.getEcsMetrics());
    clonedConfig.setLambdaFunctionsMetrics(this.getLambdaFunctionsMetrics());
    clonedConfig.setLoadBalancerMetrics(this.getLoadBalancerMetrics());
    clonedConfig.setRegion(this.getRegion());
    return clonedConfig;
  }
}

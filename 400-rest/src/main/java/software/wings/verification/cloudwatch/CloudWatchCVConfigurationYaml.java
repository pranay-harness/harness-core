/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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

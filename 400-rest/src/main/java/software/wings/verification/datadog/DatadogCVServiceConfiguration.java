/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.verification.datadog;

import software.wings.sm.states.DatadogState.Metric;
import software.wings.verification.CVConfiguration;

import com.github.reinert.jjschema.Attributes;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author Vaibhav Tulsyan
 * 16/Oct/2018
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class DatadogCVServiceConfiguration extends CVConfiguration {
  @Attributes(required = false, title = "Datadog Service Name") private String datadogServiceName;

  // comma separated metrics
  @Attributes(required = false, title = "Docker Metrics") private Map<String, String> dockerMetrics;
  // comma separated metrics
  @Attributes(required = false, title = "ECS Metrics") private Map<String, String> ecsMetrics;
  @Attributes(required = false, title = "Custom Metrics") private Map<String, Set<Metric>> customMetrics;

  @Override
  public CVConfiguration deepCopy() {
    DatadogCVServiceConfiguration clonedConfig = new DatadogCVServiceConfiguration();
    super.copy(clonedConfig);
    clonedConfig.setDatadogServiceName(this.getDatadogServiceName());
    clonedConfig.setDockerMetrics(this.getDockerMetrics());
    clonedConfig.setEcsMetrics(this.getEcsMetrics());
    clonedConfig.setCustomMetrics(this.getCustomMetrics());
    return clonedConfig;
  }
}

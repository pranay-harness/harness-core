/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CDP)
public final class AwsAmiInfraMappingYaml extends YamlWithComputeProvider {
  private String region;
  private String autoScalingGroupName;
  private List<String> classicLoadBalancers;
  private List<String> targetGroupArns;
  private String hostNameConvention;
  private List<String> stageClassicLoadBalancers;
  private List<String> stageTargetGroupArns;
  private AmiDeploymentType amiDeploymentType;
  private String spotinstElastiGroupJson;
  private String spotinstCloudProviderName;

  @Builder
  public AwsAmiInfraMappingYaml(String type, String harnessApiVersion, String computeProviderType, String serviceName,
      String infraMappingType, String deploymentType, String computeProviderName, String region,
      String autoScalingGroupName, List<String> classicLoadBalancers, List<String> targetGroupArns,
      String hostNameConvention, List<String> stageClassicLoadBalancers, List<String> stageTargetGroupArns,
      Map<String, Object> blueprints, AmiDeploymentType amiDeploymentType, String spotinstElastiGroupJson,
      String spotinstCloudProviderName) {
    super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
        computeProviderName, blueprints);
    this.region = region;
    this.autoScalingGroupName = autoScalingGroupName;
    this.classicLoadBalancers = classicLoadBalancers;
    this.targetGroupArns = targetGroupArns;
    this.hostNameConvention = hostNameConvention;
    this.stageClassicLoadBalancers = stageClassicLoadBalancers;
    this.stageTargetGroupArns = stageTargetGroupArns;
    this.amiDeploymentType = amiDeploymentType;
    this.spotinstElastiGroupJson = spotinstElastiGroupJson;
    this.spotinstCloudProviderName = spotinstCloudProviderName;
  }
}

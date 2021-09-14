/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.cloudprovider.aws;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import software.wings.cloudprovider.ClusterConfiguration;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by anubhaw on 12/29/16.
 */
@Data
@NoArgsConstructor
@OwnedBy(CDP)
public class AwsClusterConfiguration extends ClusterConfiguration {
  private String serviceDefinition;
  private String launcherConfiguration;
  private String vpcZoneIdentifiers;
  private List<String> availabilityZones;
  private String autoScalingGroupName;

  @Builder
  public AwsClusterConfiguration(Integer size, String name, String serviceDefinition, String launcherConfiguration,
      String vpcZoneIdentifiers, List<String> availabilityZones, String autoScalingGroupName) {
    super(size, name);
    this.serviceDefinition = serviceDefinition;
    this.launcherConfiguration = launcherConfiguration;
    this.vpcZoneIdentifiers = vpcZoneIdentifiers;
    this.availabilityZones = availabilityZones;
    this.autoScalingGroupName = autoScalingGroupName;
  }
}

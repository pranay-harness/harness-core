/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.infrastructure.instance.info;

import com.amazonaws.services.ec2.model.Instance;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author rktummala on 01/30/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AutoScalingGroupInstanceInfo extends AbstractEc2InstanceInfo {
  private String autoScalingGroupName;

  @Builder
  public AutoScalingGroupInstanceInfo(
      String hostId, String hostName, String hostPublicDns, Instance ec2Instance, String autoScalingGroupName) {
    super(hostId, hostName, hostPublicDns, ec2Instance);
    this.autoScalingGroupName = autoScalingGroupName;
  }
}

/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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

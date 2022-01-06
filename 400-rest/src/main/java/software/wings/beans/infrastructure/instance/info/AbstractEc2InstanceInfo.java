/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.infrastructure.instance.info;

import com.amazonaws.services.ec2.model.Instance;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author rktummala on 08/25/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractEc2InstanceInfo extends HostInstanceInfo {
  private Instance ec2Instance;

  public AbstractEc2InstanceInfo(String hostId, String hostName, String hostPublicDns, Instance ec2Instance) {
    super(hostId, hostName, hostPublicDns);
    this.ec2Instance = ec2Instance;
  }
}

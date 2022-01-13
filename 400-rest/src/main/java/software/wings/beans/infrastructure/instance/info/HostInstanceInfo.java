/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.infrastructure.instance.info;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author rktummala on 09/05/17
 */
@Data
@EqualsAndHashCode(callSuper = false)
public abstract class HostInstanceInfo extends InstanceInfo {
  private String hostId;
  private String hostName;
  private String hostPublicDns;

  public HostInstanceInfo(String hostId, String hostName, String hostPublicDns) {
    this.hostId = hostId;
    this.hostName = hostName;
    this.hostPublicDns = hostPublicDns;
  }
}

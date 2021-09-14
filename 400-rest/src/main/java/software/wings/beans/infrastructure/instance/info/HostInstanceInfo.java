/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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

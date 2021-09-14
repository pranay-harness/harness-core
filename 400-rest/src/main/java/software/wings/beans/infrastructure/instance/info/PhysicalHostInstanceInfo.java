/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.infrastructure.instance.info;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author rktummala on 09/05/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PhysicalHostInstanceInfo extends HostInstanceInfo {
  private final Map<String, Object> properties;

  @Builder
  public PhysicalHostInstanceInfo(
      String hostId, String hostName, String hostPublicDns, Map<String, Object> properties) {
    super(hostId, hostName, hostPublicDns);
    this.properties = properties;
  }
}

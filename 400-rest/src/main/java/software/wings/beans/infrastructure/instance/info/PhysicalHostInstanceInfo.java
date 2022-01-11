/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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

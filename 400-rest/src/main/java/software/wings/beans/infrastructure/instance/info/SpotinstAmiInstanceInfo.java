/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.infrastructure.instance.info;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import com.amazonaws.services.ec2.model.Instance;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CDP)
public class SpotinstAmiInstanceInfo extends AbstractEc2InstanceInfo {
  private String elastigroupId;

  @Builder
  public SpotinstAmiInstanceInfo(
      String hostId, String hostName, String hostPublicDns, Instance ec2Instance, String elastigroupId) {
    super(hostId, hostName, hostPublicDns, ec2Instance);
    this.elastigroupId = elastigroupId;
  }
}

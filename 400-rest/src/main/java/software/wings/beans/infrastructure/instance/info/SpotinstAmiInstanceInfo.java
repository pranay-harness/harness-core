/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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

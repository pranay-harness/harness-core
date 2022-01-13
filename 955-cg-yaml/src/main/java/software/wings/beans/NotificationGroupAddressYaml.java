/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.yaml.BaseYaml;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Yaml representation of addressesByChannelType in NotificationGroup.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(DX)
public final class NotificationGroupAddressYaml extends BaseYaml {
  private String channelType;
  private List<String> addresses;

  @Builder
  public NotificationGroupAddressYaml(String channelType, List<String> addresses) {
    this.channelType = channelType;
    this.addresses = addresses;
  }
}

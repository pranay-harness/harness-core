/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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

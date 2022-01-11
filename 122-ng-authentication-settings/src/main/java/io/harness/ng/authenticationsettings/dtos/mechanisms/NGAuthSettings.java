/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.authenticationsettings.dtos.mechanisms;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.account.AuthenticationMechanism;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.PL)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, property = "settingsType", include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true)
public abstract class NGAuthSettings {
  @JsonProperty("settingsType") protected AuthenticationMechanism settingsType;

  public NGAuthSettings(AuthenticationMechanism settingsType) {
    this.settingsType = settingsType;
  }

  public abstract AuthenticationMechanism getSettingsType();
}

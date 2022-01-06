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

import software.wings.beans.loginSettings.LoginSettings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("USERNAME_PASSWORD")
@OwnedBy(HarnessTeam.PL)
public class UsernamePasswordSettings extends NGAuthSettings {
  @NotNull @Valid private LoginSettings loginSettings;

  public UsernamePasswordSettings(@JsonProperty("loginSettings") LoginSettings loginSettings) {
    super(AuthenticationMechanism.USER_PASSWORD);
    this.loginSettings = loginSettings;
  }

  @Override
  public AuthenticationMechanism getSettingsType() {
    return AuthenticationMechanism.USER_PASSWORD;
  }
}

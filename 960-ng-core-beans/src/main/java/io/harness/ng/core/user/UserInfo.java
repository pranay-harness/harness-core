/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.user;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.GatewayAccountRequestDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(PL)
public class UserInfo {
  @EqualsAndHashCode.Include String uuid;
  String name;
  String email;
  String token;
  String defaultAccountId;
  String intent;
  List<GatewayAccountRequestDTO> accounts;
  boolean admin;
  boolean twoFactorAuthenticationEnabled;
  boolean emailVerified;
  @Getter(value = AccessLevel.PRIVATE) boolean locked;
  String signupAction;
  String edition;
  String billingFrequency;

  public boolean isLocked() {
    return this.locked;
  }
}

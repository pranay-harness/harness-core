/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.user;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(PL)
public enum PasswordChangeResponse {
  PASSWORD_CHANGED,
  INCORRECT_CURRENT_PASSWORD,
  PASSWORD_STRENGTH_VIOLATED;
}

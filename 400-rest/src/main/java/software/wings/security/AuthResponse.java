/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author rktummala on 02/12/18
 */

@OwnedBy(PL)
@Data
@Builder
@EqualsAndHashCode
public class AuthResponse {
  public enum Status { SUCCESS, FAILURE }

  private Status status;
  private String message;
}

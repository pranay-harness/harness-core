/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
// TODO: this class seems pointless copy of EmbeddedUser
public class EmbeddedUserDetails {
  private String uuid;
  private String name;
  private String email;
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.commandlibrary.server.beans;

import io.harness.commandlibrary.server.utils.JsonSerializable;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ServiceSecretConfig implements JsonSerializable {
  String managerToCommandLibraryServiceSecret;
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.licensing;

import io.harness.ModuleType;
import io.harness.licensing.interfaces.clients.ModuleLicenseClient;
import io.harness.licensing.mappers.LicenseObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class ModuleLicenseRegistrar {
  ModuleType moduleType;

  Class<? extends LicenseObjectMapper> objectMapper;

  Class<? extends ModuleLicenseClient> moduleLicenseClient;
}

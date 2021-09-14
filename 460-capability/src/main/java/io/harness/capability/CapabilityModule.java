/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.capability;

import io.harness.capability.service.CapabilityService;
import io.harness.capability.service.CapabilityServiceImpl;

import com.google.inject.AbstractModule;

public class CapabilityModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(CapabilityService.class).to(CapabilityServiceImpl.class);
  }
}

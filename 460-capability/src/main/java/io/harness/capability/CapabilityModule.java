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
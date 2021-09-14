/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.api.DelegateDetailsService;
import io.harness.ng.core.api.DelegateProfileManagerNgService;
import io.harness.ng.core.api.impl.DelegateDetailsServiceImpl;
import io.harness.ng.core.api.impl.DelegateProfileManagerNgServiceImpl;

import com.google.inject.AbstractModule;

@OwnedBy(DEL)
public class DelegateServiceModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(DelegateDetailsService.class).to(DelegateDetailsServiceImpl.class);
    bind(DelegateProfileManagerNgService.class).to(DelegateProfileManagerNgServiceImpl.class);
  }
}

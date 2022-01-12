/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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

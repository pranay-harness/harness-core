/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.app;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.service.impl.SSOServiceImpl;
import software.wings.service.impl.SSOSettingServiceImpl;
import software.wings.service.intfc.SSOService;
import software.wings.service.intfc.SSOSettingService;

import com.google.inject.AbstractModule;

@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
public class SSOModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(SSOSettingService.class).to(SSOSettingServiceImpl.class);
    bind(SSOService.class).to(SSOServiceImpl.class);
  }
}

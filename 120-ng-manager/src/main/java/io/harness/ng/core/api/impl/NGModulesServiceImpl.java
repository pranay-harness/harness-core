/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.remote.client.RestClientUtils.getResponse;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.modules.remote.ModulesClient;
import io.harness.ng.core.api.NGModulesService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class NGModulesServiceImpl implements NGModulesService {
  private final ModulesClient modulesClient;

  @Override
  public List<ModuleType> getEnabledModules(String accountIdentifier) {
    return getResponse(modulesClient.getEnabledModules(accountIdentifier));
  }
}

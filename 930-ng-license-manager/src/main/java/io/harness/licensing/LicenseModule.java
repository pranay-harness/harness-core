/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.licensing;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.interfaces.ModuleLicenseImpl;
import io.harness.licensing.interfaces.ModuleLicenseInterface;
import io.harness.licensing.interfaces.clients.ModuleLicenseClient;
import io.harness.licensing.mappers.LicenseObjectConverter;
import io.harness.licensing.mappers.LicenseObjectMapper;
import io.harness.licensing.services.DefaultLicenseServiceImpl;
import io.harness.licensing.services.LicenseService;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

@OwnedBy(HarnessTeam.GTM)
public class LicenseModule extends AbstractModule {
  private static LicenseModule instance;

  public static LicenseModule getInstance() {
    if (instance == null) {
      instance = new LicenseModule();
    }
    return instance;
  }

  private LicenseModule() {}

  @Override
  protected void configure() {
    MapBinder<ModuleType, LicenseObjectMapper> objectMapperMapBinder =
        MapBinder.newMapBinder(binder(), ModuleType.class, LicenseObjectMapper.class);
    MapBinder<ModuleType, ModuleLicenseClient> interfaceMapBinder =
        MapBinder.newMapBinder(binder(), ModuleType.class, ModuleLicenseClient.class);

    for (ModuleType moduleType : ModuleLicenseRegistrarFactory.getSupportedModuleTypes()) {
      objectMapperMapBinder.addBinding(moduleType).to(ModuleLicenseRegistrarFactory.getLicenseObjectMapper(moduleType));
      interfaceMapBinder.addBinding(moduleType).to(ModuleLicenseRegistrarFactory.getModuleLicenseClient(moduleType));
    }

    bind(LicenseObjectConverter.class);
    bind(ModuleLicenseInterface.class).to(ModuleLicenseImpl.class);
    bind(LicenseService.class).to(DefaultLicenseServiceImpl.class);
  }
}

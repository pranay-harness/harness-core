package io.harness.licensing;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.interfaces.ModuleLicenseInterface;
import io.harness.licensing.interfaces.ModuleLicenseInterfaceImpl;
import io.harness.licensing.interfaces.clients.ModuleLicenseClient;
import io.harness.licensing.mappers.LicenseObjectMapper;
import io.harness.licensing.mappers.LicenseObjectMapperImpl;
import io.harness.licensing.services.DefaultLicenseServiceImpl;
import io.harness.licensing.services.LicenseService;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

@OwnedBy(HarnessTeam.GTM)
public class LicenseModule extends AbstractModule {
  @Override
  protected void configure() {
    MapBinder<ModuleType, LicenseObjectMapper> objectMapperMapBinder =
        MapBinder.newMapBinder(binder(), ModuleType.class, LicenseObjectMapper.class);
    MapBinder<ModuleType, ModuleLicenseClient> interfaceMapBinder =
        MapBinder.newMapBinder(binder(), ModuleType.class, ModuleLicenseClient.class);

    for (ModuleType moduleType : ModuleType.values()) {
      objectMapperMapBinder.addBinding(moduleType).to(ModuleLicenseRegistrarFactory.getLicenseObjectMapper(moduleType));
      interfaceMapBinder.addBinding(moduleType).to(ModuleLicenseRegistrarFactory.getModuleLicenseClient(moduleType));
    }

    bind(LicenseObjectMapper.class).to(LicenseObjectMapperImpl.class);
    bind(ModuleLicenseInterface.class).to(ModuleLicenseInterfaceImpl.class);
    bind(LicenseService.class).to(DefaultLicenseServiceImpl.class);
  }
}

package io.harness.licensing;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.interfaces.ModuleLicenseInterface;
import io.harness.licensing.interfaces.ModuleLicenseInterfaceImpl;
import io.harness.licensing.interfaces.clients.ModuleLicenseClient;
import io.harness.licensing.mappers.AccountLicenseMapper;
import io.harness.licensing.mappers.AccountLicenseMapperImpl;
import io.harness.licensing.mappers.LicenseObjectConverter;
import io.harness.licensing.mappers.LicenseObjectMapper;
import io.harness.licensing.mappers.LicenseTransactionConverter;
import io.harness.licensing.mappers.transactions.LicenseTransactionMapper;
import io.harness.licensing.scheduler.AccountLicenseCheckHandler;
import io.harness.licensing.scheduler.AccountLicenseCheckHandlerImpl;
import io.harness.licensing.scheduler.LicenseCheckProcessor;
import io.harness.licensing.services.AccountLicenseService;
import io.harness.licensing.services.AccountLicenseServiceImpl;
import io.harness.licensing.services.DefaultLicenseServiceImpl;
import io.harness.licensing.services.LicenseService;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

@OwnedBy(HarnessTeam.GTM)
public class LicenseModule extends AbstractModule {
  private static LicenseModule instance;

  static LicenseModule getInstance() {
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
    MapBinder<ModuleType, LicenseCheckProcessor> licenseCheckMapBinder =
        MapBinder.newMapBinder(binder(), ModuleType.class, LicenseCheckProcessor.class);
    MapBinder<ModuleType, LicenseTransactionMapper> transactionMapperMapBinder =
        MapBinder.newMapBinder(binder(), ModuleType.class, LicenseTransactionMapper.class);

    for (ModuleType moduleType : ModuleType.values()) {
      objectMapperMapBinder.addBinding(moduleType).to(ModuleLicenseRegistrarFactory.getLicenseObjectMapper(moduleType));
      interfaceMapBinder.addBinding(moduleType).to(ModuleLicenseRegistrarFactory.getModuleLicenseClient(moduleType));
      licenseCheckMapBinder.addBinding(moduleType).to(ModuleLicenseRegistrarFactory.getCheckProcessor(moduleType));
      transactionMapperMapBinder.addBinding(moduleType)
          .to(ModuleLicenseRegistrarFactory.getLicenseTransactionMapper(moduleType));
    }

    bind(LicenseObjectConverter.class);
    bind(LicenseTransactionConverter.class);
    bind(AccountLicenseMapper.class).to(AccountLicenseMapperImpl.class);
    bind(ModuleLicenseInterface.class).to(ModuleLicenseInterfaceImpl.class);
    bind(LicenseService.class).to(DefaultLicenseServiceImpl.class);
    bind(AccountLicenseService.class).to(AccountLicenseServiceImpl.class);
    bind(AccountLicenseCheckHandler.class).to(AccountLicenseCheckHandlerImpl.class);
  }
}

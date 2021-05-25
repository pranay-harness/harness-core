package io.harness.licensing;

import io.harness.licensing.interfaces.clients.ModuleLicenseClient;
import io.harness.licensing.interfaces.clients.local.CDLocalClient;
import io.harness.licensing.interfaces.clients.local.CELocalClient;
import io.harness.licensing.interfaces.clients.local.CFLocalClient;
import io.harness.licensing.interfaces.clients.local.CILocalClient;
import io.harness.licensing.interfaces.clients.local.UnsupportedClient;
import io.harness.licensing.mappers.LicenseObjectMapper;
import io.harness.licensing.mappers.modules.CDLicenseObjectMapper;
import io.harness.licensing.mappers.modules.CELicenseObjectMapper;
import io.harness.licensing.mappers.modules.CFLicenseObjectMapper;
import io.harness.licensing.mappers.modules.CILicenseObjectMapper;
import io.harness.licensing.mappers.modules.CVLicenseObjectMapper;
import io.harness.licensing.mappers.transactions.CDLicenseTransactionMapper;
import io.harness.licensing.mappers.transactions.CELicenseTransactionMapper;
import io.harness.licensing.mappers.transactions.CFLicenseTransactionMapper;
import io.harness.licensing.mappers.transactions.CILicenseTransactionMapper;
import io.harness.licensing.mappers.transactions.CVLicenseTransactionMapper;
import io.harness.licensing.mappers.transactions.LicenseTransactionMapper;
import io.harness.licensing.scheduler.LicenseCheckProcessor;
import io.harness.licensing.scheduler.modules.CDCheckProcessor;
import io.harness.licensing.scheduler.modules.CECheckProcessor;
import io.harness.licensing.scheduler.modules.CFCheckProcessor;
import io.harness.licensing.scheduler.modules.CICheckProcessor;
import io.harness.licensing.scheduler.modules.CVCheckProcessor;

import java.util.HashMap;
import java.util.Map;

public class ModuleLicenseRegistrarFactory {
  private static Map<ModuleType, ModuleLicenseRegistrar> registrar = new HashMap<>();

  private ModuleLicenseRegistrarFactory() {}

  static {
    registrar.put(ModuleType.CD,
        new ModuleLicenseRegistrar(ModuleType.CD, CDLicenseObjectMapper.class, CDLocalClient.class,
            CDCheckProcessor.class, CDLicenseTransactionMapper.class));
    registrar.put(ModuleType.CI,
        new ModuleLicenseRegistrar(ModuleType.CI, CILicenseObjectMapper.class, CILocalClient.class,
            CICheckProcessor.class, CILicenseTransactionMapper.class));
    registrar.put(ModuleType.CE,
        new ModuleLicenseRegistrar(ModuleType.CE, CELicenseObjectMapper.class, CELocalClient.class,
            CECheckProcessor.class, CELicenseTransactionMapper.class));
    registrar.put(ModuleType.CV,
        new ModuleLicenseRegistrar(ModuleType.CV, CVLicenseObjectMapper.class, UnsupportedClient.class,
            CVCheckProcessor.class, CVLicenseTransactionMapper.class));
    registrar.put(ModuleType.CF,
        new ModuleLicenseRegistrar(ModuleType.CF, CFLicenseObjectMapper.class, CFLocalClient.class,
            CFCheckProcessor.class, CFLicenseTransactionMapper.class));
  }

  public static Class<? extends LicenseObjectMapper> getLicenseObjectMapper(ModuleType moduleType) {
    return registrar.get(moduleType).getObjectMapper();
  }

  public static Class<? extends ModuleLicenseClient> getModuleLicenseClient(ModuleType moduleType) {
    return registrar.get(moduleType).getModuleLicenseClient();
  }

  public static Class<? extends LicenseCheckProcessor> getCheckProcessor(ModuleType moduleType) {
    return registrar.get(moduleType).getLicenseCheckProcessor();
  }

  public static Class<? extends LicenseTransactionMapper> getLicenseTransactionMapper(ModuleType moduleType) {
    return registrar.get(moduleType).getLicenseTransactionMapper();
  }
}

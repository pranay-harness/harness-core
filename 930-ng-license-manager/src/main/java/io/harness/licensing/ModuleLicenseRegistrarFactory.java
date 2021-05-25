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
import io.harness.licensing.scheduler.modules.CDAggregator;
import io.harness.licensing.scheduler.modules.CEAggregator;
import io.harness.licensing.scheduler.modules.CFAggregator;
import io.harness.licensing.scheduler.modules.CIAggregator;
import io.harness.licensing.scheduler.modules.CVAggregator;
import io.harness.licensing.scheduler.modules.TransactionAggregator;

import java.util.HashMap;
import java.util.Map;

public class ModuleLicenseRegistrarFactory {
  private static Map<ModuleType, ModuleLicenseRegistrar> registrar = new HashMap<>();

  private ModuleLicenseRegistrarFactory() {}

  static {
    registrar.put(ModuleType.CD,
        new ModuleLicenseRegistrar(ModuleType.CD, CDLicenseObjectMapper.class, CDLocalClient.class, CDAggregator.class,
            CDLicenseTransactionMapper.class));
    registrar.put(ModuleType.CI,
        new ModuleLicenseRegistrar(ModuleType.CI, CILicenseObjectMapper.class, CILocalClient.class, CIAggregator.class,
            CILicenseTransactionMapper.class));
    registrar.put(ModuleType.CE,
        new ModuleLicenseRegistrar(ModuleType.CE, CELicenseObjectMapper.class, CELocalClient.class, CEAggregator.class,
            CELicenseTransactionMapper.class));
    registrar.put(ModuleType.CV,
        new ModuleLicenseRegistrar(ModuleType.CV, CVLicenseObjectMapper.class, UnsupportedClient.class,
            CVAggregator.class, CVLicenseTransactionMapper.class));
    registrar.put(ModuleType.CF,
        new ModuleLicenseRegistrar(ModuleType.CF, CFLicenseObjectMapper.class, CFLocalClient.class, CFAggregator.class,
            CFLicenseTransactionMapper.class));
  }

  public static Class<? extends LicenseObjectMapper> getLicenseObjectMapper(ModuleType moduleType) {
    return registrar.get(moduleType).getObjectMapper();
  }

  public static Class<? extends ModuleLicenseClient> getModuleLicenseClient(ModuleType moduleType) {
    return registrar.get(moduleType).getModuleLicenseClient();
  }

  public static Class<? extends TransactionAggregator> getTransactionAggregator(ModuleType moduleType) {
    return registrar.get(moduleType).getTransactionAggregator();
  }

  public static Class<? extends LicenseTransactionMapper> getLicenseTransactionMapper(ModuleType moduleType) {
    return registrar.get(moduleType).getLicenseTransactionMapper();
  }
}

package io.harness.licensing;

import io.harness.licensing.interfaces.clients.ModuleLicenseClient;
import io.harness.licensing.interfaces.clients.local.CDLocalClient;
import io.harness.licensing.interfaces.clients.local.CILocalClient;
import io.harness.licensing.interfaces.clients.local.UnsupportedClient;
import io.harness.licensing.mappers.LicenseObjectMapper;
import io.harness.licensing.mappers.modules.CDLicenseObjectMapper;
import io.harness.licensing.mappers.modules.CELicenseObjectMapper;
import io.harness.licensing.mappers.modules.CFLicenseObjectMapper;
import io.harness.licensing.mappers.modules.CILicenseObjectMapper;
import io.harness.licensing.mappers.modules.CVLicenseObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class ModuleLicenseRegistrarFactory {
  private static Map<ModuleType, ModuleLicenseRegistrar> registrar = new HashMap<>();

  private ModuleLicenseRegistrarFactory() {}

  static {
    registrar.put(
        ModuleType.CD, new ModuleLicenseRegistrar(ModuleType.CD, CDLicenseObjectMapper.class, CDLocalClient.class));
    registrar.put(
        ModuleType.CI, new ModuleLicenseRegistrar(ModuleType.CI, CILicenseObjectMapper.class, CILocalClient.class));
    registrar.put(
        ModuleType.CE, new ModuleLicenseRegistrar(ModuleType.CE, CELicenseObjectMapper.class, UnsupportedClient.class));
    registrar.put(
        ModuleType.CV, new ModuleLicenseRegistrar(ModuleType.CV, CVLicenseObjectMapper.class, UnsupportedClient.class));
    registrar.put(
        ModuleType.CF, new ModuleLicenseRegistrar(ModuleType.CF, CFLicenseObjectMapper.class, UnsupportedClient.class));
  }

  public static Class<? extends LicenseObjectMapper> getLicenseObjectMapper(ModuleType moduleType) {
    return registrar.get(moduleType).getObjectMapper();
  }

  public static Class<? extends ModuleLicenseClient> getModuleLicenseClient(ModuleType moduleType) {
    return registrar.get(moduleType).getModuleLicenseClient();
  }
}

package io.harness.licensing.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.ModuleType;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.beans.transactions.LicenseTransactionDTO;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.licensing.entities.transactions.LicenseTransaction;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.GTM)
@Singleton
public class LicenseObjectConverter {
  @Inject Map<ModuleType, LicenseObjectMapper> mapperMap;
  @Inject LicenseTransactionConverter licenseTransactionConverter;

  public <T extends ModuleLicenseDTO> T toDTO(ModuleLicense moduleLicense) {
    ModuleType moduleType = moduleLicense.getModuleType();
    ModuleLicenseDTO moduleLicenseDTO = mapperMap.get(moduleType).toDTO(moduleLicense);
    moduleLicenseDTO.setAccountIdentifier(moduleLicense.getAccountIdentifier());
    moduleLicenseDTO.setModuleType(moduleLicense.getModuleType());
    moduleLicenseDTO.setEdition(moduleLicense.getEdition());
    moduleLicenseDTO.setLicenseType(moduleLicense.getLicenseType());
    moduleLicenseDTO.setStatus(moduleLicense.getStatus());
    if (moduleLicense.getTransactions() != null) {
      moduleLicenseDTO.setTransactions(moduleLicense.getTransactions()
                                           .stream()
                                           .map(l -> licenseTransactionConverter.<LicenseTransactionDTO>toDTO(l))
                                           .collect(Collectors.toList()));
    } else {
      moduleLicenseDTO.setTransactions(Lists.newArrayList());
    }
    return (T) moduleLicenseDTO;
  }

  public <T extends ModuleLicense> T toEntity(ModuleLicenseDTO moduleLicenseDTO) {
    ModuleType moduleType = moduleLicenseDTO.getModuleType();
    ModuleLicense moduleLicense = mapperMap.get(moduleType).toEntity(moduleLicenseDTO);
    moduleLicense.setAccountIdentifier(moduleLicenseDTO.getAccountIdentifier());
    moduleLicense.setModuleType(moduleLicenseDTO.getModuleType());
    moduleLicense.setEdition(moduleLicenseDTO.getEdition());
    moduleLicense.setLicenseType(moduleLicenseDTO.getLicenseType());
    moduleLicense.setStatus(moduleLicenseDTO.getStatus());
    if (moduleLicenseDTO.getTransactions() != null) {
      moduleLicense.setTransactions(moduleLicenseDTO.getTransactions()
                                        .stream()
                                        .map(l -> licenseTransactionConverter.<LicenseTransaction>toEntity(l))
                                        .collect(Collectors.toList()));
    } else {
      moduleLicense.setTransactions(Lists.newArrayList());
    }
    return (T) moduleLicense;
  }
}

package io.harness.licensing.mappers;

import io.harness.licensing.ModuleType;
import io.harness.licensing.beans.modules.AccountLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.entities.account.AccountLicense;
import io.harness.licensing.entities.modules.ModuleLicense;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class AccountLicenseMapperImpl implements AccountLicenseMapper {
  @Inject LicenseObjectConverter licenseObjectConverter;
  @Override
  public AccountLicenseDTO toDTO(AccountLicense accountLicense) {
    Map<ModuleType, ModuleLicenseDTO> moduleLicenseDTOMap;
    if (accountLicense.getModuleLicenses() != null) {
      moduleLicenseDTOMap = accountLicense.getModuleLicenses().entrySet().stream().collect(
          Collectors.toMap(Map.Entry::getKey, entry -> licenseObjectConverter.toDTO(entry.getValue())));
    } else {
      moduleLicenseDTOMap = new HashMap<>();
    }

    return AccountLicenseDTO.builder()
        .uuid(accountLicense.getUuid())
        .accountId(accountLicense.getAccountIdentifier())
        .moduleLicenses(moduleLicenseDTOMap)
        .allInactive(accountLicense.isAllInactive())
        .createdAt(accountLicense.getCreatedAt())
        .lastUpdatedAt(accountLicense.getLastUpdatedAt())
        .build();
  }

  @Override
  public AccountLicense toEntity(AccountLicenseDTO accountLicenseDTO) {
    Map<ModuleType, ModuleLicense> moduleLicenseMap;
    if (accountLicenseDTO.getModuleLicenses() != null) {
      moduleLicenseMap = accountLicenseDTO.getModuleLicenses().entrySet().stream().collect(
          Collectors.toMap(Map.Entry::getKey, entry -> licenseObjectConverter.toEntity(entry.getValue())));
    } else {
      moduleLicenseMap = new HashMap<>();
    }
    return AccountLicense.builder()
        .uuid(accountLicenseDTO.getUuid())
        .accountIdentifier(accountLicenseDTO.getAccountId())
        .moduleLicenses(moduleLicenseMap)
        .build();
  }
}

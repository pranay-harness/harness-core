package io.harness.licensing.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.ModuleType;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.entities.modules.ModuleLicense;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;

@OwnedBy(HarnessTeam.GTM)
@Singleton
public class LicenseObjectMapperImpl implements LicenseObjectMapper {
  @Inject Map<ModuleType, LicenseObjectMapper> mapperMap;
  @Override
  public ModuleLicenseDTO toDTO(ModuleLicense moduleLicense) {
    ModuleType moduleType = moduleLicense.getModuleType();
    ModuleLicenseDTO moduleLicenseDTO = mapperMap.get(moduleType).toDTO(moduleLicense);
    moduleLicenseDTO.setId(moduleLicense.getId());
    moduleLicenseDTO.setAccountIdentifier(moduleLicense.getAccountIdentifier());
    moduleLicenseDTO.setModuleType(moduleLicense.getModuleType());
    moduleLicenseDTO.setEdition(moduleLicense.getEdition());
    moduleLicenseDTO.setLicenseType(moduleLicense.getLicenseType());
    moduleLicenseDTO.setStartTime(moduleLicense.getStartTime());
    moduleLicenseDTO.setExpiryTime(moduleLicense.getExpiryTime());
    moduleLicenseDTO.setStatus(moduleLicense.getStatus());
    moduleLicenseDTO.setCreatedAt(moduleLicense.getCreatedAt());
    moduleLicenseDTO.setLastModifiedAt(moduleLicense.getLastUpdatedAt());
    return moduleLicenseDTO;
  }

  @Override
  public ModuleLicense toEntity(ModuleLicenseDTO moduleLicenseDTO) {
    ModuleType moduleType = moduleLicenseDTO.getModuleType();
    ModuleLicense moduleLicense = mapperMap.get(moduleType).toEntity(moduleLicenseDTO);
    moduleLicense.setId(moduleLicenseDTO.getId());
    moduleLicense.setAccountIdentifier(moduleLicenseDTO.getAccountIdentifier());
    moduleLicense.setModuleType(moduleLicenseDTO.getModuleType());
    moduleLicense.setEdition(moduleLicenseDTO.getEdition());
    moduleLicense.setLicenseType(moduleLicenseDTO.getLicenseType());
    moduleLicense.setStartTime(moduleLicenseDTO.getStartTime());
    moduleLicense.setExpiryTime(moduleLicenseDTO.getExpiryTime());
    moduleLicense.setStatus(moduleLicenseDTO.getStatus());
    return moduleLicense;
  }
}

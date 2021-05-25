package io.harness.licensing.mappers;

import io.harness.licensing.ModuleType;
import io.harness.licensing.beans.transactions.LicenseTransactionDTO;
import io.harness.licensing.entities.transactions.LicenseTransaction;
import io.harness.licensing.mappers.transactions.LicenseTransactionMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;

@Singleton
public class LicenseTransactionConverter {
  @Inject Map<ModuleType, LicenseTransactionMapper> mapperMap;

  public <T extends LicenseTransactionDTO> T toDTO(LicenseTransaction entity) {
    LicenseTransactionDTO dto = mapperMap.get(entity.getModuleType()).toDTO(entity);
    dto.setUuid(entity.getUuid());
    dto.setAccountIdentifier(entity.getAccountIdentifier());
    dto.setEdition(entity.getEdition());
    dto.setModuleType(entity.getModuleType());
    dto.setStatus(entity.getStatus());
    dto.setLicenseType(entity.getLicenseType());
    dto.setStartTime(entity.getStartTime());
    dto.setExpiryTime(entity.getExpiryTime());
    dto.setCreatedAt(entity.getCreatedAt());
    dto.setLastModifiedAt(entity.getLastUpdatedAt());
    return (T) dto;
  }

  public <T extends LicenseTransaction> T toEntity(LicenseTransactionDTO dto) {
    LicenseTransaction entity = mapperMap.get(dto.getModuleType()).toEntity(dto);
    entity.setUuid(dto.getUuid());
    entity.setAccountIdentifier(dto.getAccountIdentifier());
    entity.setEdition(dto.getEdition());
    entity.setModuleType(dto.getModuleType());
    entity.setLicenseType(dto.getLicenseType());
    entity.setStatus(dto.getStatus());
    entity.setStartTime(dto.getStartTime());
    entity.setExpiryTime(dto.getExpiryTime());
    entity.setCreatedAt(dto.getCreatedAt());
    return (T) entity;
  }
}

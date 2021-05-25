package io.harness.licensing.mappers.transactions;

import io.harness.licensing.beans.transactions.CFLicenseTransactionDTO;
import io.harness.licensing.entities.transactions.modules.CFLicenseTransaction;

public class CFLicenseTransactionMapper
    implements LicenseTransactionMapper<CFLicenseTransaction, CFLicenseTransactionDTO> {
  @Override
  public CFLicenseTransactionDTO toDTO(CFLicenseTransaction entity) {
    return CFLicenseTransactionDTO.builder()
        .featureFlagUnit(entity.getFeatureFlagUnit())
        .clientMAU(entity.getClientMAU())
        .build();
  }

  @Override
  public CFLicenseTransaction toEntity(CFLicenseTransactionDTO dto) {
    return CFLicenseTransaction.builder()
        .featureFlagUnit(dto.getFeatureFlagUnit())
        .clientMAU(dto.getClientMAU())
        .build();
  }
}

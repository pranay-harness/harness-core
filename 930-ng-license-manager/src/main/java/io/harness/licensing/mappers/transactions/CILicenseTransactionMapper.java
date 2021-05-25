package io.harness.licensing.mappers.transactions;

import io.harness.licensing.beans.transactions.CILicenseTransactionDTO;
import io.harness.licensing.entities.transactions.modules.CILicenseTransaction;

public class CILicenseTransactionMapper
    implements LicenseTransactionMapper<CILicenseTransaction, CILicenseTransactionDTO> {
  @Override
  public CILicenseTransactionDTO toDTO(CILicenseTransaction entity) {
    return CILicenseTransactionDTO.builder().developers(entity.getDevelopers()).build();
  }

  @Override
  public CILicenseTransaction toEntity(CILicenseTransactionDTO dto) {
    return CILicenseTransaction.builder().developers(dto.getDevelopers()).build();
  }
}

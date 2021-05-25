package io.harness.licensing.mappers.transactions;

import io.harness.licensing.beans.transactions.CDLicenseTransactionDTO;
import io.harness.licensing.entities.transactions.modules.CDLicenseTransaction;

public class CDLicenseTransactionMapper
    implements LicenseTransactionMapper<CDLicenseTransaction, CDLicenseTransactionDTO> {
  @Override
  public CDLicenseTransactionDTO toDTO(CDLicenseTransaction entity) {
    return CDLicenseTransactionDTO.builder().workload(entity.getWorkload()).build();
  }

  @Override
  public CDLicenseTransaction toEntity(CDLicenseTransactionDTO dto) {
    return CDLicenseTransaction.builder().workload(dto.getWorkload()).build();
  }
}

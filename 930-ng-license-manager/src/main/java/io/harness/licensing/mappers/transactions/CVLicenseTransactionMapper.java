package io.harness.licensing.mappers.transactions;

import io.harness.licensing.beans.transactions.CVLicenseTransactionDTO;
import io.harness.licensing.entities.transactions.modules.CVLicenseTransaction;

public class CVLicenseTransactionMapper
    implements LicenseTransactionMapper<CVLicenseTransaction, CVLicenseTransactionDTO> {
  @Override
  public CVLicenseTransactionDTO toDTO(CVLicenseTransaction entity) {
    return CVLicenseTransactionDTO.builder().build();
  }

  @Override
  public CVLicenseTransaction toEntity(CVLicenseTransactionDTO dto) {
    return CVLicenseTransaction.builder().build();
  }
}

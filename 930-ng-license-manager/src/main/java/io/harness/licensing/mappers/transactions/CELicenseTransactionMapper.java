package io.harness.licensing.mappers.transactions;

import io.harness.licensing.beans.transactions.CELicenseTransactionDTO;
import io.harness.licensing.entities.transactions.modules.CELicenseTransaction;

public class CELicenseTransactionMapper
    implements LicenseTransactionMapper<CELicenseTransaction, CELicenseTransactionDTO> {
  public CELicenseTransactionDTO toDTO(CELicenseTransaction entity) {
    return CELicenseTransactionDTO.builder().build();
  }

  public CELicenseTransaction toEntity(CELicenseTransactionDTO dto) {
    return CELicenseTransaction.builder().build();
  }
}

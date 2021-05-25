package io.harness.licensing.mappers.transactions;

import io.harness.licensing.beans.transactions.LicenseTransactionDTO;
import io.harness.licensing.entities.transactions.LicenseTransaction;

public interface LicenseTransactionMapper<T extends LicenseTransaction, Y extends LicenseTransactionDTO> {
  Y toDTO(T entity);

  T toEntity(Y dto);
}

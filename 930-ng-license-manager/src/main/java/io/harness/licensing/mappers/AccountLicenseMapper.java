package io.harness.licensing.mappers;

import io.harness.licensing.beans.modules.AccountLicenseDTO;
import io.harness.licensing.entities.account.AccountLicense;

public interface AccountLicenseMapper {
  AccountLicenseDTO toDTO(AccountLicense moduleLicense);

  AccountLicense toEntity(AccountLicenseDTO moduleLicenseDTO);
}

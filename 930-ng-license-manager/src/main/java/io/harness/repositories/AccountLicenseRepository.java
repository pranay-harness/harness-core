package io.harness.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.licensing.entities.account.AccountLicense;

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

@HarnessRepo
@Transactional
public interface AccountLicenseRepository extends CrudRepository<AccountLicense, String> {
  AccountLicense findByAccountIdentifier(String accountIdentifier);
}

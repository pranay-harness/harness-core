package io.harness.licensing.scheduler;

import io.harness.licensing.entities.account.AccountLicense;
import io.harness.mongo.iterator.MongoPersistenceIterator;

public interface AccountLicenseCheckHandler extends MongoPersistenceIterator.Handler<AccountLicense> {
  void registerIterators();
}

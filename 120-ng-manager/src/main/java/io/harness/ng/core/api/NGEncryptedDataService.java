package io.harness.ng.core.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.entities.NGEncryptedData;
import io.harness.security.encryption.EncryptedRecord;

@OwnedBy(PL)
public interface NGEncryptedDataService {
  NGEncryptedData create(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier,
      EncryptedRecord encryptedRecord);

  NGEncryptedData get(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);
}
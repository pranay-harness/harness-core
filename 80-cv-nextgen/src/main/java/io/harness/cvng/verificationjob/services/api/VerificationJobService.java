package io.harness.cvng.verificationjob.services.api;

import io.harness.cvng.verificationjob.beans.VerificationJobDTO;

public interface VerificationJobService {
  VerificationJobDTO get(String accountId, String identifier);
  void upsert(String accountId, VerificationJobDTO verificationJobDTO);
  void delete(String accountId, String identifier);
}

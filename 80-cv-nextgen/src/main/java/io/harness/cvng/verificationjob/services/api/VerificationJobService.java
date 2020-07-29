package io.harness.cvng.verificationjob.services.api;

import io.harness.cvng.verificationjob.beans.VerificationJobDTO;
import io.harness.cvng.verificationjob.entities.VerificationJob;

import javax.annotation.Nullable;

public interface VerificationJobService {
  @Nullable VerificationJob get(String uuid);
  VerificationJobDTO getVerificationJobDTO(String accountId, String identifier);
  VerificationJob getVerificationJob(String accountId, String identifier);
  void upsert(String accountId, VerificationJobDTO verificationJobDTO);
  void delete(String accountId, String identifier);
}

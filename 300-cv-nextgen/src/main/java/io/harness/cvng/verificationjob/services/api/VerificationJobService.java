package io.harness.cvng.verificationjob.services.api;

import io.harness.cvng.verificationjob.beans.VerificationJobDTO;
import io.harness.cvng.verificationjob.entities.VerificationJob;

import java.util.List;
import javax.annotation.Nullable;

public interface VerificationJobService {
  @Nullable VerificationJob get(String uuid);
  VerificationJobDTO getVerificationJobDTO(String accountId, String identifier);
  VerificationJob getVerificationJob(String accountId, String identifier);
  void upsert(String accountId, VerificationJobDTO verificationJobDTO);
  void save(VerificationJob verificationJob);
  void delete(String accountId, String identifier);
  List<VerificationJobDTO> list(String accountId, String projectId, String orgIdentifier);
}

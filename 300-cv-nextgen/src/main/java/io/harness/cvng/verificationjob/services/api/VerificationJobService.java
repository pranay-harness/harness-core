package io.harness.cvng.verificationjob.services.api;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.job.VerificationJobDTO;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.ng.beans.PageResponse;

import javax.annotation.Nullable;
@OwnedBy(HarnessTeam.CV)
public interface VerificationJobService {
  @Nullable VerificationJob get(String uuid);
  VerificationJobDTO getVerificationJobDTO(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier);
  VerificationJob getVerificationJob(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier);
  void create(String accountId, VerificationJobDTO verificationJobDTO);
  void update(String accountId, String identifier, VerificationJobDTO verificationJobDTO);
  void save(VerificationJob verificationJob);
  void delete(String accountId, String orgIdentifier, String projectIdentifier, String identifier);
  PageResponse<VerificationJobDTO> list(
      String accountId, String projectId, String orgIdentifier, Integer offset, Integer pageSize, String filter);
  boolean doesAVerificationJobExistsForThisProject(String accountId, String orgIdentifier, String projectIdentifier);
  int getNumberOfServicesUndergoingHealthVerification(String accountId, String orgIdentifier, String projectIdentifier);
  VerificationJob getResolvedHealthVerificationJob(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String envIdentifier, String serviceIdentifier);
  VerificationJob getDefaultHealthVerificationJob(String accountId, String orgIdentifier, String projectIdentifier);

  VerificationJob getByUrl(String accountId, String verificationJobUrl);
  VerificationJobDTO getDTOByUrl(String accountId, String verificationJobUrl);
  VerificationJob fromDto(VerificationJobDTO verificationJobDTO);
  void createDefaultVerificationJobs(String accountId, String orgIdentifier, String projectIdentifier);
}

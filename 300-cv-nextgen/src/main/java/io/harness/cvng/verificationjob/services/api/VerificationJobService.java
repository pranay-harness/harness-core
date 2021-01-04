package io.harness.cvng.verificationjob.services.api;

import io.harness.cvng.verificationjob.beans.VerificationJobDTO;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.ng.beans.PageResponse;

import java.util.List;
import javax.annotation.Nullable;

public interface VerificationJobService {
  @Nullable VerificationJob get(String uuid);
  VerificationJobDTO getVerificationJobDTO(String accountId, String identifier);
  VerificationJob getVerificationJob(String accountId, String identifier);
  void upsert(String accountId, VerificationJobDTO verificationJobDTO);
  void save(VerificationJob verificationJob);
  void delete(String accountId, String identifier);
  PageResponse<VerificationJobDTO> list(
      String accountId, String projectId, String orgIdentifier, Integer offset, Integer pageSize, String filter);
  boolean doesAVerificationJobExistsForThisProject(String accountId, String orgIdentifier, String projectIdentifier);
  int getNumberOfServicesUndergoingHealthVerification(String accountId, String orgIdentifier, String projectIdentifier);
  List<VerificationJob> getHealthVerificationJobs(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String envIdentifier, String serviceIdentifier);
  void createDefaultHealthVerificationJob(String accountId, String orgIdentifier, String projectIdentifier);
  VerificationJob getOrCreateDefaultHealthVerificationJob(
      String accountId, String orgIdentifier, String projectIdentifier);
  VerificationJobDTO getDefaultHealthVerificationJobDTO(
      String accountId, String orgIdentifier, String projectIdentifier);
}

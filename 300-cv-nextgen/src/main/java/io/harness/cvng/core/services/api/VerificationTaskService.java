package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.entities.VerificationTask;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface VerificationTaskService {
  String create(String accountId, String cvConfigId, DataSourceType provider);
  String create(String accountId, String cvConfigId, String verificationJobInstanceId, DataSourceType provider);
  String getCVConfigId(String verificationTaskId);
  String getVerificationJobInstanceId(String verificationTaskId);
  VerificationTask get(String verificationTaskId);
  Optional<VerificationTask> maybeGet(String verificationTaskId);
  String getVerificationTaskId(String accountId, String cvConfigId, String verificationJobInstanceId);
  Set<String> getVerificationTaskIds(String accountId, String verificationJobInstanceId);

  /**
   * This can return empty if mapping does not exist. Only use this if know that the mapping might not exist. Use
   * #getVerificationTasks otherwise.
   */
  Set<String> maybeGetVerificationTaskIds(String accountId, String verificationJobInstanceId);

  String getServiceGuardVerificationTaskId(String accountId, String cvConfigId);
  List<String> getServiceGuardVerificationTaskIds(String accountId, List<String> cvConfigIds);
  boolean isServiceGuardId(String verificationTaskId);
  void removeCVConfigMappings(String cvConfigId);
  List<String> getVerificationTaskIds(String cvConfigId);
  Optional<String> findBaselineVerificationTaskId(
      String currentVerificationTaskId, VerificationJobInstance verificationJobInstance);
  List<String> getAllVerificationJobInstanceIdsForCVConfig(String cvConfigId);
  List<String> maybeGetVerificationTaskIds(List<String> verificationJobInstanceIds);
}

package io.harness.cvng.verificationjob.services.api;

import io.harness.cvng.core.beans.TimeRange;
import io.harness.cvng.verificationjob.beans.VerificationJobInstanceDTO;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.ProgressLog;

public interface VerificationJobInstanceService {
  String create(String accountId, VerificationJobInstanceDTO verificationJobInstanceDTO);
  VerificationJobInstanceDTO get(String verificationTaskId);
  VerificationJobInstance getVerificationJobInstance(String verificationTaskId);
  void createDataCollectionTasks(VerificationJobInstance verificationJobInstance);
  void logProgress(String verificationJobInstanceId, ProgressLog progressLog);
  void deletePerpetualTasks(VerificationJobInstance entity);
  TimeRange getPreDeploymentTimeRange(String verificationJobInstanceId);
}

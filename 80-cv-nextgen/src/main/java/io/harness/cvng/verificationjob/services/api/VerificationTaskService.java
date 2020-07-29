package io.harness.cvng.verificationjob.services.api;

import io.harness.cvng.verificationjob.beans.VerificationTaskDTO;
import io.harness.cvng.verificationjob.entities.VerificationTask;

public interface VerificationTaskService {
  String create(String accountId, VerificationTaskDTO verificationTaskDTO);
  VerificationTaskDTO get(String verificationTaskId);
  VerificationTask getVerificationTask(String verificationTaskId);
  void createDataCollectionTasks(VerificationTask verificationTask);
}

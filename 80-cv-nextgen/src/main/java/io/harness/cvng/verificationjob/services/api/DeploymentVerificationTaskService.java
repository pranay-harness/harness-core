package io.harness.cvng.verificationjob.services.api;

import io.harness.cvng.verificationjob.beans.DeploymentVerificationTaskDTO;
import io.harness.cvng.verificationjob.entities.DeploymentVerificationTask;

public interface DeploymentVerificationTaskService {
  String create(String accountId, DeploymentVerificationTaskDTO deploymentVerificationTaskDTO);
  DeploymentVerificationTaskDTO get(String verificationTaskId);
  DeploymentVerificationTask getVerificationTask(String verificationTaskId);
  void createDataCollectionTasks(DeploymentVerificationTask deploymentVerificationTask);
}

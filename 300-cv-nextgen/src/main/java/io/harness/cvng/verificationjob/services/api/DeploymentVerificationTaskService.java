package io.harness.cvng.verificationjob.services.api;

import io.harness.cvng.core.beans.TimeRange;
import io.harness.cvng.verificationjob.beans.DeploymentVerificationTaskDTO;
import io.harness.cvng.verificationjob.entities.DeploymentVerificationTask;
import io.harness.cvng.verificationjob.entities.DeploymentVerificationTask.ProgressLog;

public interface DeploymentVerificationTaskService {
  String create(String accountId, DeploymentVerificationTaskDTO deploymentVerificationTaskDTO);
  DeploymentVerificationTaskDTO get(String verificationTaskId);
  DeploymentVerificationTask getVerificationTask(String verificationTaskId);
  void createDataCollectionTasks(DeploymentVerificationTask deploymentVerificationTask);
  void logProgress(String deploymentVerificationId, ProgressLog progressLog);
  void deletePerpetualTasks(DeploymentVerificationTask entity);
  TimeRange getPreDeploymentTimeRange(String deploymentVerificationTaskId);
}

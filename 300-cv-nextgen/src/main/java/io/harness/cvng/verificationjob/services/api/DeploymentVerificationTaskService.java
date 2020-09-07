package io.harness.cvng.verificationjob.services.api;

import io.harness.cvng.statemachine.entities.AnalysisStatus;
import io.harness.cvng.verificationjob.beans.DeploymentVerificationTaskDTO;
import io.harness.cvng.verificationjob.entities.DeploymentVerificationTask;

import java.time.Instant;

public interface DeploymentVerificationTaskService {
  String create(String accountId, DeploymentVerificationTaskDTO deploymentVerificationTaskDTO);
  DeploymentVerificationTaskDTO get(String verificationTaskId);
  DeploymentVerificationTask getVerificationTask(String verificationTaskId);
  void createDataCollectionTasks(DeploymentVerificationTask deploymentVerificationTask);
  void logProgress(String deploymentVerificationId, Instant startTime, Instant endTime, AnalysisStatus analysisStatus);

  void deletePerpetualTasks(DeploymentVerificationTask entity);
}

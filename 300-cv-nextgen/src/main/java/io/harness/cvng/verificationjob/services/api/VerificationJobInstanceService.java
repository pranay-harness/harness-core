package io.harness.cvng.verificationjob.services.api;

import io.harness.cvng.activity.beans.ActivityVerificationSummary;
import io.harness.cvng.activity.beans.DeploymentActivityPopoverResultDTO;
import io.harness.cvng.activity.beans.DeploymentActivityResultDTO;
import io.harness.cvng.activity.beans.DeploymentActivityResultDTO.DeploymentResultSummary;
import io.harness.cvng.activity.beans.DeploymentActivityVerificationResultDTO;
import io.harness.cvng.core.beans.TimeRange;
import io.harness.cvng.verificationjob.beans.TestVerificationBaselineExecutionDTO;
import io.harness.cvng.verificationjob.beans.VerificationJobInstanceDTO;
import io.harness.cvng.verificationjob.beans.VerificationJobType;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.ProgressLog;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface VerificationJobInstanceService {
  String create(String accountId, VerificationJobInstanceDTO verificationJobInstanceDTO);
  String create(VerificationJobInstance verificationJobInstance);
  List<String> create(List<VerificationJobInstance> verificationJobInstances);
  VerificationJobInstanceDTO get(String verificationJobInstanceId);
  List<VerificationJobInstance> get(List<String> verificationJobInstanceIds);
  List<VerificationJobInstance> getNonDeploymentInstances(List<String> verificationJobInstanceIds);
  VerificationJobInstance getVerificationJobInstance(String verificationJobInstanceId);
  void processVerificationJobInstance(VerificationJobInstance verificationJobInstance);
  void createDataCollectionTasks(VerificationJobInstance verificationJobInstance);
  void logProgress(String verificationJobInstanceId, ProgressLog progressLog);
  void deletePerpetualTasks(VerificationJobInstance entity);
  Optional<TimeRange> getPreDeploymentTimeRange(String verificationJobInstanceId);
  DeploymentActivityVerificationResultDTO getAggregatedVerificationResult(List<String> verificationJobInstanceIds);
  void addResultsToDeploymentResultSummary(
      String accountId, List<String> verificationJobInstanceIds, DeploymentResultSummary deploymentResultSummary);
  DeploymentActivityPopoverResultDTO getDeploymentVerificationPopoverResult(List<String> verificationJobInstanceIds);
  List<TestVerificationBaselineExecutionDTO> getTestJobBaselineExecutions(
      String accountId, String orgIdentifier, String projectIdentifier, String verificationJobIdentifier);
  Optional<String> getLastSuccessfulTestVerificationJobExecutionId(
      String accountId, String orgIdentifier, String projectIdentifier, String verificationJobIdentifier);
  ActivityVerificationSummary getActivityVerificationSummary(List<VerificationJobInstance> verificationJobInstances);
  ActivityVerificationSummary getDeploymentSummary(List<VerificationJobInstance> verificationJobInstances);
  DeploymentActivityResultDTO.DeploymentVerificationJobInstanceSummary getDeploymentVerificationJobInstanceSummary(
      List<String> verificationJobInstanceIds);
  List<VerificationJobInstance> getRunningOrQueuedJobInstances(String accountI, String orgIdentifier,
      String projectIdentifier, String envIdentifier, String serviceIdentifier, VerificationJobType jobType,
      Instant endTimeBefore);
}
